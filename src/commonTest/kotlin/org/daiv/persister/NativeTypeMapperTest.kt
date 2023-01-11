package org.daiv.persister

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeMapperTest<T : Any> {
    class IntHolder(val i: Int)
    class LongHolder(val i: Long)
    class StringHolder(val i: String)

    inline fun <reified HOLDER : Any, reified T> testRun(
        name: String,
        nativeType: NativeType,
        mapperClass: KClass<out MapValue<*>>,
        noinline func: HOLDER.() -> T
    ) {
        val got = memberValueGetterCreator(name, false, func)
        assertEquals(
            got.toNativeTypeHandler(),
            NativeTypeHandler(nativeType, name, false, got.mapValue, got.member)
        )
        assertEquals(mapperClass, got.mapValue::class)
    }

    @Test
    fun test() {
        testRun<IntHolder, Int>("i", NativeType.INT, DefaultValueMapper::class) { i }
        testRun<LongHolder, Long>("i", NativeType.LONG, LongValueGetterDecorator::class) { i }
        testRun<StringHolder, String>("i", NativeType.STRING, StringValueGetterDecorator::class) { i }
    }
}
