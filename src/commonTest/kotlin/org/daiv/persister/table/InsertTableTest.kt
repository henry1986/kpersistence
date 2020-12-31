package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.daiv.persister.MoreKeys
import org.daiv.persister.PCEncoder
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class InsertTableTest {

    @Serializable
    data class SimpleClass(val x: Int, val s: String)

    @Serializable
    data class ComplexClass(val z: Int, val simpleClass: SimpleClass)

    @Serializable
    @MoreKeys(2)
    data class MultipleKeyClass(val z: Int, val simpleClass: SimpleClass)

    @Serializable
    data class ListClass(val y: Int, val list: List<SimpleClass>)

    @Test
    fun testInsertHead() {
        assertEquals(listOf("x", "s"), SimpleClass.serializer().descriptor.getNativeDescriptorNames())
        assertEquals(listOf("z", "simpleClass_x"), ComplexClass.serializer().descriptor.getNativeDescriptorNames())
    }

    @Test
    fun testInsertionHandler() {
        val cache = InsertionCache()
        val serializer = ComplexClass.serializer()
        serializer.insertObject(cache, ComplexClass(7, SimpleClass(5, "Hello")))
        cache.get(serializer)?.all()?.firstOrNull()?.let { handler ->
            assertEquals(listOf("z", "simpleClass_x"), handler.head())
            assertEquals(listOf(7, 5), handler.toValues())
        } ?: run {
            fail("got no insertionObject")
        }
    }

    @Test
    fun testGetKey() {
        val simple = SimpleClass(8, "Hello")
        assertEquals(
            ClassKeyImpl(KeyType.DEFAULT, listOf(CollectedValue("z", Int.serializer().descriptor, 5)), false),
            ComplexClass.serializer().getKeys(ComplexClass(5, simple))
        )
        assertEquals(
            ClassKeyImpl(KeyType.DEFAULT,listOf(
                CollectedValue("z", Int.serializer().descriptor, 5),
                CollectedValue("simpleClass", SimpleClass.serializer().descriptor, simple)
            ), false),
            MultipleKeyClass.serializer().getKeys(MultipleKeyClass(5, simple))
        )
    }

    @Test
    fun testInsert() {
        val s1 = ComplexClass(7, SimpleClass(5, "Hello"))
        val s2 = ComplexClass(8, SimpleClass(6, "World"))
        val expect = "INSERT INTO SimpleClass (x, y) VALUES (5, \"Hello\"), (6, \"World\");"
        val expect2 = "INSERT INTO ComplexClass (z, simpleClass_x) VALUES (7, 5), (8, 6);"
        val expectList = setOf(expect, expect2)
        val l = listOf(s1, s2)
        val x = InsertTable(InsertionCache())
        x.insert(l, ComplexClass.serializer())
        assertEquals(expectList, x.persistCache().toSet())
//        val names = collected.map { it.name }
//        collected.forEach {
//            println("name: ${it.name}")
//        }
//        collected.forEach {
//            println("value: ${it.value}")
//        }
//        encoder.composite.dataCollector.collectedValues.forEach {
//            println("it: $it")
//        }

    }

//    @Test
//    fun testList() {
//        val module = SerializersModule { }
//        val dataCollector = DataCollector(ListClass.serializer().descriptor, false, null)
//        val encoder = PEncoder(module, dataCollector)
//        ListClass.serializer()
//            .serialize(encoder, ListClass(5, listOf(SimpleClass(1, "Hello"), SimpleClass(2, "Hello2"), SimpleClass(3, "Hello3"))))
//    }
}
