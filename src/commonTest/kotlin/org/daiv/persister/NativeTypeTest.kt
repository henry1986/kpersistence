package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeTest {

    @Test
    fun testNativeType() {
        val handler = memberValueGetter<Any, Int>("i", false, valueFactory = { Any() }) {
            throw RuntimeException("test should not use getValue")
        } as NativeTypeHandler
        assertEquals(Row("5"), handler.insertValue(5))
        assertEquals(Row("i"), handler.insertHead())
        assertEquals(Row("i INT NOT NULL"), handler.toHeader())
        assertEquals(5, handler.getValue(DatabaseRunner(listOf(5))).list.last())
    }
}

class ColumnRefTest {

    @Test
    fun test() {
        val ref = ColumnRef( "i")
        val ref2 = ColumnRef( "m", ref)
        assertEquals("m_i", ref2.buildName())
        val ref3 = ColumnRef("c", ref2)
        assertEquals("c_m_i", ref3.buildName())
    }
}