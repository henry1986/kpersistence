package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.daiv.persister.MoreKeys
import kotlin.test.Test
import kotlin.test.assertEquals


class CreateTableTest {

    @Serializable
    data class SimpleClass(val x: Int, val s: String)

    @Serializable
    @MoreKeys(2)
    data class MultipleKeyClass(val x: Int, val s: String)

    @Serializable
    data class ComplexClass(val s: MultipleKeyClass, val d: Int)

    @Serializable
    @MoreKeys(2)
    data class Complex2Class(val s: MultipleKeyClass, val complexClass: ComplexClass, val d: Int)

    @Serializable
    data class Complex3Class(val simpleClass: SimpleClass, val d: Int)


    @Test
    fun testToHead() {
        assertEquals("x INT NOT NULL", SimpleClass.serializer().descriptor.toHead()[0])
        assertEquals("s TEXT NOT NULL", SimpleClass.serializer().descriptor.toHead()[1])
        assertEquals("s TEXT NOT NULL", MultipleKeyClass.serializer().descriptor.toHead()[1])
        assertEquals(listOf("s_x INT NOT NULL", "s_s TEXT NOT NULL", "d INT NOT NULL"), ComplexClass.serializer().descriptor.toHead())
        assertEquals("simpleClass_x INT NOT NULL", Complex3Class.serializer().descriptor.toHead()[0])
    }

    @Test
    fun testGetNativeDescriptors() {
        assertEquals(
            listOf(Int.serializer().descriptor, String.serializer().descriptor),
            SimpleClass.serializer().descriptor.getNativeDescriptors()
        )
        assertEquals(
            listOf(Int.serializer(), String.serializer(), Int.serializer()).map { it.descriptor },
            ComplexClass.serializer().descriptor.getNativeDescriptors()
        )
        assertEquals(
            listOf(
                Int.serializer(),
                String.serializer(),
                Int.serializer(),
                String.serializer(),
                Int.serializer(),
                Int.serializer()
            ).map { it.descriptor },
            Complex2Class.serializer().descriptor.getNativeDescriptors()
        )
    }

    @Test
    fun testBuildCreateTable() {
        assertEquals(
            "CREATE TABLE IF NOT EXISTS SimpleClass (x INT NOT NULL, s TEXT NOT NULL, PRIMARY KEY(x));",
            SimpleClass.serializer().descriptor.buildCreateTable()
        )
        assertEquals(
            "CREATE TABLE IF NOT EXISTS ComplexClass (s_x INT NOT NULL, s_s TEXT NOT NULL, d INT NOT NULL, PRIMARY KEY(s_x, s_s));",
            ComplexClass.serializer().descriptor.buildCreateTable()
        )
        assertEquals(
            "CREATE TABLE IF NOT EXISTS Complex2Class (s_x INT NOT NULL, s_s TEXT NOT NULL, complexClass_s_x INT NOT NULL, complexClass_s_s TEXT NOT NULL, d INT NOT NULL, PRIMARY KEY(s_x, s_s, complexClass_s_x, complexClass_s_s));",
            Complex2Class.serializer().descriptor.buildCreateTable()
        )
//        println("cre: $createTable")
    }

    @Test
    fun testGetNativeKeyDescriptorNames() {
        val primary = Complex2Class.serializer().descriptor.getNativeKeyDescriptorNames(null)
        assertEquals(listOf("s_x", "s_s", "complexClass_s_x", "complexClass_s_s"), primary)
    }

    @Test
    fun testGetNativeDescriptorNamesComplex() {
        val descriptors = Complex2Class.serializer().descriptor.getNativeDescriptorNames()
        assertEquals(listOf("s_x", "s_s", "complexClass_s_x", "complexClass_s_s", "d"), descriptors)
    }

    @Test
    fun testBuildPrefix() {
        assertEquals("s", "s".prefix(null))
        assertEquals("n_s", "s".prefix("n"))
    }

    @Test
    fun testGetNativeDescriptorNamesSimple() {
        val descriptors = MultipleKeyClass.serializer().descriptor.getNativeKeyDescriptorNames("t")
        assertEquals(listOf("t_x", "t_s"), descriptors)
    }

//    @Test
//    fun testKeyToHead() {
//        assertEquals(listOf("x INT NOT NULL"), SimpleClass.serializer().descriptor.keyToHead(null))
//        assertEquals(listOf("s_x INT NOT NULL", "s_s TEXT NOT NULL"), ComplexClass.serializer().descriptor.keyToHead(null))
//    }

    @Test
    fun testPrimaryKey() {
        val primary = SimpleClass.serializer().descriptor.primaryKey()
        assertEquals("PRIMARY KEY(x)", primary)
    }

    @Test
    fun testMultiplePrimaryKey() {
        val primary = MultipleKeyClass.serializer().descriptor.primaryKey()
        assertEquals("PRIMARY KEY(x, s)", primary)
    }

    @Test
    fun testMultipleComplexPrimaryKey() {
        val primary = ComplexClass.serializer().descriptor.primaryKey()
        assertEquals("PRIMARY KEY(s_x, s_s)", primary)
    }
}
