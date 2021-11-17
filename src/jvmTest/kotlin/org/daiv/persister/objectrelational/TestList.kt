package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.daiv.persister.table.runTest
import org.daiv.time.isoTime
import org.junit.Test
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.test.assertEquals

class TestList {

    data class SimpleList(val x: Int, val list: List<Int>)

    private val calculationMap = CalculationMap()

    @Test
    fun testParameter() {
        val x = ClassHeaderData.toParameters(SimpleList::class)
        assertEquals(
            ClassHeaderData(
                SimpleList::class,
                listOf(
                    SimpleParameter("x", Int::class.starProjectedType, {}),
                    ParameterWithOneGeneric(
                        "list",
                        List::class.createType(listOf(KTypeProjection.invariant(Int::class.starProjectedType))),
                        Int::class.starProjectedType,
                        {}
                    )
                )
            ), x
        )
        val p = x.parameters[1]
        val s = SimpleList(5, listOf(2))
        assertEquals(listOf(2), p.propGetter(s))
    }

//    @Test
//    fun testKeysList() = runTest {
//        val keys = calculationMap.createKeys(ClassParameterImpl(SimpleList::class))
//        assertEquals(
//            emptyMap(),
//            keys.map { it.key to it.value.classParameter.clazz }.toMap()
//        )
//    }

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

//    @Test
//    fun testGetType() {
//        val list = listOf(5, 6)
//        SimpleList::class.primaryConstructor!!.parameters.forEach {
//            if (it.type.typeName()?.isCollection())
//                println("${it.type.arguments[0].type}")
//        }
//    }
}
