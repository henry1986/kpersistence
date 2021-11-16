package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.daiv.persister.table.runTest
import org.daiv.time.isoTime
import org.junit.Test
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals

class TestList {

    data class SimpleList(val x: Int, val list: List<Int>)

    private val calculationMap = CalculationMap()

    @Test
    fun testKeysList() = runTest {
        val keys = calculationMap.createKeys(ClassParameterImpl(SimpleList::class))
        assertEquals(
            emptyMap(),
            keys.map { it.key to it.value.classParameter.clazz }.toMap()
        )
    }

    @Test
    fun testHead() = runTest {
        val header = SimpleList::class.objectRelationMapper(calculationMap).objectRelationalHeader
        val head = listOf(HeadEntry("x", "Int", true))
        val headerData = ObjectRelationalHeaderData(
            head,
            emptyList(),
            listOf()
        )
        assertEquals(headerData, header)
//        assertEquals(head.take(1), mapper.keyHead(null))
    }

    @Test
    fun testGetType() {
        val list = listOf(5, 6)
        SimpleList::class.primaryConstructor!!.parameters.forEach {
            if (it.type.typeName()?.isCollection())
                println("${it.type.arguments[0].type}")
        }
    }
}
