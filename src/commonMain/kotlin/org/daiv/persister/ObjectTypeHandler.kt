package org.daiv.persister

data class ObjectTypeHandler<HIGHER : Any, T : Any>(
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<T, *, *>>
) : ValueInserterMapper<HIGHER, T>, NullableElement, InsertHeadable, Headerable {

    override fun insertHead(): String {
        return nativeTypes.joinToString(", ") { it.insertHead() }
    }

    override fun toHeader(): String {
        return nativeTypes.joinToString(", ") { it.toHeader() }
    }

    override fun insertValue(t: T?): String {
        return nativeTypes.joinToString(", ") { it.toInsert(t) }
    }

    override fun toInsert(any: HIGHER?): String {
        TODO("Not yet implemented")
    }
}

data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<TypeHandler<T, *, *>>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T, ObjectTypeRefHandler<HIGHER, T>>, Nameable {

    private val keys = nativeTypes.take(moreKeys.amount).map { it.map(name) }

    override fun insertHead(): String {
        return keys.joinToString(", ") { it.insertHead() }
    }

    override fun toHeader(): String {
        return keys.joinToString(", ") { it.toHeader() }
    }

    override fun mapName(name: String): ObjectTypeRefHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    override fun insertValue(t: T?): String {
        return keys.joinToString(", ") { it.toInsert(t) }
    }

    override fun toInsert(any: HIGHER?): String {
        if(any == null){
            return "null"
        }
        return insertValue(getValue.get(any))
    }
}
