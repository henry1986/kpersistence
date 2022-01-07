package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.daiv.persister.objectrelational.ReflectionObjectRelationalMapperTest.*
import org.daiv.persister.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestComplexObject {
    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val id: Int, val comment: String, val s: SimpleObject)

    private val calculationMap = CormMap()

    @Test
    fun testHeader() = runTest {
        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val header = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalHeader
        
        val headerData = ObjectRelationalHeaderData(
            listOf(HeadEntry("id", "Int", true)),
            listOf(HeadEntry("comment", "String", false), HeadEntry("s_x", "Int", false)),
            listOf { simpleMapper.objectRelationalHeader }
        )
        assertEquals(headerData, header)
    }

    @Test
    fun testInsert() = runTest {
        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val complexObject = ComplexObject(5, "this is crap", SimpleObject(9, "Hello"))
        val writer = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalWriter
        assertTrue { writer.list.size == 1 }
        val first = writer.list.first()
        assertTrue { first is ObjectRelationalWriterMap<*, *> }
        first as ObjectRelationalWriterMap<*, *>
        assertEquals(simpleMapper.objectRelationalWriter, first.objectRelationalWriter)
        val got = writer.writeKey(null, complexObject, HashCodeCounterGetter.nullGetter)
        assertEquals(listOf(WriteEntry("id", 5, true)), got)
        val written = writer.write(listOf(), complexObject, HashCodeCounterGetter.nullGetter)
        assertEquals(
            listOf(
                WriteRow(
                    listOf(
                        WriteEntry("comment", "this is crap", false),
                        WriteEntry("s_x", 9, false)
                    )
                )
            ), written
        )
        val channel = Channel<Unit>()
        writer.subs(complexObject, object : TaskReceiver {
            override suspend fun <R> task(r: R, higherKeys: List<WriteEntry>, mapper: ObjectRelationalWriter<R>) {
                val written = mapper.write(higherKeys, r, HashCodeCounterGetter.nullGetter)
                val keys = mapper.writeKey(null, r, HashCodeCounterGetter.nullGetter)
                assertEquals(listOf(WriteEntry("x", 9, true)), keys)
                assertEquals(listOf(WriteRow(listOf(WriteEntry("y", "Hello", false)))), written)
                GlobalScope.launch {
                    channel.send(Unit)
                }
            }
        }, HashCodeCounterGetter.nullGetter)
        channel.receive()
//        assertEquals(writerData, writer.list.map { it as ObjectRelationalWriterMap })
    }

//    @Test
//    fun testKeysComplex() = runTest {
//        val keys = calculationMap.createKeys(ClassParameterImpl(ComplexObject::class))
//        assertEquals(
//            mapOf(SimpleObject::class to SimpleObject::class),
//            keys.map { it.key to it.value.classParameter.clazz }.toMap()
//        )
//    }

    @Test
    fun testRead() = runTest {
//        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val complexObject = ComplexObject(5, "this is crap", SimpleObject(9, "Hello"))
        val reader = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalReader
        val dataRequester = object : DataRequester {
            override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
                println("key: $key")
                if (key == listOf(ReadEntry(9))) {
                    return complexObject.s as T
                }
                return null as T
            }
        }
        val key = reader.readKey(ReadCollection(ListNativeReads(listOf(listOf(5))), dataRequester))
        assertEquals(listOf(ReadEntry(5)), key)
        val read = reader.read(ReadCollection(ListNativeReads(listOf(listOf(5, "this is crap", 9))), dataRequester))
        assertEquals(complexObject, read)
    }
}
