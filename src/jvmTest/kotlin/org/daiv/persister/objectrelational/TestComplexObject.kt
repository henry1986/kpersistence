package org.daiv.persister.objectrelational

import org.daiv.persister.objectrelational.ReflectionObjectRelationalMapperTest.*
import org.junit.Test
import kotlin.test.assertEquals

class TestComplexObject {
    data class ComplexObject(val id: Int, val comment: String, val s: SimpleObject)

    private val simpleMapper = SimpleObject::class.objectRelationMapper()

    @Test
    fun testHeader() {
        val header = ComplexObject::class.objectRelationMapper().objectRelationalHeader
        val headerData = ObjectRelationalHeaderData(
            listOf(HeadEntry("id", "Int", true)),
            listOf(HeadEntry("comment", "String", false), HeadEntry("s_x", "Int", false)),
            listOf(simpleMapper.objectRelationalHeader)
        )
        assertEquals(headerData, header)
    }

    @Test
    fun testInsert() {
        val writer = ComplexObject::class.objectRelationMapper().objectRelationalWriter
        val writerData = ObjectRelationalWriterData<ComplexObject>(
            listOf(DefaultPreWriteEntry("id", true) { id }),
            listOf(DefaultPreWriteEntry("comment", false) { comment }),
            listOf(ObjectRelationalWriterMap(simpleMapper.objectRelationalWriter) { s })
        )
        assertEquals(writerData, writer)
    }

    @Test
    fun testRead(){

    }
}