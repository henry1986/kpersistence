package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.modules.SerializersModule
import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
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
        fun Int.key() = DBEntry("key", Int.serializer().descriptor, this, true)
        fun Int.value() = DBEntry("value", Int.serializer().descriptor, this, false)
        assertEquals(
            listOf(
                DBRow(listOf(0.key(), 2.value())),
                DBRow(listOf(1.key(), 5.value())),
                DBRow(listOf(2.key(), 6.value())),
            ),
            done.rows
        )
    }


    @Test
    fun intWithPKeyTest() = runTest {
        val intList = listOf(2, 5, 6)
        val module = SerializersModule { }
        val s = ListSerializer(Int.serializer())
        val keyEntry = DBEntry("pkey_i", Int.serializer().descriptor, 0, true)

        val done = s.createListCollection(CollectionInsertData(keyEntry, intList), ElementAdder.noAdd, module)
        fun Int.key() = DBEntry("key", Int.serializer().descriptor, this, true)
        fun Int.value() = DBEntry("value", Int.serializer().descriptor, this, false)
        val e1 = listOf(
            DBRow(listOf(keyEntry, 0.key(), 2.value())),
            DBRow(listOf(keyEntry, 1.key(), 5.value())),
            DBRow(listOf(keyEntry, 2.key(), 6.value())),
        )
        assertEquals(e1, done.rows)
    }

    @Serializable
    data class SimpleWithList(val i: Int, val l: List<Int>)

    @Test
    fun intWithPKeyInObjectTest() = runTest {
        val intList = listOf(2, 5, 6)
        val simpleWithList = SimpleWithList(5, intList)
        val module = SerializersModule { }
        val s = SimpleWithList.serializer()
        val cache = InsertionCache(DefaultScopeContextable())
        InsertElementAdder(cache, module).addElement(s, simpleWithList)
        cache.join()
        val all = cache.all().flatMap { it.all() }
        all.forEach {
            println("it: $it")
        }
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
        fun Int.key() = DBEntry("key", Int.serializer().descriptor, this, true)
        fun Int.value() = DBEntry("value_x", Int.serializer().descriptor, this, false)
        val e1 = listOf(
            DBRow(listOf(0.key(), 5.value())),
            DBRow(listOf(1.key(), 7.value())),
            DBRow(listOf(2.key(), 8.value())),
        )
        assertEquals(e1, done.rows)
    }

    @Test
    fun insertCacheTest() = runTest {
        val list = listOf(SimpleClass(5, "Hello"), SimpleClass(7, "Aber"), SimpleClass(8, "No"))
        val cache = InsertionCache(DefaultScopeContextable())

        val s = ListSerializer(SimpleClass.serializer())
        InsertElementAdder(cache, SerializersModule { }).addList(s, CollectionInsertData(DBEntry("", Int.serializer().descriptor, 5, false), list))
        cache.join()
        val res = cache.all()

        if (res.isEmpty()) {
            println("isEmpty")
        }
        res.forEach {
            val all = it.all()
            all.forEach {
                println("it: $it")
            }
        }
//        assertEquals(
//            listOf(
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 0), DBEntry("value_x", Int.serializer().descriptor, 5))),
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 1), DBEntry("value_x", Int.serializer().descriptor, 7))),
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 2), DBEntry("value_x", Int.serializer().descriptor, 8))),
//            ),
//
//        )
//        val module = SerializersModule { }
//        val dbCollector = DBMutableCollector()
//        val p = PEncoder(
//            module,
//            ListEncoderStrategyFactory(dbCollector, ElementAdder.noAdd)
//        )
//        s.serialize(p, list)
//        val done = dbCollector.done()
////        InsertionResult()
//        assertEquals(
//            listOf(
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 0), DBEntry("value_x", Int.serializer().descriptor, 5))),
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 1), DBEntry("value_x", Int.serializer().descriptor, 7))),
//                DBRow(listOf(DBEntry("key", Int.serializer().descriptor, 2), DBEntry("value_x", Int.serializer().descriptor, 8))),
//            ),
//            done.rows
//        )
    }
}