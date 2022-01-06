package org.daiv.persister.objectrelational

import org.daiv.persister.runTest
import org.junit.Test
import kotlin.test.assertEquals

class PropertyMapperTest {
    private data class TestClass(val x: Int)
    private data class TestComplexClass(val x: Int, val t: TestClass)

    @Test
    fun testGetValue() = runTest {
        val map = CormMap()
        val p = map.createNoNative(TestComplexClass::class).first()
        val writerMap = p.writerMap()
        val f = writerMap.func
        val expect = TestClass(9)
        val c = TestComplexClass(5, expect)
        val toTest = c.f()
        assertEquals(expect, toTest)
    }

    @Test
    fun testObjectRelationWriterMapEquality() = runTest {
        val map = CormMap()
        val testClassMapper = map.getValue(TestClass::class)
        val p = map.createNoNative(TestComplexClass::class).first()
        val writerMap: ObjectRelationalWriterMap<TestComplexClass, out Any?> = p.writerMap()
        val x: Any =
            ObjectRelationalWriterMap<TestComplexClass, TestClass>(testClassMapper.objectRelationalWriter) { t }
        assertEquals(x, writerMap)
    }
}