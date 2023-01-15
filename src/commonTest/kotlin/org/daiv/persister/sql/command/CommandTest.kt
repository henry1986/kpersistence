package org.daiv.persister.sql.command

import org.daiv.persister.Row
import kotlin.test.Test
import kotlin.test.assertEquals

class CommandTest {

    val tableName = "MyObject"
    val command = DefaultCommand(tableName)

    @Test
    fun testCreateTable() {
        val row = Row("i INT NOT NULL", "s TEXT NOT NULL", "x LONG NOT NULL")
        val primaryKey = Row("i", "s")
        assertEquals(
            "CREATE TABLE IF NOT EXISTS `$tableName` (i INT NOT NULL, s TEXT NOT NULL, x LONG NOT NULL, PRIMARY KEY(i, s));",
            command.createTable(CreateTableData(row, primaryKey))
        )
    }

    @Test
    fun testInsert() {
        val row = Row("i", "s", "x")
        val values = listOf(Row("5", "\"Hello\"", "9"), Row("6", "\"World\"", "54"), Row("7", "\"Mine\"", "546"))
        assertEquals(
            "INSERT INTO `$tableName` (i, s, x) VALUES (5, \"Hello\", 9), (6, \"World\", 54), (7, \"Mine\", 546);",
            command.insert(InsertTableData(row, values))
        )
    }

    @Test
    fun testSelectAll() {
        assertEquals("SELECT * FROM `$tableName`;", command.selectAll(SelectAllTableData))
    }

    val selectData = DefaultSelectKeyTableData(Row("i", "s"), Row("5", "\"Hello\""))

    @Test
    fun testSelectKey() {
        assertEquals(
            "SELECT * FROM `${tableName}` WHERE i = 5 AND s = \"Hello\";",
            command.selectKey(selectData)
        )
    }

    @Test
    fun testDelete() {
        assertEquals(
            "DELETE FROM `${tableName}` WHERE i = 5 AND s = \"Hello\";",
            command.deleteKey(selectData)
        )
    }

    @Test
    fun testUpdate() {
        assertEquals(
            "UPDATE `$tableName` SET i = 9, x = 856 WHERE i = 5 AND s = \"Hello\";",
            command.updateTable(
                UpdateSelectKeyTableData(
                    DefaultSelectKeyTableData(Row("i", "s"), Row("5", "\"Hello\"")),
                    DefaultSelectKeyTableData(Row("i", "x"), Row("9", "856"))
                )
            )
        )
    }
}
