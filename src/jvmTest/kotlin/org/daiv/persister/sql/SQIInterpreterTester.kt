package org.daiv.persister.sql

import org.daiv.persister.MoreKeys
import org.daiv.persister.objectrelational.*
import org.daiv.persister.runTest
import org.junit.Test
import java.sql.DriverManager
import kotlin.reflect.KClass
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
    @MoreKeys(2)
    private data class XP2(val x1: Int, val x2: String, val x3:Int)
    private data class XPHandler(val x1: Int, val x2: XP, val xp2: XP2)

    @Test
    fun test() = runTest {
        val path = "test.db"
        val driver = DriverManager.getConnection("jdbc:sqlite:$path")
        val create = "CREATE TABLE IF NOT EXISTS XP (x1 INT NOT NULL, x2 String NOT NULL, PRIMARY KEY(x1));"

        val x = cormMap.getValue(XPHandler::class)
        val sqlInterpreter = SQLInterpreter(x, cormMap)
        val xpHandler1 = XPHandler(5, XP(9, "Blub"), XP2(1, "h1", 19))
        val xpHandler2 = XPHandler(3, XP(36, "Blub36"), XP2(2, "h2", 29))
        val insert = sqlInterpreter.insert(listOf(xpHandler1, xpHandler2))
        val h = HashCodeCounterGetter.nullGetter
        x.objectRelationalWriter.subs(xpHandler1, object : TaskReceiver{
            override suspend fun <R> task(r: R, higherKeys: List<WriteEntry>, mapper: ObjectRelationalWriter<R>) {
                r?.let {
                    val c = it::class as KClass<Any>
                    val sqlInterpreter = SQLInterpreter(cormMap.getValue(c), cormMap)
                    val ret = sqlInterpreter.insert(listOf(r))
                    println("writeKeySub: ${ret.first}")
                    ret.second.forEach {
                        println("itSub: $it")
                    }
                }
            }
        }, h)
        println("writeKey: ${insert.first}")
        insert.second.forEach {
            println("it: $it")
        }
//        val insert = "insert into XP (x1, x2) values (?, ?), (?, ?);"
//        val st = driver.prepareStatement(insert)
//        st.setInt(1, 5)
//        st.setString(2, "Blub")
//        st.setInt(3, 6)
//        st.setString(4, "Wow")
//        st.execute()
//        st.close()
    }

    private data class ListHolder(val x1: Int, val l:List<String>)

    @Test
    fun testSQLList() = runTest{
        val x = cormMap.getValue(ListHolder::class)
        val sqlInterpreter = SQLInterpreter(x, cormMap)
        val holder = ListHolder(5, listOf("Hello", "World"))
        val insert = sqlInterpreter.insert(listOf(holder))

        println("insert: ${insert.first}")
        insert.second.forEach {
            println("it: $it")
        }

        x.objectRelationalWriter.subs(holder, object : TaskReceiver{
            override suspend fun <R> task(r: R, higherKeys: List<WriteEntry>, mapper: ObjectRelationalWriter<R>) {
                r?.let {
                    val c = it::class as KClass<Any>
                    val sqlInterpreter = SQLInterpreter(cormMap.getValue(c), cormMap)
                    val ret = sqlInterpreter.insert(listOf(r))
                    println("writeKeySub: ${ret.first}")
                    ret.second.forEach {
                        println("itSub: $it")
                    }
                }
            }
        }, HashCodeCounterGetter.nullGetter)

    }
}