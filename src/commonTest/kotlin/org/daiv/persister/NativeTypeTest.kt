package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeTest {

    @Test
    fun testNativeType() {
        val handler = memberValueGetter<Any, Int>("i", false) {
            throw RuntimeException("test should not use getValue")
        } as NativeTypeHandler
        assertEquals(Row("5"), handler.insertValue(5))
        assertEquals("i", handler.insertHead())
        assertEquals("i INT NOT NULL", handler.toHeader())
        assertEquals(5, handler.getValue(DatabaseRunner(listOf(5))).list.last())
    }
}