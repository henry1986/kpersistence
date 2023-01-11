package org.daiv.persister

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties


class DefaultValueGetter<HIGHERCLASS : Any, LOWERTYPE:Any>(val member: KProperty1<HIGHERCLASS, LOWERTYPE?>) :
    MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return member.get(higher)
    }

    override fun getLowerMembers(): List<MemberValueGetter<*, *>> {
        val clazz = member.returnType.classifier as KClass<*>
        return clazz.declaredMemberProperties.map { DefaultValueGetter(it) }
    }

    override val clazz: KClassifier?
        get() = member.returnType.classifier
    override val name: String
        get() = member.name
    override val isMarkedNullable: Boolean
        get() = member.returnType.isMarkedNullable
}


