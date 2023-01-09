package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeTest {

    @Test
    fun testNativeType() {
        val handler = NativeTypeHandler(NativeType.INT, "i", false, object : GetValue<Any, Int> {
            override fun get(any: Any): Int {
                return 5
            }
        })
        assertEquals("5", handler.insertValue(Any()))
        assertEquals("i", handler.insertHead())
        assertEquals("i INT NOT NULL", handler.toHeader())
        assertEquals(5, handler.getValue(DatabaseRunner(object : DatabaseReader {
            override fun next(i: Int): Any? {
                if(i != 1){
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