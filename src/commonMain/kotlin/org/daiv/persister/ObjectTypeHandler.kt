package org.daiv.persister

interface ListHandler<T : Any> : InsertHeadable, ValueInserter<T>, Headerable, ReadFromDB {
    val nativeTypes: List<TypeHandler<T, *, *>>

    override fun insertHead(): String {
        return nativeTypes.joinToString(", ") { it.insertHead() }
    }
    override fun toHeader(): String {
        return nativeTypes.joinToString(", ") { it.toHeader() }
    }

    override fun insertValue(t: T?): String {
        return nativeTypes.joinToString(", ") { it.toInsert(t) }
    }

    private fun recursiveRead(databaseRunner: DatabaseRunner, i: Int): DatabaseRunner {
        if (i < nativeTypes.size) {
            val n = nativeTypes[i]
            val next = n.getValue(databaseRunner)
            return recursiveRead(next, i + 1)
        }
        return databaseRunner
    }

    override fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        return recursiveRead(databaseRunner, 0)
    }
}

data class ObjectTypeHandler<T : Any>(
    override val nativeTypes: List<TypeHandler<T, *, *>>
) : ValueInserter<T>, InsertHeadable, Headerable, ListHandler<T>

data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val _nativeTypes: List<TypeHandler<T, *, *>>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T, ObjectTypeRefHandler<HIGHER, T>>, Nameable, ListHandler<T> {

    override val nativeTypes = _nativeTypes.take(moreKeys.amount).map { it.map(name) }

    override fun mapName(name: String): ObjectTypeRefHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    override fun toInsert(any: HIGHER?): String {
        if (any == null) {
            return "null"
        }
        return insertValue(getValue.get(any))
    }
}
