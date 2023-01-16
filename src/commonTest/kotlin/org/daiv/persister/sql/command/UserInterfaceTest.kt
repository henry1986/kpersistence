package org.daiv.persister.sql.command

import org.daiv.persister.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UserInterfaceTest {
    val tableName = "MyObject"
    val command = DefaultCommandImplementer(tableName, myObjectTypeHandler)

//    @Test
//    fun testPersist() {
//        assertEquals(
//            "CREATE TABLE IF NOT EXISTS `$tableName` (i INT NOT NULL, s TEXT NOT NULL, x LONG NOT NULL, PRIMARY KEY(i, s));",
//            command.persist()
//        )
//    }
//
//    @Test
//    fun testInsert() {
//        assertEquals(
//            "INSERT INTO `$tableName` (i, s, x) VALUES (5, \"Hello\", 9), (6, \"World\", 54), (7, \"Mine\", 546);",
//            command.insert(
//                listOf(
//                    ObjectTypeHandlerTest.MyObject(5, "Hello", 9),
//                    ObjectTypeHandlerTest.MyObject(6, "World", 54),
//                    ObjectTypeHandlerTest.MyObject(7, "Mine", 546)
//                )
//            )
//        )
//    }
//
//    @Test
//    fun testSelect() {
//        assertEquals(
//            "SELECT * FROM `${tableName}` WHERE i = 5 AND s = \"Hello\";",
//            command.select(listOf(5, "Hello"))
//        )
//    }

}