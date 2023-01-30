package org.daiv.persister

import org.daiv.persister.ObjectTypeHandlerTest.MyObject
import org.daiv.persister.TestComplexObjectType.ComplexObject
import org.daiv.persister.sql.command.HeaderValuePair
import org.junit.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JvmNativeTypeMapperTest<T : Any> {
    data class IntHolder(val i: Int)
    data class ComplexHolder(val ih: IntHolder, val i: Int)

    val intHolderGetter = DefaultValueGetter(
        IntHolder::class.declaredMemberProperties.first() as KProperty1<IntHolder, Int?>,
        IntHolder::class.primaryConstructor!!
    )

    @Test
    fun test() {
        val getter = intHolderGetter
        assertEquals(Int::class, getter.memberClass)
        assertEquals("i", getter.name)
        assertEquals(5, getter.get(IntHolder(5)))
        assertFalse(getter.isMarkedNullable)
    }

    @Test
    fun testComplex() {
        val member: KProperty1<ComplexHolder, IntHolder> =
            ComplexHolder::class.memberInConstructorOrder().first() as KProperty1<ComplexHolder, IntHolder>
        val getter = DefaultValueGetter(member, ComplexHolder::class.primaryConstructor!!)
        assertEquals("ih", getter.name)
        assertEquals(IntHolder::class, member.returnType.classifier)
        assertEquals(IntHolder::class, getter.memberClass)
        assertEquals(IntHolder(5), getter.get(ComplexHolder(IntHolder(5), 6)))
        assertEquals<List<MemberValueGetter<*, *>>>(listOf(intHolderGetter), getter.members)
        assertFalse(getter.isMarkedNullable)
    }

    @Test
    fun testFindAnno() {

        val c = MyObject::class
        val m = c.findAnnotation<MoreKeys>()
        assertEquals(2, m?.amount)
    }
}
