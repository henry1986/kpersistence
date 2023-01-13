package org.daiv.persister

import kotlin.reflect.KClass

interface TableCollector {
    fun <T : Any> getTableReader(clazz: KClass<T>): TableReader<T?>?
}

interface TableReader<T> {
    fun readFromTable(list: List<Any?>): T?
}

class ClassTableReaderPair<K : Any>(val clazz: KClass<K>, val tableReader: TableReader<K>)

infix fun <T : Any> KClass<T>.pairedWith(tableReader: TableReader<T>) = ClassTableReaderPair(this, tableReader)

class DefaultTableCollector private constructor(val map: Map<KClass<*>, TableReader<*>>) : TableCollector {

    constructor(list: List<ClassTableReaderPair<*>>) : this(list.associate { it.clazz to it.tableReader })

    override fun <T : Any> getTableReader(clazz: KClass<T>): TableReader<T?>? {
        return map[clazz] as TableReader<T?>?
    }
}

class DefaultTableReader<T : Any>(val map: Map<List<Any?>, T>) : TableReader<T> {
    override fun readFromTable(list: List<Any?>): T? {
        return map[list]
    }
}
