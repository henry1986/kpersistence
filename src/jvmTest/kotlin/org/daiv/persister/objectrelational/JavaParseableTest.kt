package org.daiv.persister.objectrelational

import org.junit.Test
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals

class JavaParseableTest {
    private data class TestClass(val x: Int)
    private object X : JavaParseable<TestClass> {
        override val noNative: List<PropertyMapper<TestClass, Any?>> = emptyList()
        fun testWrite() {
            val clazz = TestClass::class
            val writeKeys =
                clazz.primaryConstructor!!.parameters.toWriteEntry(clazz, true) as List<DefaultPreWriteEntry<TestClass>>
            val t = TestClass(5)
            val list = writeKeys.map {
                val f = it.func
                t.f()
            }
            assertEquals(listOf(5), list)
        }
    }

    @Test
    fun test() {
        X.testWrite()
    }
}