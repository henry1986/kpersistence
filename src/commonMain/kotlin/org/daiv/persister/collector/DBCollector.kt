package org.daiv.persister.collector

import kotlinx.serialization.descriptors.SerialDescriptor

data class DBEntry(val name: String, val serialDescriptor: SerialDescriptor, val value: Any?, val isKey:Boolean){
    init {
        if(name == "value_x" && value == 5 && isKey){
           val x = 5
        }
    }
}

data class DBMutableRow(val list: MutableList<DBEntry>) : EntryConsumer {

    fun done() = DBRow(list.toList())
    override fun add(t: DBEntry) {
        list.add(t)
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

    override fun entries() = list.flatMap { it.list }

    fun done() = DBCollection(list.map { it.done() })
}

data class DBRow(val entries: List<DBEntry>)
data class DBCollection(val rows: List<DBRow>) {
    fun toEntries() = rows.flatMap { it.entries }
}
