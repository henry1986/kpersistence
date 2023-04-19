package org.daiv.persister

import org.daiv.sql.DatabaseHandler
import org.daiv.sql.ReadWriter
import org.junit.Test
import kotlin.test.AfterTest

class SQLTester {
    val handler = DatabaseHandler("testDB.db")

    @Test
    fun test(){
        val r = ReadWriter(handler)
        r.write("create table if not exists Positions (i INT, title Text, other Long, PRIMARY KEY(i));")
        r.write("insert into Positions (i, title, other) values (5, null, 9);")
        r.write("insert into Positions (i, title, other) values (6, \"world\", 5);")
        r.read("select * from Positions;"){ read ->
            read.next()
            val columnCount = read.metaData.columnCount

            println("columnCount: $columnCount")
            println("className: ${read.metaData.getColumnTypeName(1)}")
            println("className: ${read.metaData.getColumnTypeName(2)}")
            println("className: ${read.metaData.getColumnTypeName(3)}")

            read.getObject(1)
            println("${read.getObject(1)}")
            val got = read.getObject(2)
            if(got == null){
                println("got null")
            }
            println("${read.getObject(2)}")
            read.next()
            println("${read.getObject(1)}")
            println("${read.getObject(2)}")
        }
    }

    @AfterTest
    fun afterTest(){
        handler.delete()
    }
}
