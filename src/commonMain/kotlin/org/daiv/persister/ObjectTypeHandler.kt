package org.daiv.persister

data class ObjectTypeHandler(
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<*>>
): NullableElement, InsertHeadable, Headerable {

    override fun insertHead(): String {
        return nativeTypes.map { it.insertHead() }.joinToString(", ")
    }

    override fun toHeader(): String {
        return nativeTypes.joinToString(", ") { it.toHeader() }
    }
}

data class ObjectTypeRefHandler(
    override val name: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<*>>
) : TypeHandler<ObjectTypeRefHandler>, Nameable{

    val keys = nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    override fun insertHead(): String {
        return keys.map { it.insertHead() }.joinToString(", ")
    }

    override fun toHeader(): String {
        return keys.joinToString(", ") { it.toHeader() }
    }

    override fun mapName(name: String): ObjectTypeRefHandler {
        return copy(name = "${name}_${this.name}")
    }
}
