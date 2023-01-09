package org.daiv.persister

import org.junit.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals

class NativeTypeMapperTest {

    class IntHolder(val i: Int)

    @Test
    fun test() {

        val intClass = IntHolder::class
        val member: KProperty1<IntHolder, Int> = intClass.declaredMemberProperties.first() as KProperty1<IntHolder, Int>
//        val nativeTypeMapper = NativeTypeMapper(member)
//        assertEquals(
//            nativeTypeMapper.toNativeTypeHandler(),
//            NativeTypeHandler(NativeType.INT, "i", false, nativeTypeMapper)
//        )

    }
}