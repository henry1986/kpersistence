package org.daiv.persister.table

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.*
import org.daiv.persister.table.InsertTableTest.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ListTests {
    @Test
    fun intTest() = runTest {
        val intList = listOf(2, 5, 6)
        val module = SerializersModule { }
        val s = ListSerializer(Int.serializer())
        val dbCollector = DBMutableCollector()
        val p = PEncoder(
            module,
            ListEncoderStrategyFactory(dbCollector, ElementAdder.noAdd)
        )
        s.serialize(p, intList)
        val done = dbCollector.done()
        assertEquals(
            listOf(
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 0), DBEntry("value", Int.serializer().descriptor, 2))),
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 1), DBEntry("value", Int.serializer().descriptor, 5))),
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 2), DBEntry("value", Int.serializer().descriptor, 6))),
            ),
            done.rows
        )
    }

    @Test
    fun complexTest() {
        val list = listOf(SimpleClass(5, "Hello"), SimpleClass(7, "Aber"), SimpleClass(8, "No"))
        val module = SerializersModule { }
        val s = ListSerializer(SimpleClass.serializer())
        val dbCollector = DBMutableCollector()
        val p = PEncoder(
            module,
            ListEncoderStrategyFactory(dbCollector, ElementAdder.noAdd)
        )
        s.serialize(p, list)
        val done = dbCollector.done()
        assertEquals(
            listOf(
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 0), DBEntry("value_x", Int.serializer().descriptor, 5))),
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 1), DBEntry("value_x", Int.serializer().descriptor, 7))),
                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 2), DBEntry("value_x", Int.serializer().descriptor, 8))),
            ),
            done.rows
        )
    }
}