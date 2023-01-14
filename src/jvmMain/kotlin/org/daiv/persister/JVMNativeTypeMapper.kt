package org.daiv.persister

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor


fun <T : Any> KClass<T>.memberInConstructorOrder(): List<KProperty1<T, *>> {
    return primaryConstructor!!.parameters.map { p -> declaredMemberProperties.find { it.name == p.name }!! }
}

data class DefaultValueGetter<HIGHERCLASS : Any, LOWERTYPE : Any>(
    val member: KProperty1<HIGHERCLASS, LOWERTYPE?>,
    val primaryConstructor: KFunction<HIGHERCLASS>
) : MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override val moreKeys: MoreKeysData
        get() = member.findAnnotation<MoreKeys>().default().toMoreKeysData()

    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return member.get(higher)
    }

    override fun createValueArgs(vararg args: Any?): HIGHERCLASS {
        return primaryConstructor.call(args)
    }

    override fun getLowerMembers(): List<MemberValueGetter<LOWERTYPE, *>> {
        val clazz = member.returnType.classifier as KClass<LOWERTYPE>
        val x: List<DefaultValueGetter<LOWERTYPE, Any>> = clazz.memberInConstructorOrder().map {
            DefaultValueGetter(
                it,
                clazz.primaryConstructor
                    ?: throw RuntimeException("no primary constructor for class ${clazz.qualifiedName}")
            )
        }
        return x
    }

    override val holderClass: KClass<HIGHERCLASS>
        get() = member.returnType.classifier as KClass<HIGHERCLASS>
    override val memberClass: KClass<LOWERTYPE>
        get() = member.returnType.classifier as KClass<LOWERTYPE>
    override val name: String
        get() = member.name
    override val isMarkedNullable: Boolean
        get() = member.returnType.isMarkedNullable
}


