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

class TestComplexObjectType {
    @MoreKeys(2)
    data class ComplexObject(val m: ObjectTypeHandlerTest.MyObject, val x: Int, val s: String)


    @Test
    fun testObjectType() {
        val myObjectHandler = ObjectTypeRefHandler(
            "m",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, ObjectTypeHandlerTest.TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, ObjectTypeHandlerTest.TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, ObjectTypeHandlerTest.TestValueGetter(592)),
            )
        )
        val handler = ObjectTypeHandler(
            false,
            MoreKeysData(2, false),
            listOf(myObjectHandler) + listOf(
                NativeTypeHandler(NativeType.INT, "x", false, ObjectTypeHandlerTest.TestValueGetter(586)),
                NativeTypeHandler(NativeType.STRING, "s", false, ObjectTypeHandlerTest.TestValueGetter("Hello")),
            )
        )
        assertEquals("m_i INT NOT NULL, m_s TEXT NOT NULL, x INT NOT NULL, s TEXT NOT NULL", handler.toHeader())
        assertEquals("m_i, m_s, x, s", handler.insertHead())
    }

    @Test
    fun testObjectTypeRef() {
        val myObjectHandler = ObjectTypeRefHandler(
            "m",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, ObjectTypeHandlerTest.TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, ObjectTypeHandlerTest.TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, ObjectTypeHandlerTest.TestValueGetter(592)),
            )
        )
        val handler = ObjectTypeRefHandler(
            "c",
            false,
            MoreKeysData(2, false),
            listOf(myObjectHandler) + listOf(
                NativeTypeHandler(NativeType.INT, "x", false, ObjectTypeHandlerTest.TestValueGetter(586)),
                NativeTypeHandler(NativeType.STRING, "s", false, ObjectTypeHandlerTest.TestValueGetter("Hello")),
            )
        )
        assertEquals("c_m_i INT NOT NULL, c_m_s TEXT NOT NULL, c_x INT NOT NULL", handler.toHeader())
        assertEquals("c_m_i, c_m_s, c_x", handler.insertHead())
    }
}
