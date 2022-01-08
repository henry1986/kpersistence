package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.HashCodeCounterGetter
import org.daiv.persister.runTest
import org.junit.Test
import java.sql.DriverManager
import kotlin.test.assertEquals

class SQIInterpreterTester {

    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val x: Int, val s: SimpleObject)

    private val cormMap = CormMap()

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

    private data class XP(val x1: Int, val x2: String)

    @Test
    fun test() = runTest {
        val path = "test.db"
        val driver = DriverManager.getConnection("jdbc:sqlite:$path")
        val create = "CREATE TABLE IF NOT EXISTS XP (x1 INT NOT NULL, x2 String NOT NULL, PRIMARY KEY(x1));"

        val x = cormMap.getValue(XP::class)
        val h = HashCodeCounterGetter.nullGetter
        val d =  x.objectRelationalWriter.writeRow(null, XP(5, "Blub"), h)
        val join = d.joinToString(", ") { "${it.name}" }
        val values = d.joinToString(", ") { "${it.value}" }
        println("join: $join")
        println("values: $values")
        println("writeKey: $d")
//        val insert = "insert into XP (x1, x2) values (?, ?), (?, ?);"
//        val st = driver.prepareStatement(insert)
//        st.setInt(1, 5)
//        st.setString(2, "Blub")
//        st.setInt(3, 6)
//        st.setString(4, "Wow")
//        st.execute()
//        st.close()
    }
}