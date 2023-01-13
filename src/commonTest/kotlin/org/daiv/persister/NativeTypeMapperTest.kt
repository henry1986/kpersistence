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
        noinline creation: (List<Any?>) -> HOLDER,
        noinline func: HOLDER.() -> T
    ) {
        val member = memberValueGetterCreator(name, false, MoreKeysData(), emptyList(), creation = creation, func)
        val n = NativeTypeMapperCreator.create(member)
        assertEquals(
            member.create(),
            NativeTypeHandler(nativeType, name, false, member)
        )
        assertEquals(mapperClass, n.mapValue::class)
    }

    @Test
    fun test() {
        testRun("i", NativeType.INT, DefaultValueMapper::class, { IntHolder(it[0] as Int) }) { i }
        testRun("i", NativeType.LONG, LongValueGetterDecorator::class, { LongHolder(it[0] as Long) }) { i }
        testRun("i", NativeType.STRING, StringValueGetterDecorator::class, { StringHolder(it[0] as String) }) { i }
    }
}
