package org.daiv.persister

import kotlin.reflect.KClass

interface TableCollector {
    fun <T : Any, R : Any> getCollectionTableReader(clazz: KClass<T>, fieldName: String? = null): TableReader<R?>?
    fun <T : Any> getTableReader(clazz: KClass<T>): TableReader<T?>?
}

interface TableReader<T> {
    fun readFromTable(list: List<Any?>): T?
}

class ClassTableReaderPair<K : Any>(val clazz: KClass<K>, val tableReader: TableReader<K>)

infix fun <T : Any> KClass<T>.pairedWith(tableReader: TableReader<T>) =
    ClassTableReaderPair(this, tableReader)

class DefaultTableCollector private constructor(
    val map: Map<KClass<*>, TableReader<*>>,
    val collectionTables: Map<Pair<KClass<*>, String?>, TableReader<*>>
) : TableCollector {

    constructor(
        list: List<ClassTableReaderPair<*>>,
        collectionTables: Map<Pair<KClass<*>, String?>, TableReader<*>>
    ) : this(
        list.associate { it.clazz to it.tableReader }, collectionTables
    )

    override fun <T : Any> getTableReader(clazz: KClass<T>): TableReader<T?>? {
        val t = map[clazz]
        try {
            t as TableReader<T?>
        } catch (t: Throwable) {
            throw t
        }
        return t
    }

    override fun <T : Any, R : Any> getCollectionTableReader(clazz: KClass<T>, fieldName: String?): TableReader<R?>? {
        return collectionTables[clazz to fieldName] as TableReader<R?>?
    }
}

class DefaultTableReader<T : Any>(val map: Map<List<Any?>, T>) : TableReader<T> {
    override fun readFromTable(list: List<Any?>): T? {
        return map[list]
    }
}
