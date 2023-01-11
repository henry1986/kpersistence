package org.daiv.persister

import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JvmNativeTypeMapperTest<T : Any> {
    class IntHolder(val i: Int)

    @Test
    fun test() {
        val member: KProperty1<Any, *> =
            IntHolder::class.declaredMemberProperties.first() as KProperty1<Any, *>
        val getter = DefaultValueGetter(member)
        assertEquals(Int::class,getter.clazz)
        assertEquals("i",getter.name)
        assertEquals(5,getter.get(IntHolder(5)))
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