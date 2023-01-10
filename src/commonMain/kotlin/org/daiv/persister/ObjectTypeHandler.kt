package org.daiv.persister

data class ObjectTypeHandler(
    override val typeName: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<*>>
): TypeNameable, NullableElement, InsertHeadable, Headerable {

    override fun insertHead(): String {
        return nativeTypes.map { it.insertHead() }.joinToString(", ")
    }

    override fun toHeader(): String {
        return nativeTypes.joinToString(", ") { it.toHeader() }
    }
}

data class ObjectTypeRefHandler(
    override val typeName: String,
    override val name: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<*>>
) :TypeNameable, Nameable, Headerable, NullableElement, InsertHeadable{

    private val keys = nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    override fun insertHead(): String {
        return keys.map { it.insertHead() }.joinToString(", ")
    }

    override fun toHeader(): String {
        return keys.joinToString(", ") { it.toHeader() }
    }
}
