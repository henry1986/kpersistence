package org.daiv.persister.objectrelational

import org.daiv.persister.runTest
import org.junit.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class ParameterHeadEntryTest {
    private data class TestClass(val x: Int)
    private data class TestComplexClass(val x: Int, val t: TestClass)

    private val cormMap = CormMap()

    @Test
    fun test() = runTest {
        val vale = cormMap.getValue(TestComplexClass::class)
        val heads = vale.objectRelationalHeader.allHeads(null, null)
        val cgh = CodeGenHelper(TestComplexClass::class, mapOf())
        assertEquals(
            listOf(
                cgh.simpleKeyHead<Int>("x"),
                HeadEntry(
                    listOf(
                        cgh.simple<TestClass>("t"),
                        SimpleParameter(TestClass::class, "x", typeOf<Int>(), KeyType.NORM, cormMap.chdMap)
                    ), "t_x", "Int", false
                ),
            ).toList(), heads.toList()
        )
    }
}
