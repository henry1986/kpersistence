package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ListTypeHandlerTest {
    class ListHolder(val i: Int, val l: List<Int>)

    @Test
    fun test() {
        val l = ListTypeHandler(
            ListNativeTypeHandler(NativeType.INT, "key_i", false),
            ListNativeTypeHandler(NativeType.INT, "index", false),
            ListNativeTypeHandler(NativeType.INT, "value_l", false),
            object:ListTypeReader<Int, ListHolder, Int, Int>{
                override fun getList(listHolder: ListHolder): Map<Int, Int> {
                    return listHolder.l.asMap()
                }

                override fun getPrimaryKey(listHolder: ListHolder): Int {
                    return listHolder.i
                }
            }
        )
        assertEquals("key_i INT NOT NULL, index INT NOT NULL, value_l INT NOT NULL", l.toHeader())
        assertEquals("key_i, index, value_l", l.insertHead())
        assertEquals(listOf(Row("5", "0", "6"), Row("5", "1", "7")), l.insertValue(ListHolder(5, listOf(6, 7))))
    }
}