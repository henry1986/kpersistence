package org.daiv.persister

import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertEquals

class NativeTypeMapperTest<T:Any> {
    class IntHolder(val i: Int)
    class LongHolder(val i: Long)
    class StringHolder(val i: String)

    fun<T:Any> runTest(nativeType:NativeType, clazz: KClass<*>, expectedGetValue: KClass<out GetValue<*,*>>){
        val member: KProperty1<Any, *> =
            clazz.declaredMemberProperties.first() as KProperty1<Any, *>

        val nativeTypeMapper = NativeTypeMapperCreator(member)
        assertEquals(
            nativeTypeMapper.toNativeTypeHandler(),
            NativeTypeHandler(nativeType, "i", false, nativeTypeMapper.getValue)
        )
        assertEquals(expectedGetValue, nativeTypeMapper.getValue::class)
    }

    @Test
    fun test() {
        runTest<Int>(NativeType.INT, IntHolder::class, DefaultValueGetter::class)
        runTest<Long>(NativeType.LONG, LongHolder::class, LongValueGetterDecorator::class)
        runTest<String>(NativeType.STRING, StringHolder::class, StringValueGetterDecorator::class)
    }

//    @Test
//    fun testFindAnno(){
//        @MoreKeys(2)
//        class MyObject(val i: Int, val s: String, val x: Long)
//        val c = MyObject::class
//        val m = c.findAnnotation<MoreKeys>()
//        assertEquals(2, m?.amount)
//    }
}