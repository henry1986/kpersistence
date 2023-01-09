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
            "m",
            false,
            MoreKeysData(2, false),
            listOf(
                NativeTypeHandler(NativeType.INT, "i", false, TestValueGetter(5)),
                NativeTypeHandler(NativeType.STRING, "s", false, TestValueGetter("Hello")),
                NativeTypeHandler(NativeType.LONG, "x", false, TestValueGetter(592)),
            )
        )
        assertEquals("m", handler.insertHead())
        assertEquals("m_i INT NOT NULL, m_s TEXT NOT NULL", handler.toHeader())
    }
}
