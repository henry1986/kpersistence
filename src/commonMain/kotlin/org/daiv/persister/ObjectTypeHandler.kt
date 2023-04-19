package org.daiv.persister

import org.daiv.persister.sql.command.Column
import org.daiv.persister.sql.command.SelectKey
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

interface MainObjectHandler<T : Any> : ReadFromDB, ValueInserterBuilder<T>, InsertHeadableList, HeaderableList {
    override val nativeTypes: List<TypeHandler<T, *>>


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

/**
 * Represents a handler for a reference to an object type `T` that is used as a property in a higher-level object type
 * `HIGHER`. Provides methods for converting data between the database and the object types.
 *
 * @param HIGHER the type of the higher-level object that contains this property
 * @param T the type of the object that this property represents
 * @property name the name of the property
 * @property isNullable `true` if the property can be `null`, `false` otherwise
 * @property clazz the [KClass] object representing the object type `T`
 * @property moreKeys additional metadata about the property
 * @property _nativeTypes a list of type handlers for the native types that correspond to the columns in the database table
 * @property getValue a function that retrieves the value of the property from a higher-level object
 * @property nonMappedName the name of the property in the database table (optional; defaults to the value of `name`)
 */
data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    override val clazz: KClass<T>,
    val moreKeys: MoreKeysData,
    val _nativeTypes: List<TypeHandler<T, *>>,
    val getValue: GetValue<HIGHER, T>,
    override val nonMappedName: String = name,
) : TypeHandler<HIGHER, T>, Nameable, MainObjectHandler<T>, ToValueable<T>, Classable<T> {

    /**
     * A list of type handlers for the native types that correspond to the columns in the database table.
     * Includes only the first [MoreKeysData.amount] handlers from [_nativeTypes].
     */
    override val nativeTypes = _nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    /**
     * Converts a [ColumnValues] object that contains the column values of a row in the database table into an object
     * of type `T`, using the [TableReader] for `T` that is registered in the [TableCollector].
     *
     * @param columnValues the [ColumnValues] object containing the column values of a row in the database table
     * @param tableCollector the [TableCollector] that contains the [TableReader] for `T`
     * @return the object of type `T` that corresponds to the row in the database table, or `null` if the row is empty
     */
    override fun toValue(columnValues: ColumnValues, tableCollector: TableCollector): T? {
        return tableCollector.getTableReader(clazz)?.readFromTable(columnValues.lowerValues)
    }

    /**
     * The total number of columns in the database table that correspond to this property.
     */
    override val numberOfColumns: Int = nativeTypes.sumOf { it.numberOfColumns }

    /**
     * Returns a copy of this object with a modified name.
     *
     * @param name the new name of the property
     * @return a copy of this object with the modified name
     */
    override fun mapName(name: String): ObjectTypeRefHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    /**
     * Generates a list of [Column]s for the [SelectKey]s provided in [list].
     *
     * @throws [RuntimeException] if [list] contains multiple [SelectKey]s or if a [SelectKey] contains more than one key.
     *
     * @return A list of [Column]s generated from the [SelectKey] in [list].
     */
    override fun select(list: List<SelectKey>): List<Column> {
        if (list.size == 1) {
            if (list.first().keys.size == 1) {
                val v = list.first().value
                val insert = insertValue(v as T?)
                val head = insertHead()
                return head.list.mapIndexed { i, it -> Column(it, insert.list[i]) }
            } else {
                throw RuntimeException("list doesn't make sense: $list")
            }
        } else {
            throw RuntimeException()
        }
    }

    /**
     * Converts the given object into a [Row] object that can be inserted into a database table.
     *
     * @param any The object to insert into the database table.
     * @return A [Row] object representing the object to be inserted.
     */
    override fun toInsert(any: HIGHER?): Row {
        if (any == null) {
            return Row("null")
        }
        return insertValue(getValue.get(any))
    }
}
