package org.daiv.persister

import kotlin.reflect.KClass


fun <T> List<T>.foldList(func: T.() -> Row): Row {
    return fold(Row()) { r1, r2 -> r1 + r2.func() }
}

interface InsertHeadableList : InsertHeadable {
    val nativeTypes: List<InsertHeadable>
    override fun insertHead(): Row {
        return nativeTypes.foldList { insertHead() }
    }
}

interface HeaderableList : Headerable {
    val nativeTypes: List<Headerable>

    override fun toHeader(): Row {
        return nativeTypes.foldList { toHeader() }
    }
}

interface ValueInserterBuilder<T : Any> : ValueInserter<T> {
    val nativeTypes: List<ValueInserterMapper<in T>>
    override fun insertValue(t: T?): Row {
        return nativeTypes.foldList { toInsert(t) }
    }
}

interface MainObjectHandler<T : Any> : ReadFromDB, ValueInserterBuilder<T>, InsertHeadableList, HeaderableList,
    SelectMapper {
    override val nativeTypes: List<TypeHandler<T, *>>

    private fun recMap(i: Int, list: List<Any?>, row: Row): Row {
        if (i < nativeTypes.size) {
            val got = nativeTypes[i]
            val keys = list.take(got.numberOfColumns)
            val next = got.select(keys)
            return recMap(i + 1, list.drop(got.numberOfColumns), row + next)
        }
        return row
    }

    override fun select(list: List<Any?>): Row {
        return recMap(0, list, Row())
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

inline fun <reified T : Any> objectType(
    nativeTypes: List<TypeHandler<T, *>>,
    moreKeys: MoreKeysData,
    valueFactory: ValueFactory<T>
) = ObjectTypeHandler(nativeTypes, moreKeys, valueFactory)

interface Classable<T : Any> {
    val clazz: KClass<T>
}

data class ObjectTypeHandler<T : Any>(
    override val nativeTypes: List<TypeHandler<T, *>>,
    val moreKeys: MoreKeysData,
    val valueFactory: ValueFactory<T>,
) : ValueInserter<T>, InsertHeadableList, HeaderableList, MainObjectHandler<T>, ValueFactory<T> by valueFactory {

    private val keys = nativeTypes.take(moreKeys.amount)
    private val keyNumberOfColumns = keys.sumOf { it.numberOfColumns }

    fun keyNames(): Row = keys.fold(Row()) { r1, r2 -> r1 + r2.insertHead() }

    fun receiveValuesForConstructor(
        row: DRow,
        tableCollector: TableCollector,
        keyColumnValues: List<Any?>,
        i: Int,
        ret: List<Any?>
    ): List<Any?> {
        if (i < nativeTypes.size) {
            val got = nativeTypes[i]
            val value = got.toValue(ColumnValues(keyColumnValues, row.take(got.numberOfColumns)), tableCollector)
            return receiveValuesForConstructor(
                row - got.numberOfColumns,
                tableCollector,
                keyColumnValues,
                i + 1,
                ret + value
            )
        }
        return ret
    }

    /**
     * select * from X where i = 5 AND s = "Hello";
     * [5], ["Hello"], [9L]
     */
    fun toValue(row: DRow, tableCollector: TableCollector): T {
        val list = receiveValuesForConstructor(row, tableCollector, row.list.take(keyNumberOfColumns), 0, emptyList())
        return createValue(list)
    }
}

data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    override val clazz: KClass<T>,
    val moreKeys: MoreKeysData,
    val _nativeTypes: List<TypeHandler<T, *>>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T>, Nameable, MainObjectHandler<T>, ToValueable<T>, Classable<T> {

    override val nativeTypes = _nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    override fun toValue(columnValues: ColumnValues, tableCollector: TableCollector): T? {
        return tableCollector.getTableReader(clazz)?.readFromTable(columnValues.lowerValues)
    }

    override val numberOfColumns: Int = nativeTypes.sumOf { it.numberOfColumns }

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
