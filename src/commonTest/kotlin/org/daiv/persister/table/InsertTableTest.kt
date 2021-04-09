package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
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
        val cache = InsertionCache(DefaultScopeContextable())
        val serializer = ComplexClass.serializer()
        serializer.insertObject(cache, ComplexClass(7, SimpleClass(5, "Hello")))
//        cache.get(serializer)?.all()?.firstOrNull()?.let { handler ->
//            assertEquals(listOf("z", "simpleClass_x"), handler.head())
//            assertEquals(listOf(7, 5), handler.toValues())
//        } ?: run {
//            fail("got no insertionObject")
//        }
    }

    @Test
    fun testGetKey() {
        val simple = SimpleClass(8, "Hello")
        assertEquals(
            ClassKeyImpl(KeyType.DEFAULT, listOf(DBEntry("z", Int.serializer().descriptor, 5)), false),
            ComplexClass.serializer().getKeys(ComplexClass(5, simple))
        )
        assertEquals(
            ClassKeyImpl(
                KeyType.DEFAULT, listOf(
                    DBEntry("z", Int.serializer().descriptor, 5),
                    DBEntry("simpleClass", SimpleClass.serializer().descriptor, simple)
                ), false
            ),
            MultipleKeyClass.serializer().getKeys(MultipleKeyClass(5, simple))
        )
    }

    @Test
    fun testInsert() = runTest {
        val s1 = ComplexClass(7, SimpleClass(5, "Hello"))
        val s2 = ComplexClass(8, SimpleClass(6, "World"))
//        val expect = "INSERT INTO `ComplexClass` (z, simpleClass_x) VALUES (7, 5), (8, 6);"
//        val expect2 = "INSERT INTO `SimpleClass` (x, s) VALUES (5, \"Hello\"), (6, \"World\");"
//        val expectList = setOf(expect, expect2)
        val l = listOf(s1, s2)
        val x = InsertTable(InsertionCache(DefaultScopeContextable()))

        x.insert(l, ComplexClass.serializer())
        x.readCache.join()
        val t = x.readCache.all().map { it.map().values }.flatten()
        assertEquals(
            setOf(
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 7),
                        DBEntry("simpleClass_x", Int.serializer().descriptor, 5)
                    ), ComplexClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 8),
                        DBEntry("simpleClass_x", Int.serializer().descriptor, 6)
                    ), ComplexClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("x", Int.serializer().descriptor, 6),
                        DBEntry("s", String.serializer().descriptor, "\"World\"")
                    ), SimpleClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("x", Int.serializer().descriptor, 5),
                        DBEntry("s", String.serializer().descriptor, "\"Hello\"")
                    ), SimpleClass.serializer().descriptor
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
        val x = InsertTable(InsertionCache(DefaultScopeContextable()))

        x.insert(l, Complex2Class.serializer())
        x.readCache.join()
        val t = x.readCache.all().map { it.map().values }.flatten()
        t.forEach {
            println("it: $it")
        }
        assertEquals(
            setOf(
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 6),
                        DBEntry("complexClass_z", Int.serializer().descriptor, 7)
                    ), Complex2Class.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 9),
                        DBEntry("complexClass_z", Int.serializer().descriptor, 8)
                    ), Complex2Class.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 7),
                        DBEntry("simpleClass_x", Int.serializer().descriptor, 5)
                    ), ComplexClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("z", Int.serializer().descriptor, 8),
                        DBEntry("simpleClass_x", Int.serializer().descriptor, 6)
                    ), ComplexClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("x", Int.serializer().descriptor, 6),
                        DBEntry("s", String.serializer().descriptor, "\"World\"")
                    ), SimpleClass.serializer().descriptor
                ),
                InsertionResult(
                    listOf(
                        DBEntry("x", Int.serializer().descriptor, 5),
                        DBEntry("s", String.serializer().descriptor, "\"Hello\"")
                    ), SimpleClass.serializer().descriptor
                ),
            ), t.toSet()
        )
    }

}
