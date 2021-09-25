package org.daiv.persister.collector

import kotlinx.serialization.descriptors.SerialDescriptor
import org.daiv.persister.table.InsertionResult

data class DBEntry(val name: String, val serialDescriptor: SerialDescriptor, val value: Any?, val isKey: Boolean) {
    fun prefix(prefix: String, isKey: Boolean): DBEntry = copy(name = "${prefix}_${name}", isKey = isKey)
}

data class DBMutableRow(val list: MutableList<DBEntry>) : EntryConsumer {

    fun done() = DBRow(list.toList())
    override fun add(t: DBEntry) {
        list.add(t)
    }

    fun addEntries(entries: List<DBEntry>){
        list.addAll(entries)
    }

    override fun entries() = list.toList()
}

interface ValueConsumer<T> {
    fun add(t: T)
}

interface EntryConsumer : ValueConsumer<DBEntry> {
    fun entries(): List<DBEntry>
}

class DBMutableCollector(val onNewRow: (DBMutableRow) -> Unit = {}) : Beginable, EntryConsumer {
    private val list: MutableList<DBMutableRow> = mutableListOf()
    override fun begin() {
        list.add(DBMutableRow(mutableListOf()))
    }

    fun new(): DBMutableRow {
        val row = DBMutableRow(mutableListOf())
        onNewRow(row)
        list.add(row)
        return row
    }

    override fun add(dbEntry: DBEntry) {
        val last = list.last()
        last.list.add(dbEntry)
    }

    private val listBuilderList = mutableListOf<suspend (List<DBEntry>) -> Unit>()

    fun insertionResultBuilder(build: suspend (List<DBEntry>) -> Unit) {
        listBuilderList.add(build)
    }

    override fun entries() = list.flatMap { it.list }

    fun done() = DBCollection(list.map { it.done() }, listBuilderList.toList())
}

data class DBRow(val entries: List<DBEntry>)
data class DBCollection(val rows: List<DBRow>, val listBuilderList: List<suspend (List<DBEntry>) -> Unit>) {
    fun toEntries() = rows.flatMap { it.entries }
}
