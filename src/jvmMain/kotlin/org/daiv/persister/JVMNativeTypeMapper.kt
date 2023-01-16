package org.daiv.persister

import org.daiv.persister.sql.command.CommandReceiver
import org.daiv.persister.sql.command.DefaultTable
import org.daiv.persister.sql.command.Table
import org.daiv.persister.sql.command.table
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor


fun <T : Any> KClass<T>.memberInConstructorOrder(): List<KProperty1<T, *>> {
    return primaryConstructor!!.parameters.map { p -> declaredMemberProperties.find { it.name == p.name }!! }
}

fun <T : Any> KClass<T>.getPrimary() =
    primaryConstructor ?: throw RuntimeException("no primary constructor for class $qualifiedName")

inline fun <reified T : Any> CommandReceiver.table(tableName: String = T::class.simpleName!!): Table<T> {
    val clazz = T::class
    return table(
        tableName,
        clazz.findAnnotation<MoreKeys>().default().toMoreKeysData(),
        getMembers(clazz)
    ) { clazz.getPrimary().call(it) }
}

fun <LOWERTYPE : Any> getMembers(clazz: KClass<LOWERTYPE>): List<MemberValueGetter<LOWERTYPE, *>> {
    val x: List<DefaultValueGetter<LOWERTYPE, Any>> = clazz.memberInConstructorOrder().map {
        DefaultValueGetter(it, clazz.getPrimary())
    }
    return x
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

    override val members: List<MemberValueGetter<LOWERTYPE, out Any>> =
        getMembers(member.returnType.classifier as KClass<LOWERTYPE>)

    override val holderClass: KClass<HIGHERCLASS>
        get() = member.returnType.classifier as KClass<HIGHERCLASS>
    override val memberClass: KClass<LOWERTYPE>
        get() = member.returnType.classifier as KClass<LOWERTYPE>
    override val name: String
        get() = member.name
    override val isMarkedNullable: Boolean
        get() = member.returnType.isMarkedNullable
}


