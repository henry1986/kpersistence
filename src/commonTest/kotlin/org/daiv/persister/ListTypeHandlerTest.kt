package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ListTypeHandlerTest {
    class ListHolder(val i: Int, val l: List<Int>)

    val listTypeHandler = ListTypeHandler(
        ListNativeTypeHandler(NativeType.INT, "key_i", false),
        ListNativeTypeHandler(NativeType.INT, "value_l", false),
        { listHolder -> listHolder.i },
        ListTypeReader<ListHolder, Int> { listHolder -> listHolder.l }
    )

    @Test
    fun test() {
        assertEquals("key_i INT NOT NULL, index INT NOT NULL, value_l INT NOT NULL", listTypeHandler.toHeader())
        assertEquals("key_i, index, value_l", listTypeHandler.insertHead())
        assertEquals(
            listOf(Row("5", "0", "6"), Row("5", "1", "7")),
            listTypeHandler.insertValue(ListHolder(5, listOf(6, 7)))
        )
        val got: List<DRow> = listTypeHandler.getValue(
            DatabaseRunner(
                DefaultDatabaseReader(listOf(listOf(5, 0, 6), listOf(5, 1, 7), listOf(5, 2, 8))),
                1
            )
        ).rows
        assertEquals(listOf(DRow(listOf(5, 0, 6)), DRow(listOf(5, 1, 7)), DRow(listOf(5, 2, 8))), got)
    }

    @Test
    fun testToList() {
        val list = listOf(DRow(listOf(5, 0, 6)), DRow(listOf(5, 1, 7)), DRow(listOf(5, 2, 8)))
        val expect = ListHolder(5, listOf(6, 7, 8))
        assertEquals(expect.l, listTypeHandler.getList(list, DefaultTableCollector(emptyList())))
    }
}
