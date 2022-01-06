package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SQIInterpreterTester {

    private data class SimpleObject(val x:Int, val y:String)

    @Test
    fun testSQL() = runTest{
        val sqlRequest = "SELECT * from SimpleObject where x = ? and y = ?"
        val tableName = "SimpleObject"
        val map = CormMap()
        val mapper = map.getValue(SimpleObject::class)
        val sqlInterpreter = SQLInterpreter(mapper, map)
        assertEquals(sqlInterpreter.select(SimpleObject::x))
    }
}