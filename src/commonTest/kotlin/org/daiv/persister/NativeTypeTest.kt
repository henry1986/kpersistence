package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeTest {

    @Test
    fun testNativeType() {
        val handler = memberValueGetter<Any, Int>("i", false) {
            throw RuntimeException("test should not use getValue")
        }
        assertEquals("5", handler.insertValue(5))
        assertEquals("i", handler.insertHead())
        assertEquals("i INT NOT NULL", handler.toHeader())
        assertEquals(5, handler.getValue(DatabaseRunner(object : DatabaseReader {
            override fun next(i: Int): Any? {
                if (i != 1) {
                    throw RuntimeException("not expected $i")
                }
                return 5
            }

            override fun nextLong(i: Int): Long? {
                TODO("Not yet implemented")
            }
        }, 1, emptyList())).list.last())
    }
}