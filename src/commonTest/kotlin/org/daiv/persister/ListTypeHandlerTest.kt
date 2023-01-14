package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ListTypeHandlerTest {
    data class ListHolder(val i: Int, val l: List<Int>)

    val listTypeHandler = ListTypeHandler(
        ListNativeTypeHandler(NativeType.INT, "key_i", false),
        ListNativeTypeHandler(NativeType.INT, "value_l", false),
        { listHolder -> listHolder.i },
        ListTypeReader<ListHolder, Int> { listHolder -> listHolder.l }
    )

    val listHolderHandler = objectType(
        listOf(
            memberValueGetter("i", false, valueFactory = { ListHolder(it[0] as Int, it[1] as List<Int>) }) { i },
            collection("l", false)
        ), MoreKeysData()
    ) {
        try {
            ListHolder(it[0] as Int, it[1] as List<Int>)
        } catch (t: Throwable) {
            println("got ${it.asList()}")
            throw t
        }
    }

    @Test
    fun test() {
        assertEquals(
            Row("key_i INT NOT NULL", "index INT NOT NULL", "value_l INT NOT NULL"),
            listTypeHandler.toHeader()
        )
        assertEquals(Row("key_i", "index", "value_l"), listTypeHandler.insertHead())
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
        val fromTable = listOf(
            DRow(listOf(5, 0, 6)),
            DRow(listOf(5, 1, 7)),
            DRow(listOf(5, 2, 8))
        )
        val expect = ListHolder(5, listOf(6, 7, 8))
        assertEquals(expect.l, listTypeHandler.getList(fromTable, DefaultTableCollector(emptyList(), emptyMap())))
    }

    @Test
    fun testToListHolder() {
        val read =
            listHolderHandler.getValue(DatabaseRunner(DefaultDatabaseReader(listOf(listOf(5))), 1)).rows.first()
        val v = listHolderHandler.toValue(
            read, DefaultTableCollector(
                emptyList(), mapOf(
                    (ListHolder::class to "l") to DefaultTableReader(
                        mapOf(listOf(5) to listOf(7, 8, 9))
                    )
                )
            )
        )
        assertEquals(ListHolder(5, listOf(7, 8, 9)), v)
    }

}
