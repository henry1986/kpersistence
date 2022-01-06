package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.runTest
import org.junit.Test
import kotlin.test.assertEquals

class SQIInterpreterTester {

    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val x: Int, val s: SimpleObject)

    @Test
    fun testSQL() = runTest {
        val sqlRequest = "SELECT * FROM SimpleObject WHERE x = ? AND y = ?;"
        val tableName = "SimpleObject"
        val map = CormMap()
        val mapper = map.getValue(SimpleObject::class)
        val sqlInterpreter = SQLInterpreter(mapper, map)
        assertEquals(sqlRequest, sqlInterpreter.select(tableName, listOf(SimpleObject::x.name, SimpleObject::y.name)))
    }

    @Test
    fun testSQLComplex() = runTest {
        val sqlRequest = "SELECT * FROM ComplexObject WHERE s_x = ?;"
        val tableName = "ComplexObject"
        val map = CormMap()
        val mapper = map.getValue(ComplexObject::class)
        val sqlInterpreter = SQLInterpreter(mapper, map)
        assertEquals(sqlRequest, sqlInterpreter.select(tableName, listOf(ComplexObject::s.name)))
    }
}