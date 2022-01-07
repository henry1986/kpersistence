package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeysData
import org.daiv.persister.runTest
import org.junit.Test
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.reflect.full.starProjectedType
import kotlin.test.assertEquals

class TestList {

    private data class ReverseA(val b: Int, val rb: ReverseB)
    private data class ReverseB(val a: Int, val rA: ReverseA)

    private data class SimpleObject(val x: Int, val y: String)
    private data class ComplexObject(val id: SimpleObject, val comment: String, val s: SimpleObject)
    private data class Complex2Object(val id: Int, val comment: String, val c: ComplexObject)

    private data class SimpleList(val x: Int, val list: List<Int>)

    private val calculationMap = CormMap()
    val codeGenHelper = CodeGenHelper(TestList::class, mapOf())
    private val chdMap = codeGenHelper.map

    @Test
    fun testComplex() = runTest {
        val clz = Complex2Object::class
        val chd = chdMap.getAndJoin(clz)
        val head = chd.head(null, false, emptyList())
        println("head: $head")
    }

    @Test
    fun testReverse() = runTest {
        val reverseA = ReverseA::class
        val header = chdMap.getAndJoin(reverseA)
        val head = header.head(null, false, emptyList())
        println("head: $head")

    }

    @Test
    fun testParameter() {
        val x = JClassHeaderData.toParameters(SimpleList::class, chdMap)
        assertEquals(
            ClassHeaderData(
                SimpleList::class,
                listOf(
                    SimpleParameter(SimpleList::class, "x", Int::class.starProjectedType,KeyType.NORM, chdMap),
                    ParameterWithOneGeneric(
                        SimpleList::class,
                        "list",
                        List::class.createType(listOf(KTypeProjection.invariant(Int::class.starProjectedType))),
                        KeyType.NO_KEY,
                        chdMap,
                        Int::class.starProjectedType
                    )
                ),
                MoreKeysData(1)
            ), x
        )
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
        val codeGenHelper = CodeGenHelper(SimpleList::class, mapOf())
        val head:List<HeadEntry> = listOf(codeGenHelper.listHead<Int>("x"))
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
