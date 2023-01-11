package org.daiv.persister

import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1


class DefaultValueGetter<HIGHERCLASS : Any, LOWERTYPE>(val member: KProperty1<HIGHERCLASS, LOWERTYPE>) :
    MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return member.get(higher)
    }

    override val clazz: KClassifier?
        get() = member.returnType.classifier
    override val name: String
        get() = member.name
    override val isMarkedNullable: Boolean
        get() = member.returnType.isMarkedNullable
}


