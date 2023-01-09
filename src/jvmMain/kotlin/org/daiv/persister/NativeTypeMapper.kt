package org.daiv.persister

import kotlin.reflect.KProperty1

data class DefaultValueGetter<HIGHERCLASS : Any, LOWERTYPE>(val member: KProperty1<HIGHERCLASS, LOWERTYPE>): GetValue<HIGHERCLASS, LOWERTYPE> {
    override fun get(higherClass: HIGHERCLASS): LOWERTYPE {
        return member.get(higherClass)
    }
}

class NativeTypeMapperCreator<HIGHERCLASS : Any, LOWERTYPE>(val member: KProperty1<HIGHERCLASS, LOWERTYPE>) {

    private val type: NativeType = when (member.returnType.classifier) {
        Int::class -> NativeType.INT
        Long::class -> NativeType.LONG
        String::class -> NativeType.STRING
        Double::class -> NativeType.DOUBLE
        Boolean::class -> NativeType.BOOLEAN
        else -> throw RuntimeException("unknown type: ${member.returnType}")
    }

    val getValue = DecoratorFactory.getDecorator(type, DefaultValueGetter(member))

    fun toNativeTypeHandler() = NativeTypeHandler(type, member.name, member.returnType.isMarkedNullable, getValue)
}