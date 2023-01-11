package org.daiv.persister

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeTypeMapperTest<T : Any> {
    class IntHolder(val i: Int)
    class LongHolder(val i: Long)
    class StringHolder(val i: String)

    inline fun <reified HOLDER : Any, reified T : Any> testRun(
        name: String,
        nativeType: NativeType,
        mapperClass: KClass<out MapValue<*>>,
        noinline func: HOLDER.() -> T
    ) {
        val member = memberValueGetterCreator(name, false, MoreKeysData(), emptyList(), func)
        val n = NativeTypeMapperCreator.create(member)
        assertEquals(
            member.create(),
            NativeTypeHandler(nativeType, name, false, n.mapValue, member)
        )
        assertEquals(mapperClass, n.mapValue::class)
    }

    @Test
    fun test() {
        testRun<IntHolder, Int>("i", NativeType.INT, DefaultValueMapper::class) { i }
        testRun<LongHolder, Long>("i", NativeType.LONG, LongValueGetterDecorator::class) { i }
        testRun<StringHolder, String>("i", NativeType.STRING, StringValueGetterDecorator::class) { i }
    }
}
