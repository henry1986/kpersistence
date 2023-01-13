package org.daiv.persister

import kotlin.test.Test
import kotlin.test.assertEquals

class ObjectTypeHandlerTest {
    @MoreKeys(2)
    class MyObject(val i: Int, val s: String, val x: Long)

    val handler = objectType<MyObject>(
        listOf(
            memberValueGetter("i", false) { i },
            memberValueGetter("s", false) { s },
            memberValueGetter("x", false) { x },
        )
    )
    val refHandler = memberValueGetter<Any, MyObject>(
        "m", false, MoreKeysData(2), listOf(
            memberValueGetterCreator("i", false) { i },
            memberValueGetterCreator("s", false) { s },
            memberValueGetterCreator("x", false) { x },
        )
    ) {
        throw RuntimeException("test should not use getValue")
    }

    @Test
    fun test() {
        assertEquals(
            Row("i INT NOT NULL", "s TEXT NOT NULL", "x LONG NOT NULL"), handler.toHeader()
        )
        assertEquals(Row("i", "s", "x"), handler.insertHead())
        val insert = handler.insertValue(MyObject(5, "Hello", 90L))
        assertEquals(Row("5", "\"Hello\"", "90"), insert)
    }

    @Test
    fun testRefHandler() {
        assertEquals(Row("m_i INT NOT NULL", "m_s TEXT NOT NULL"), refHandler.toHeader())
        assertEquals(Row("m_i", "m_s"), refHandler.insertHead())
        assertEquals(Row("5", "\"Hello\""), refHandler.insertValue(MyObject(5, "Hello", 90)))
    }

    @Test
    fun testDatabaseRead() {
        val toRead = listOf(5, "Hello", 90L)
        val got = handler.getValue(DatabaseRunner(toRead))
        assertEquals(toRead, got.list)
    }

    @Test
    fun testToValue() {
        val expect = MyObject(5, "Hello", 9)
        val tableReader = DefaultTableReader(mapOf(listOf(5, "Hello") to expect))
        val got = refHandler.toValue(
            listOf(5, "Hello"), DefaultTableCollector(
                listOf(MyObject::class pairedWith tableReader), emptyMap()
            )
        )
        assertEquals(expect, got)
    }
}

class TestComplexObjectType {
    @MoreKeys(2)
    data class ComplexObject(val m: ObjectTypeHandlerTest.MyObject, val x: Int, val s: String)

    private val myObjectHandler = memberValueGetterCreator<ComplexObject, ObjectTypeHandlerTest.MyObject>(
        "m", false, MoreKeysData(2), listOf(
            memberValueGetterCreator("i", false) { i },
            memberValueGetterCreator("s", false) { s },
            memberValueGetterCreator("x", false) { x },
        )
    ) { m }
    private val complexObjectMember = listOf(myObjectHandler) + listOf(
        memberValueGetterCreator<ComplexObject, Int>("x", false) { x },
        memberValueGetterCreator<ComplexObject, String>("s", false) { s },
    )

    private val complexObjectTypeHandler = complexObjectMember.map { it.create() }

    @Test
    fun testObjectType() {
        val handler = objectType(complexObjectTypeHandler)
        assertEquals(
            Row("m_i INT NOT NULL", "m_s TEXT NOT NULL", "x INT NOT NULL", "s TEXT NOT NULL"),
            handler.toHeader()
        )
        assertEquals(Row("m_i", "m_s", "x", "s"), handler.insertHead())
        assertEquals(
            Row("5", "\"Hello\"", "1", "\"World\""),
            handler.insertValue(ComplexObject(ObjectTypeHandlerTest.MyObject(5, "Hello", 95L), 1, "World"))
        )
    }

    @Test
    fun testObjectTypeRef() {
        val handler = memberValueGetterCreator<Any, ComplexObject>("c", false, MoreKeysData(2), complexObjectMember) {
            throw RuntimeException()
        }.create()

        assertEquals(Row("c_m_i INT NOT NULL", "c_m_s TEXT NOT NULL", "c_x INT NOT NULL"), handler.toHeader())
        assertEquals(Row("c_m_i", "c_m_s", "c_x"), handler.insertHead())
        assertEquals(
            Row("5", "\"Hello\"", "1"),
            handler.insertValue(ComplexObject(ObjectTypeHandlerTest.MyObject(5, "Hello", 95L), 1, "World"))
        )
    }

    @Test
    fun testDatabaseRead() {
        val handler = objectType(complexObjectTypeHandler)
        val toRead = listOf(5, "Hello", 1, "World")
        val got = handler.getValue(DatabaseRunner(toRead))
        assertEquals(toRead, got.list)
    }

//    @Test
//    fun testToValue() {
//        val expect = ObjectTypeHandlerTest.MyObject(5, "Hello", 9)
//        val tableReader = DefaultTableReader(mapOf(listOf(5, "Hello") to expect))
//        val got = refHandler.toValue(
//            listOf(5, "Hello"), DefaultTableCollector(
//                listOf(ObjectTypeHandlerTest.MyObject::class pairedWith tableReader)
//            )
//        )
//        assertEquals(expect, got)
//    }
}
