package org.daiv.persister

import kotlin.reflect.KClassifier


fun interface GetValue<HIGHER : Any, LOWER> {
    fun get(higher: HIGHER): LOWER?
}


interface Member {
    val clazz: KClassifier?
    val moreKeys: MoreKeysData
    val name: String
    val isMarkedNullable: Boolean

    fun getType(): NativeType? {
        return when (clazz) {
            Int::class -> NativeType.INT
            Long::class -> NativeType.LONG
            String::class -> NativeType.STRING
            Double::class -> NativeType.DOUBLE
            Boolean::class -> NativeType.BOOLEAN
            else -> null
        }
    }

    fun isNative() = getType() != null
}

interface MemberValueGetter<HIGHERCLASS : Any, LOWERTYPE : Any> : Member, GetValue<HIGHERCLASS, LOWERTYPE> {
    fun getLowerMembers(): List<MemberValueGetter<LOWERTYPE, *>>

    fun create(): TypeHandler<HIGHERCLASS, LOWERTYPE, *> {
        return if (isNative())
            NativeTypeMapperCreator.create(this)
        else
            ObjectTypeMapperCreator.create(this)
    }
}


inline fun <HIGHERCLASS : Any, reified LOWERTYPE : Any> memberValueGetter(
    name: String,
    isMarkedNullable: Boolean,
    moreKeys: MoreKeysData = MoreKeysData(1, false),
    members: List<MemberValueGetter<LOWERTYPE, *>> = emptyList(),
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = memberValueGetterCreator(name, isMarkedNullable, moreKeys, members, func).create()

inline fun <HIGHERCLASS : Any, reified LOWERTYPE : Any> memberValueGetterCreator(
    name: String,
    isMarkedNullable: Boolean,
    moreKeys: MoreKeysData = MoreKeysData(1, false),
    members: List<MemberValueGetter<LOWERTYPE, *>> = emptyList(),
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = DefaultMemberValueGetter(LOWERTYPE::class, name, isMarkedNullable, moreKeys, members, func)

class DefaultMemberValueGetter<HIGHERCLASS : Any, LOWERTYPE : Any>(
    override val clazz: KClassifier?,
    override val name: String,
    override val isMarkedNullable: Boolean,
    override val moreKeys: MoreKeysData,
    val members: List<MemberValueGetter<LOWERTYPE, out Any>> = emptyList(),
    val func: HIGHERCLASS.() -> LOWERTYPE
) : MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return higher.func()
    }

    override fun getLowerMembers(): List<MemberValueGetter<LOWERTYPE, *>> {
        return members
    }
}

interface TypeHandlerFactory {
    fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): TypeHandler<HIGHERCLASS, LOWERTYPE, *>
}

object ObjectTypeMapperCreator : TypeHandlerFactory {

    override fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): ObjectTypeRefHandler<HIGHERCLASS, LOWERTYPE> {
        val got: List<TypeHandler<LOWERTYPE, *, *>> = member.getLowerMembers().map { it.create() }
        return ObjectTypeRefHandler(member.name, member.isMarkedNullable, member.moreKeys, got, member)
    }
}

object NativeTypeMapperCreator : TypeHandlerFactory {
    override fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): NativeTypeHandler<HIGHERCLASS, LOWERTYPE> {
        val type: NativeType = member.getType() ?: throw RuntimeException("unknown type: ${member.clazz}")

        return NativeTypeHandler(type, member.name, member.isMarkedNullable, member)
    }
}
