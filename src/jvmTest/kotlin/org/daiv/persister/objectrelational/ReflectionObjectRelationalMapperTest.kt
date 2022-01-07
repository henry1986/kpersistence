package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.daiv.persister.runTest
import org.junit.Test
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class ReflectionObjectRelationalMapperTest {

    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val cx: Int, val s: SimpleObject)

    private val calculationMap = CormMap()

    @Test
    fun testReflection() {
        val p = SimpleObject::class.declaredMemberProperties
        val s = SimpleObject(5, "World")
        val list = listOf(1, 5, 9)
        runBlocking {
            list.asFlow().map {
                GlobalScope.launch {
                    println("before delay $it")
                    delay(100)
                    println("after delay $it")
                    delay(100)
                    println("after delay $it")
                }
            }.toList().forEach { it.join() }
        }
    }

    @Test
    fun testSimpleHeader() = runTest {
        val mapper = SimpleObject::class.objectRelationMapper(calculationMap)
        val header = mapper.objectRelationalHeader
        val chdMap = mapper.classHeaderData.parameters.first().chdMap
        val p1 = SimpleParameter(SimpleObject::class, "x", typeOf<Int>(), KeyType.NORM, chdMap)
        val p2 = SimpleParameter(SimpleObject::class, "y", typeOf<String>(), KeyType.NO_KEY, chdMap)
        val head = listOf(HeadEntry(p1, "x", "Int", true), HeadEntry(p2, "y", "String", false))
        assertEquals(head.drop(1), header.headOthers())
        assertEquals(head.take(1), header.keyHead(null, p2))
        assertEquals(head.take(1).map { it.copy(name = "s_${it.name}") }, header.keyHead("s", p2))
    }

    @Test
    fun testWrite() = runTest {
        val mapper = SimpleObject::class.objectRelationMapper(calculationMap).objectRelationalWriter
        val writerData =
            ObjectRelationalWriterData<SimpleObject>(
                listOf(DefaultPreWriteEntry("x", true) {}),
                listOf(DefaultPreWriteEntry("y", false) {}),
                listOf()
            )
        assertEquals(writerData, mapper)
    }

    @Test
    fun testWriteComplex() = runTest {
        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val mapper = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalWriter
        val writerData =
            ObjectRelationalWriterData<ComplexObject>(
                listOf(DefaultPreWriteEntry("cx", true) { cx }),
                listOf(DefaultPreWriteEntry("s_x", false) { s }),
                listOf(ObjectRelationalWriterMap(simpleMapper.objectRelationalWriter) { s })
            )
        assertEquals(writerData, mapper)
    }

    fun List<Any>.nativeReads() = ListNativeReads(listOf(this), 0, 0)

    val dataRequester: DataRequester = object : DataRequester {
        override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
            return objectRelationalMapper.read(ReadCollection(listOf<Any>().nativeReads(), this))
        }
    }

    @Test
    fun testRead() = runTest {
        val reader = SimpleObject::class.objectRelationMapper(calculationMap).objectRelationalReader
        val readerData = ObjectRelationalReaderData(
            SimpleObject::class,
            listOf(ReadEntryTask("x") {}),
            listOf(ReadEntryTask("y") {})
        ) { SimpleObject(5, "Hello") }
        assertEquals(readerData, reader)
    }

    @Test
    fun testReadComplex() = runTest {
        val reader = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalReader
        val readerData = ObjectRelationalReaderData(
            ComplexObject::class,
            listOf(ReadEntryTask("cx") {}),
            listOf(ReadEntryTask("s") {})
        ) { ComplexObject(9, SimpleObject(5, "Hello")) }
        assertEquals(readerData, reader)
    }
}