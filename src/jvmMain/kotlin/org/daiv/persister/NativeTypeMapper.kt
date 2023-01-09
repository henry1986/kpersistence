package org.daiv.persister

import kotlin.reflect.KProperty1

class NativeTypeMapperCreator<HIGHERCLASS:Any, LOWERTYPE>(val member: KProperty1<HIGHERCLASS, LOWERTYPE>) {

    fun toNativeTypeHandler() = NativeTypeHandler(type, member.name, member.returnType.isMarkedNullable, object : GetValue<HIGHERCLASS, LOWERTYPE>{
        override fun get(higherClass: HIGHERCLASS): LOWERTYPE {
            return member.get(higherClass)
        }
    })

    val type:NativeType = when(member.returnType.classifier){
        Int::class -> NativeType.INT
        Long::class -> NativeType.LONG
        String::class -> NativeType.STRING
        Double::class -> NativeType.DOUBLE
        Boolean::class -> NativeType.BOOLEAN
        else -> throw RuntimeException("unknown type: ${member.returnType}")
    }
}