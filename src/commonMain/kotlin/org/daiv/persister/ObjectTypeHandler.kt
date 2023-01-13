package org.daiv.persister

import kotlin.reflect.KClass

interface HeaderBuilder<T> : InsertHeadable, Headerable where T : Headerable, T : InsertHeadable {
    val nativeTypes: List<T>
    override fun insertHead(): Row {
        return nativeTypes.fold(Row()) { r1, r2 -> r1 + r2.insertHead() }
    }

    override fun toHeader(): Row {
        return nativeTypes.fold(Row()) { r1, r2 -> r1 + r2.toHeader() }
    }
}

interface ListHandler<T : Any> : ValueInserter<T>, ReadFromDB, HeaderBuilder<TypeHandler<T, *>> {
    override val nativeTypes: List<TypeHandler<T, *>>

    override fun insertValue(t: T?): Row {
        return nativeTypes.fold(Row()) { r1, r2 -> r1 + r2.toInsert(t) }
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
    override val nativeTypes: List<TypeHandler<T, *>>
) : ValueInserter<T>, InsertHeadable, Headerable, ListHandler<T>

data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    val clazz: KClass<T>,
    val moreKeys: MoreKeysData,
    val _nativeTypes: List<TypeHandler<T, *>>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T>, Nameable, ListHandler<T> {

    override val nativeTypes = _nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    override val numberOfColumns: Int = nativeTypes.sumOf { it.numberOfColumns }

    override fun toValue(list: List<Any?>, tableCollector: TableCollector): T? {
        return tableCollector.getTableReader(clazz)?.readFromTable(list)
    }

    override fun mapName(name: String): ObjectTypeRefHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    override fun toInsert(any: HIGHER?): Row {
        if (any == null) {
            return Row("null")
        }
        return insertValue(getValue.get(any))
    }
}
