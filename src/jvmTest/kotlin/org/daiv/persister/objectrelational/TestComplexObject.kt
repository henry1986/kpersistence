package org.daiv.persister.objectrelational

import org.daiv.persister.objectrelational.ReflectionObjectRelationalMapperTest.*
import org.daiv.persister.table.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestComplexObject {
    data class ComplexObject(val id: Int, val comment: String, val s: SimpleObject)

    private val calculationMap = CalculationMap()

    @Test
    fun testHeader() = runTest {
        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val header = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalHeader
        val headerData = ObjectRelationalHeaderData(
            listOf(HeadEntry("id", "Int", true)),
            listOf(HeadEntry("comment", "String", false), HeadEntry("s_x", "Int", false)),
            listOf(simpleMapper.objectRelationalHeader)
        )
        assertEquals(headerData, header)
    }

    @Test
    fun testInsert() = runTest {
        val simpleMapper = calculationMap.getValue(SimpleObject::class)
        val complexObject = ComplexObject(5, "this is crap", SimpleObject(9, "Hello"))
        val writer = ComplexObject::class.objectRelationMapper(calculationMap).objectRelationalWriter
        val keys = listOf(DefaultPreWriteEntry<ComplexObject>("id", true) { id })
        val others = listOf(DefaultPreWriteEntry<ComplexObject>("comment", false) { comment })
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
        println("got: $written")
//        assertEquals(writerData, writer.list.map { it as ObjectRelationalWriterMap })
    }

    @Test
    fun testRead() {

    }
}