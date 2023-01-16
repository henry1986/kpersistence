package org.daiv.persister

import kotlin.reflect.KClass


fun interface GetValue<HIGHER : Any, LOWER> {
    fun get(higher: HIGHER): LOWER?
}

fun interface ValueFactory<HOLDER> {
    fun createValueArgs(vararg args: Any?): HOLDER
    fun createValue(list: List<Any?>): HOLDER {
        return createValueArgs(*list.toTypedArray())
    }
}

interface FlatClass<MEMBER : Any> {
    val memberClass: KClass<MEMBER>
    val moreKeys: MoreKeysData
    val members: List<MemberValueGetter<MEMBER, out Any>>
    fun getType(): NativeType? {
        return when (memberClass) {
            Int::class -> NativeType.INT
            Long::class -> NativeType.LONG
            String::class -> NativeType.STRING
            Double::class -> NativeType.DOUBLE
            Boolean::class -> NativeType.BOOLEAN
            Map::class -> NativeType.MAP
            List::class -> NativeType.LIST
            Set::class -> NativeType.SET
            else -> null
        }
    }

    fun createObjectType(valueFactory: ValueFactory<MEMBER>) =
        ObjectTypeHandler(members.map { it.create() }, moreKeys, valueFactory)
}

interface Member<HOLDER : Any, MEMBER : Any> : ValueFactory<HOLDER>, FlatClass<MEMBER> {
    val holderClass: KClass<HOLDER>
    val name: String
    val isMarkedNullable: Boolean
}

interface MemberValueGetter<HOLDERCLASS : Any, LOWERTYPE : Any> : Member<HOLDERCLASS, LOWERTYPE>,
    GetValue<HOLDERCLASS, LOWERTYPE> {

    fun create(): TypeHandler<HOLDERCLASS, LOWERTYPE> {
        val type = getType()
        return when {
            type == null -> ObjectTypeMapperCreator.create(this)
            type.isNative -> NativeTypeMapperCreator.create(this)
            else -> CollectionTypeHandlerRef(name, isMarkedNullable, holderClass)
        }
    }
}

inline fun <reified HOLDER : Any, reified MEMBERTYPE : Any> memberValueGetter(
    name: String,
    isMarkedNullable: Boolean,
    moreKeys: MoreKeysData = MoreKeysData(1, false),
    members: List<MemberValueGetter<MEMBERTYPE, *>> = emptyList(),
    valueFactory: ValueFactory<HOLDER>,
    noinline func: HOLDER.() -> MEMBERTYPE
) = memberValueGetterCreator(name, isMarkedNullable, moreKeys, members, valueFactory, func).create()

inline fun <reified HOLDER : Any, reified MEMBERTYPE : Any> memberValueGetterCreator(
    name: String,
    isMarkedNullable: Boolean,
    moreKeys: MoreKeysData = MoreKeysData(1, false),
    members: List<MemberValueGetter<MEMBERTYPE, *>> = emptyList(),
    valueFactory: ValueFactory<HOLDER>,
    noinline func: HOLDER.() -> MEMBERTYPE
) = DefaultMemberValueGetter(
    HOLDER::class,
    MEMBERTYPE::class,
    name,
    isMarkedNullable,
    moreKeys,
    members,
    valueFactory,
    func
)

class DefaultFlatClass<MEMBER : Any>(
    override val memberClass: KClass<MEMBER>,
    override val moreKeys: MoreKeysData,
    override val members: List<MemberValueGetter<MEMBER, *>> = emptyList()
) : FlatClass<MEMBER>

class DefaultMemberValueGetter<HOLDER : Any, MEMBER : Any>(
    override val holderClass: KClass<HOLDER>,
    override val memberClass: KClass<MEMBER>,
    override val name: String,
    override val isMarkedNullable: Boolean,
    override val moreKeys: MoreKeysData,
    override val members: List<MemberValueGetter<MEMBER, out Any>> = emptyList(),
    val valueFactory: ValueFactory<HOLDER>,
    val func: HOLDER.() -> MEMBER
) : MemberValueGetter<HOLDER, MEMBER>, ValueFactory<HOLDER> by valueFactory {
    override fun get(higher: HOLDER): MEMBER? {
        return higher.func()
    }
}

interface TypeHandlerFactory {
    fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): TypeHandler<HIGHERCLASS, LOWERTYPE>
}

object ObjectTypeMapperCreator : TypeHandlerFactory {

    override fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): ObjectTypeRefHandler<HIGHERCLASS, LOWERTYPE> {
        return ObjectTypeRefHandler(
            member.name,
            member.isMarkedNullable,
            member.memberClass,
            member.moreKeys,
            member.members.map { it.create() },
            member
        )
    }
}

object NativeTypeMapperCreator : TypeHandlerFactory {
    override fun <HIGHERCLASS : Any, LOWERTYPE : Any> create(member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>): NativeTypeHandler<HIGHERCLASS, LOWERTYPE> {
        val type: NativeType = member.getType() ?: throw RuntimeException("unknown type: ${member.memberClass}")
        return NativeTypeHandler(type, member.name, member.isMarkedNullable, member)
    }
}
