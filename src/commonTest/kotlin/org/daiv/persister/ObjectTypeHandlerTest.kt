package org.daiv.persister

import kotlin.reflect.KProperty1
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A value factory that creates instances of [MyObject] using a list of values.
 *
 * @param it The list of values to use for constructing the [MyObject] instance.
 *           Must contain at least 3 values: an [Int], a [String], and a [Long].
 * @return A new instance of [MyObject] constructed from the given list of values.
 * @throws IllegalArgumentException If the given list of values does not contain exactly 3 values, or if any of the values
 *         are not of the expected type (i.e. [Int], [String], [Long]).
 */
val myObjectValueFactory = ValueFactory {
    ObjectTypeHandlerTest.MyObject(
        it[0] as Int,
        it[1] as String,
        it[2] as Long
    )
}

/**
 * ValueFactory that creates a new instance of ComplexObject class using the given constructor parameters
 */
val createComplexObject = ValueFactory {
    TestComplexObjectType.ComplexObject(
        it[0] as ObjectTypeHandlerTest.MyObject,
        it[0] as Int,
        it[1] as String,
    )
}

/**
 * Creates an ObjectTypeHandler for MyObject class with member value getters for 'i', 's', and 'x' properties.
 * The MoreKeysData parameter is used to specify the number of keys used to uniquely identify each object of the class.
 */
val myObjectTypeHandler = objectType(
    listOf(
        memberValueGetter("i", false, valueFactory = myObjectValueFactory) { i },
        memberValueGetter("s", false, valueFactory = myObjectValueFactory) { s },
        memberValueGetter("x", false, valueFactory = myObjectValueFactory) { x },
    ), MoreKeysData(2), myObjectValueFactory
)

class ObjectTypeHandlerTest {
    @MoreKeys(2)
    data class MyObject(val i: Int, val s: String, val x: Long)

    val refHandler = memberValueGetter(
        "m", false, MoreKeysData(2), listOf(
            memberValueGetterCreator("i", false, valueFactory = myObjectValueFactory) { i },
            memberValueGetterCreator("s", false, valueFactory = myObjectValueFactory) { s },
            memberValueGetterCreator("x", false, valueFactory = myObjectValueFactory) { x },
        ), valueFactory = { Any() }
    ) {
        throw RuntimeException("test should not use getValue")
    }

    @Test
    fun test() {
        assertEquals(
            Row("i INT NOT NULL", "s TEXT NOT NULL", "x LONG NOT NULL"), myObjectTypeHandler.toHeader()
        )
        assertEquals(Row("i", "s", "x"), myObjectTypeHandler.insertHead())
        val insert = myObjectTypeHandler.insertValue(MyObject(5, "Hello", 90L))
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
        val got = myObjectTypeHandler.getValue(DatabaseRunner(toRead))
        assertEquals(toRead, got.list)
    }

    @Test
    fun testToValue() {
        val expect = MyObject(5, "Hello", 9)
        val tableReader = DefaultTableReader(mapOf(listOf(5, "Hello") to expect))
        val got = refHandler.toValue(
            ColumnValues(emptyList(), listOf(5, "Hello")), DefaultTableCollector(
                listOf(MyObject::class pairedWith tableReader), emptyMap()
            )
        )
        assertEquals(expect, got)
    }
}

/**
 * This is an example of creating a member value getter for a complex object with nested properties.
 *
 * This getter retrieves the values of the nested properties "i", "s", and "x" of an object's member variable "m".
 *
 * @property myObjectValueFactory The [ValueFactory] to use when creating the final object.
 * @property createComplexObject A lambda that takes the values of the nested properties and returns the final object.
 * @property i A function that returns the value of the "i" property.
 * @property s A function that returns the value of the "s" property.
 * @property x A function that returns the value of the "x" property.
 * @property m The name of the member variable containing the nested properties.
 * @property valueFactory The [ValueFactory] to use when creating the intermediate values.
 * @property moreKeys The [MoreKeysData] specifying the number of keys to use for the intermediate values.
 */
private val myObjectDefaultMemberValueGetter = memberValueGetterCreator(
    "m", false, MoreKeysData(2), listOf(
        memberValueGetterCreator("i", false, valueFactory = myObjectValueFactory) { i },
        memberValueGetterCreator("s", false, valueFactory = myObjectValueFactory) { s },
        memberValueGetterCreator("x", false, valueFactory = myObjectValueFactory) { x },
    ), valueFactory = createComplexObject
) { m }

/**
 * A list of [ValueGetter] objects that represent the members of [TestComplexObjectType.ComplexObject].
 * This list is composed of [myObjectDefaultMemberValueGetter] and two other members, "x" and "s",
 * that use [createComplexObject] as value factory and "x" and "s" as name respectively.
 */
private val complexObjectMember = listOf(myObjectDefaultMemberValueGetter) + listOf(
    memberValueGetterCreator("x", false, valueFactory = createComplexObject) { x },
    memberValueGetterCreator("s", false, valueFactory = createComplexObject) { s },
)

private val complexObjectTypeHandlerList = complexObjectMember.map { it.create() }
val complexObjectTypeHandler = objectType(complexObjectTypeHandlerList, MoreKeysData(2)) {
    TestComplexObjectType.ComplexObject(
        it[0] as ObjectTypeHandlerTest.MyObject, it[1] as Int,
        it[2] as String
    )
}

class TestComplexObjectType {
    @MoreKeys(2)
    data class ComplexObject(val m: ObjectTypeHandlerTest.MyObject, val x: Int, val s: String)


    @Test
    fun testObjectType() {
        assertEquals(
            Row("m_i INT NOT NULL", "m_s TEXT NOT NULL", "x INT NOT NULL", "s TEXT NOT NULL"),
            complexObjectTypeHandler.toHeader()
        )
        assertEquals(Row("m_i", "m_s", "x", "s"), complexObjectTypeHandler.insertHead())
        assertEquals(
            Row("5", "\"Hello\"", "1", "\"World\""),
            complexObjectTypeHandler.insertValue(
                ComplexObject(
                    ObjectTypeHandlerTest.MyObject(5, "Hello", 95L),
                    1,
                    "World"
                )
            )
        )
    }

    @Test
    fun testObjectTypeRef() {
        val handler = memberValueGetterCreator(
            "c",
            false,
            MoreKeysData(2),
            complexObjectMember,
            valueFactory = { Any() }
        ) {
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
        val toRead = listOf(5, "Hello", 1, "World")
        val got = complexObjectTypeHandler.getValue(DatabaseRunner(toRead))
        assertEquals(toRead, got.list)
    }

    @Test
    fun testSelect(){
        val m: KProperty1<ComplexObject, ObjectTypeHandlerTest.MyObject> = ComplexObject::m
        val i = ObjectTypeHandlerTest.MyObject::i
        
    }

    @Test
    fun testKeyNames() {
        assertEquals(Row("i", "s"), myObjectTypeHandler.keyNames())
        assertEquals(Row("m_i", "m_s", "x"), complexObjectTypeHandler.keyNames())
    }

    @Test
    fun testToValue() {
        val expect = ObjectTypeHandlerTest.MyObject(5, "Hello", 9)
        val tableReader = DefaultTableReader(mapOf(listOf(5, "Hello") to expect))
        val got = myObjectTypeHandler.toValue(
            DRow(listOf(5, "Hello", 9L)), DefaultTableCollector(
                listOf(ObjectTypeHandlerTest.MyObject::class pairedWith tableReader), emptyMap()
            )
        )
        assertEquals(expect, got)
    }

}
