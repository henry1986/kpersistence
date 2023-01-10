package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectTypeHandlerTest {
    @MoreKeys(2)
    class MyObject(val i: Int, val s: String, val x: Long)

    class TestValueGetter(val any: Any) : GetValue<Any, Any> {
        override fun get(higherClass: Any): Any {
            return any
        }
    }

    @Test
    fun test() {
        val handler = ObjectTypeHandler(
            "MyObject",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, TestValueGetter(592)),
            )
        )
        assertEquals("i INT NOT NULL, s TEXT NOT NULL, x LONG NOT NULL", handler.toHeader())
        assertEquals("i, s, x", handler.insertHead())
    }

    @Test
    fun testRefHandler() {
        val handler = ObjectTypeRefHandler(
            "MyObject",
            "m",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, TestValueGetter(592)),
            )
        )
        assertEquals("m_i INT NOT NULL, m_s TEXT NOT NULL", handler.toHeader())
        assertEquals("m_i, m_s", handler.insertHead())
    }
}

class TestComplexObjectType{
    @MoreKeys(2)
    data class ComplexObject(val my: ObjectTypeHandlerTest.MyObject, val x:Int, val s:String)


    fun test1(){
        val handler = ObjectTypeHandler(
            "ComplexObject",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, ObjectTypeHandlerTest.TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, ObjectTypeHandlerTest.TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, ObjectTypeHandlerTest.TestValueGetter(592)),
            )
        )
        assertEquals("i INT NOT NULL, s TEXT NOT NULL, x LONG NOT NULL", handler.toHeader())
        assertEquals("i, s, x", handler.insertHead())
    }
}
