package org.daiv.persister.table

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.persister.MoreKeys
import org.daiv.persister.collector.*
import kotlin.test.Test
import kotlin.test.assertEquals

expect fun runTest(block: suspend () -> Unit)

class InsertTableTest {

    @Serializable
    data class SimpleClass(val x: Int, val s: String)

    @Serializable
    data class ComplexClass(val z: Int, val simpleClass: SimpleClass)

    @Serializable
    data class Complex2Class(val z: Int, val complexClass: ComplexClass)

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
        val cache = InsertionCache("test",DefaultScopeContextable())
        val serializer = ComplexClass.serializer()
        InsertElementAdder(cache, SerializersModule { }).addElement(serializer, ComplexClass(7, SimpleClass(5, "Hello")))
//        cache.get(serializer)?.all()?.firstOrNull()?.let { handler ->
//            assertEquals(listOf("z", "simpleClass_x"), handler.head())
//            assertEquals(listOf(7, 5), handler.toValues())
//        } ?: run {
//            fail("got no insertionObject")
//        }
    }

    fun String.value(value: Int) = DBEntry(this, Int.serializer().descriptor, value, false)
    fun String.key(value: Int) = DBEntry(this, Int.serializer().descriptor, value, true)

    fun String.simpleValue(value: SimpleClass) = DBEntry(this, SimpleClass.serializer().descriptor, value, false)
    fun String.simpleKey(value: SimpleClass) = DBEntry(this, SimpleClass.serializer().descriptor, value, true)

    fun String.valueString(value: String) = DBEntry(this, String.serializer().descriptor, value, false)
    fun String.keyString(value: String) = DBEntry(this, String.serializer().descriptor, value, true)

    @Test
    fun testToInsertCommand() {
        val inserts = listOf(
            InsertionResult(
                listOf(DBRow(listOf("z".key(7), "simpleClass_x".value(5))), DBRow(listOf("z".key(5), "simpleClass_x".value(1)))),
                ComplexClass.serializer().descriptor.tableDescriptor()
            ),
            InsertionResult(
                listOf(DBRow(listOf("z".key(8), "simpleClass_x".value(6)))), ComplexClass.serializer().descriptor.tableDescriptor()
            ),
        )
        val command = inserts.toInsertCommand()
        assertEquals("INSERT INTO `ComplexClass` (z, simpleClass_x) VALUES (7, 5), (5, 1), (8, 6);", command)
    }

    @Test
    fun testGetKey() {
        val simple = SimpleClass(8, "Hello")
        assertEquals(
            ClassKeyImpl(KeyType.DEFAULT, listOf("z".key(5)), false),
            ComplexClass.serializer().getKeys(ComplexClass(5, simple))
        )
        assertEquals(
            ClassKeyImpl(KeyType.DEFAULT, listOf("z".key(5), "simpleClass".simpleKey(simple)), false),
            MultipleKeyClass.serializer().getKeys(MultipleKeyClass(5, simple))
        )
    }

    @Test
    fun testInsertObject() = runTest {
        val cache = InsertionCache("test",DefaultScopeContextable())
        val s = SimpleClass(5, "Heelo")
        val s2 = SimpleClass(9, "Hello")
        val serializer = SimpleClass.serializer()
        val mutableList = mutableSetOf<SimpleClass>()
        var counter = 0
        val valueCreation: suspend (SimpleClass) -> InsertionResult = {
            mutableList.add(it)
            counter++
            InsertionResult(listOf(), serializer.descriptor.tableDescriptor())
        }
        cache.insert(s, serializer.descriptor, valueCreation)
        cache.insert(s2, serializer.descriptor, valueCreation)
        cache.join()
        assertEquals(2, counter)
        assertEquals(setOf(s, s2), mutableList)
    }

    @Test
    fun testInsert() = runTest {
        val s1 = ComplexClass(7, SimpleClass(5, "Hello"))
        val s2 = ComplexClass(8, SimpleClass(6, "World"))
//        val expect = "INSERT INTO `ComplexClass` (z, simpleClass_x) VALUES (7, 5), (8, 6);"
//        val expect2 = "INSERT INTO `SimpleClass` (x, s) VALUES (5, \"Hello\"), (6, \"World\");"
//        val expectList = setOf(expect, expect2)
        val l = listOf(s1, s2)
        val x = InsertTable(InsertionCache("test",DefaultScopeContextable()), SerializersModule { })

        x.insert(l, ComplexClass.serializer())
        x.readCache.join()
        val t = x.readCache.all().map { it.map().values }.flatten()
        assertEquals(
            setOf(
                InsertionResult(
                    listOf(DBRow(listOf("z".key(7), "simpleClass_x".value(5)))), ComplexClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("z".key(8), "simpleClass_x".value(6)))), ComplexClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("x".key(6), "s".valueString("\"World\"")))), SimpleClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("x".key(5), "s".valueString("\"Hello\"")))), SimpleClass.serializer().descriptor.tableDescriptor()
                ),
            ), t.toSet()
        )
    }

    @Test
    fun tooComplex() = runTest {
        val s1 = Complex2Class(6, ComplexClass(7, SimpleClass(5, "Hello")))
        val s2 = Complex2Class(9, ComplexClass(8, SimpleClass(6, "World")))
//        val expect = "INSERT INTO `ComplexClass` (z, simpleClass_x) VALUES (7, 5), (8, 6);"
//        val expect2 = "INSERT INTO `SimpleClass` (x, s) VALUES (5, \"Hello\"), (6, \"World\");"
//        val expectList = setOf(expect, expect2)
        val l = listOf(s1, s2)
        val x = InsertTable(InsertionCache("test",DefaultScopeContextable()), SerializersModule { })

        x.insert(l, Complex2Class.serializer())
        x.readCache.join()
        val t = x.readCache.all().map { it.map().values }.flatten()
        t.forEach {
            println("it: $it")
        }
        assertEquals(
            setOf(
                InsertionResult(
                    listOf(DBRow(listOf("z".key(6), "complexClass_z".value(7)))), Complex2Class.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("z".key(9), "complexClass_z".value(8)))), Complex2Class.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("z".key(7), "simpleClass_x".value(5)))), ComplexClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("z".key(8), "simpleClass_x".value(6)))), ComplexClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("x".key(6), "s".valueString("\"World\"")))), SimpleClass.serializer().descriptor.tableDescriptor()
                ),
                InsertionResult(
                    listOf(DBRow(listOf("x".key(5), "s".valueString("\"Hello\"")))), SimpleClass.serializer().descriptor.tableDescriptor()
                ),
            ), t.toSet()
        )
    }
}
