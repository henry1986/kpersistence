package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectTypeHandlerTest {
    @MoreKeys(2)
    class MyObject(val i: Int, val s: String, val x: Long)

    @Test
    fun test() {
        val handler = ObjectTypeHandler<Any, MyObject>(
            false,
            MoreKeysData(2, false),
            listOf(
                memberValueGetter("i", false) { i },
                memberValueGetter("s", false) { s },
                memberValueGetter("x", false) { x },
            )
        )
        assertEquals(
            "i INT NOT NULL, s TEXT NOT NULL, x LONG NOT NULL", handler.toHeader()
        )
        assertEquals("i, s, x", handler.insertHead())
        assertEquals("5, \"Hello\", 90", handler.insertValue(MyObject(5, "Hello", 90L)))
    }

    @Test
    fun testRefHandler() {
        val handler = memberValueGetter<Any, MyObject>(
            "m", false, MoreKeysData(2), listOf(
                memberValueGetterCreator("i", false) { i },
                memberValueGetterCreator("s", false) { s },
                memberValueGetterCreator("x", false) { x },
            )
        ) {
            throw RuntimeException("test should not use getValue")
        }
        assertEquals("m_i INT NOT NULL, m_s TEXT NOT NULL", handler.toHeader())
        assertEquals("m_i, m_s", handler.insertHead())
        assertEquals("5, \"Hello\"", handler.insertValue(MyObject(5, "Hello", 90)))
    }
}

class TestComplexObjectType {
    @MoreKeys(2)
    data class ComplexObject(val m: ObjectTypeHandlerTest.MyObject, val x: Int, val s: String)

    private val myObjectHandler = ObjectTypeRefHandler<ComplexObject, ObjectTypeHandlerTest.MyObject>(
        "m",
        false,
        MoreKeysData(2, false),
        listOf(
            memberValueGetter("i", false) { i },
            memberValueGetter("s", false) { s },
            memberValueGetter("x", false) { x },
        )
    ) { it.m }

    @Test
    fun testObjectType() {
        val handler = ObjectTypeHandler<Any, ComplexObject>(
            false,
            MoreKeysData(2, false),
            listOf(myObjectHandler) + listOf(
                memberValueGetter("x", false) { x },
                memberValueGetter("s", false) { s },
            )
        )
        assertEquals("m_i INT NOT NULL, m_s TEXT NOT NULL, x INT NOT NULL, s TEXT NOT NULL", handler.toHeader())
        assertEquals("m_i, m_s, x, s", handler.insertHead())
        assertEquals(
            "5, \"Hello\", 1, \"World\"",
            handler.insertValue(ComplexObject(ObjectTypeHandlerTest.MyObject(5, "Hello", 95L), 1, "World"))
        )
    }

    @Test
    fun testObjectTypeRef() {
        val handler = ObjectTypeRefHandler<Any, ComplexObject>(
            "c",
            false,
            MoreKeysData(2, false),
            listOf(myObjectHandler) + listOf(
                memberValueGetter("x", false) { x },
                memberValueGetter("s", false) { s },
            )
        ) {
            throw RuntimeException("test should not use getValue")
        }
        assertEquals("c_m_i INT NOT NULL, c_m_s TEXT NOT NULL, c_x INT NOT NULL", handler.toHeader())
        assertEquals("c_m_i, c_m_s, c_x", handler.insertHead())
        assertEquals(
            "5, \"Hello\", 1",
            handler.insertValue(ComplexObject(ObjectTypeHandlerTest.MyObject(5, "Hello", 95L), 1, "World"))
        )
    }
}
