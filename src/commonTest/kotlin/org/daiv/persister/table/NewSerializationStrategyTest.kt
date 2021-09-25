package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.*
import org.daiv.persister.collector.encoder.MapEncoderStrategy
import org.daiv.persister.collector.encoder.toRows
import kotlin.test.Test
import kotlin.test.assertEquals

class NewSerializationStrategyTest {
    @Serializable
    data class SimpleClass(val x: Int, val y: Int)

    @Serializable
    data class ComplexClass(val e: Int, val d: SimpleClass)

    @Serializable
    data class CLassWithList(val m: Int, val list: List<SimpleClass>)


    val module = SerializersModule { }

    @Test
    fun testSimple() = runTest {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val values = SimpleClass(5, 6).toEntries(module, SimpleClass.serializer(), cache)
        assertEquals(
            listOf(DBEntry("x", Int.serializer().descriptor, 5, true), DBEntry("y", Int.serializer().descriptor, 6, false)),
            values.values.flatMap { it.entries }
        )
    }

    @Test
    fun testComplex() = runTest {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val simple = SimpleClass(5, 6)
        val complex = ComplexClass(9, simple)
        val entries = complex.toEntries(module, ComplexClass.serializer(), cache)
        cache.join()

        assertEquals(
            listOf(DBEntry("e", Int.serializer().descriptor, 9, true), DBEntry("d_x", Int.serializer().descriptor, 5, false)),
            entries.values.flatMap { it.entries }
        )
    }

    @Test
    fun testSimpleList() = runTest {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val list = listOf(5, 6, 7)
        val serializer = ListSerializer(Int.serializer())
        val entries = list.toRows(emptyList(), module, serializer, cache)
//        entries.forEach {
//            println("it: $it")
//        }
    }

    private val intDescriptor = Int.serializer().descriptor

    companion object {
        private val logger = KotlinLogging.logger("org.daiv.persister.table.NewSerializationStrategyTest")
    }

    @Test
    fun testComplexList() = runTest {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val list = listOf(SimpleClass(5, 6), SimpleClass(9, 7))
        val serializer = ListSerializer(SimpleClass.serializer())
        val rows = list.toRows(emptyList(), module, serializer, cache)
        val values = rows.values
        values.forEach {
            logger.trace { "it: $it" }
        }
        assertEquals(
            setOf(
                DBRow(listOf(DBEntry("index", intDescriptor, 0, true), DBEntry("value_x", intDescriptor, 5, false))),
                DBRow(listOf(DBEntry("index", intDescriptor, 1, true), DBEntry("value_x", intDescriptor, 9, false)))
            ),
            values.toSet()
        )
    }

    @Test
    fun testClassWithList() = runTest {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val list = listOf(SimpleClass(5, 6), SimpleClass(9, 7))
        val clas = CLassWithList(12, list)
        val entries = clas.toEntries(module, CLassWithList.serializer(), cache).values
        entries.forEach {
            println("it: $it")
        }
        cache.join()
        val all = cache.toMap()
        val ret = all.map { it.key to it.value.all().flatMap { it.values } }.toMap()
        assertEquals(ret[CLassWithList.serializer().descriptor], listOf(DBRow(listOf(DBEntry("m", Int.serializer().descriptor, 12, true)))))
        assertEquals(
            setOf(
                DBRow(
                    listOf(
                        DBEntry("m", Int.serializer().descriptor, 12, true),
                        DBEntry("index", Int.serializer().descriptor, 0, true),
                        DBEntry("value_x", Int.serializer().descriptor, 5, false)
                    )
                ),
                DBRow(
                    listOf(
                        DBEntry("m", Int.serializer().descriptor, 12, true),
                        DBEntry("index", Int.serializer().descriptor, 1, true),
                        DBEntry("value_x", Int.serializer().descriptor, 9, false)
                    )
                ),
            ), ret[ListSerializer(SimpleClass.serializer()).descriptor]?.toSet()
        )
        assertEquals(
            setOf(
                DBRow(listOf(DBEntry("x", Int.serializer().descriptor, 5, true), DBEntry("y", Int.serializer().descriptor, 6, false))),
                DBRow(listOf(DBEntry("x", Int.serializer().descriptor, 9, true), DBEntry("y", Int.serializer().descriptor, 7, false))),
            ), ret[SimpleClass.serializer().descriptor]?.toSet()
        )
//        ret.forEach {
//            println("it: ${it.first}")
//            it.second.forEach {
//                println("row: $it")
//            }
//        }
    }

    @Test
    fun testMap() {
        val cache = InsertionCache("test", DefaultScopeContextable())
        val map = mapOf(5 to SimpleClass(9,9), 6 to SimpleClass(10,9), 7 to SimpleClass(8,9))
        val s = MapSerializer(Int.serializer(), SimpleClass.serializer())
        val pEncoder = PEncoder(SerializersModule { }) { isCollection -> MapEncoderStrategy(null, emptyList(), cache) }
        s.serialize(pEncoder, map)
    }
}