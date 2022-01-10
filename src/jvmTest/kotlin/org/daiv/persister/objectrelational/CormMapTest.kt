package org.daiv.persister.objectrelational

import org.daiv.persister.runTest
import org.junit.Test
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals


class CormMapTest {
    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val cx: Int, val s: SimpleObject)
    private data class ComplexListObject(val cx: Int, val s: List<SimpleObject>)

    @Test
    fun testSimpleObject() = runTest {
        val map = CormMap()
        val noNative = map.createNoNative(SimpleObject::class)
        assertEquals(emptyList(), noNative)
    }

    @Test
    fun testComplexObject() = runTest {
        val map = CormMap()
        val simpleObjectMapper = map.getValue(SimpleObject::class)
        val noNative = map.createNoNative(ComplexObject::class)
        val expect = ComplexObject::class.declaredMemberProperties.drop(1)
            .map { PropertyMapper(it, simpleObjectMapper as ObjectRelationalMapper<Any?>) }
        assertEquals(expect, noNative)
    }

    @Test
    fun testComplexListObject() = runTest {
        val map = CormMap()
        val simpleObjectMapper = map.getValue(SimpleObject::class)
        val collection = map.createCollections(ComplexListObject::class)
        println("collection: $collection")

//        val expect = ComplexListObject::class.declaredMemberProperties.drop(1)
//            .map { PropertyMapper(it, simpleObjectMapper as ObjectRelationalMapper<Any?>) }
//        assertEquals(expect, noNative)
    }
}
