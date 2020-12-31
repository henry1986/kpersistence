package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.daiv.persister.collector.ClassKey
import org.daiv.persister.collector.ClassKeyImpl
import org.daiv.persister.collector.CollectedValue
import org.daiv.persister.collector.KeyType
import kotlin.test.Test
import kotlin.test.assertEquals

fun String.intValue(value: Int) = CollectedValue(this, Int.serializer().descriptor, value)
fun String.stringValue(value: String) = CollectedValue(this, String.serializer().descriptor, value)

class InsertionCacheTest {
    @Serializable
    private data class SimpleTest1(val x: Int, val y: Int)

    @Serializable
    private data class SimpleTest2(val z: Int, val d: String)


    @Test
    fun testInsertion() {
        val cache = InsertionCache()
        val l1 = listOf("x".intValue(5), "y".intValue(9))
        val l2 = listOf("x".intValue(6), "y".intValue(15))
        val l3 = listOf("x".intValue(6), "y".stringValue("hello"))
        cache.set(ClassKeyImpl(KeyType.DEFAULT, listOf("x".intValue(5)), false), InsertionResult(l1, SimpleTest1.serializer()))
        cache.set(ClassKeyImpl(KeyType.DEFAULT, listOf("x".intValue(6)), false), InsertionResult(l2, SimpleTest1.serializer()))
        cache.set(ClassKeyImpl(KeyType.DEFAULT, listOf("x".intValue(6)), false), InsertionResult(l3, SimpleTest2.serializer()))
        val got = cache.get(SimpleTest1.serializer())
        assertEquals(
            setOf(InsertionResult(l1, SimpleTest1.serializer()), InsertionResult(l2, SimpleTest1.serializer())),
            got?.all()?.toSet()
        )
    }
}