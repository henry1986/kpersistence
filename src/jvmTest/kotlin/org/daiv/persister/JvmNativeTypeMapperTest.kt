package org.daiv.persister

import org.junit.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JvmNativeTypeMapperTest<T : Any> {
    data class IntHolder(val i: Int)
    data class ComplexHolder(val ih: IntHolder, val i: Int)

    val intHolderGetter = DefaultValueGetter(IntHolder::class.declaredMemberProperties.first())

    @Test
    fun test() {
        val getter = intHolderGetter
        assertEquals(Int::class, getter.clazz)
        assertEquals("i", getter.name)
        assertEquals(5, getter.get(IntHolder(5)))
        assertFalse(getter.isMarkedNullable)
    }

    @Test
    fun testComplex() {
        val member: KProperty1<ComplexHolder, *> = ComplexHolder::class.memberInConstructorOrder().first()
        val getter = DefaultValueGetter(member)
        assertEquals("ih", getter.name)
        assertEquals(IntHolder::class, member.returnType.classifier)
        assertEquals(IntHolder::class, getter.clazz)
        assertEquals(IntHolder(5), getter.get(ComplexHolder(IntHolder(5), 6)))
        assertEquals<List<MemberValueGetter<*,*>>>(listOf(intHolderGetter), getter.getLowerMembers())
        assertFalse(getter.isMarkedNullable)
    }

    @Test
    fun testFindAnno() {
        @MoreKeys(2)
        class MyObject(val i: Int, val s: String, val x: Long)

        val c = MyObject::class
        val m = c.findAnnotation<MoreKeys>()
        assertEquals(2, m?.amount)
    }
}
