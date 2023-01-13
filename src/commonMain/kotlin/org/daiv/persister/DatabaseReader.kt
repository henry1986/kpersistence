package org.daiv.persister

interface DatabaseReader {
    fun get(i: Int): Any?
    fun getLong(i: Int): Long?
    fun next(): Boolean
}

class DefaultDatabaseReader(val list: List<List<Any?>>) : DatabaseReader {
    companion object {
        fun simple(l: List<Any?>) = DefaultDatabaseReader(listOf(l))
    }

    private var counter: Int = 0

    override fun get(i: Int): Any? {
        return list[counter].getOrElse(i - 1) {
            throw RuntimeException("failed with $i")
        }
    }

    override fun getLong(i: Int): Long? {
        return list[counter][i - 1] as Long
    }

    override fun next(): Boolean {
        if (counter < list.size - 1) {
            counter++
            return true
        }
        return false
    }
}

data class DRow(val list: List<Any?> = emptyList()) {

}


data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val rows: List<DRow> = listOf(DRow())) {
    constructor(list: List<Any?>) : this(DefaultDatabaseReader.simple(list), 1, listOf(DRow()))

    val list
        get() = rows.first().list

    fun next(): Pair<DatabaseRunner, Boolean> {
        return copy(count = 1) to databaseReader.next()
    }

    fun nextRow() = copy(rows = rows + DRow())

    fun next(databaseReader: DatabaseReader = this.databaseReader, count: Int, row: DRow): DatabaseRunner {
        return copy(databaseReader = databaseReader, count = count, rows = rows.dropLast(1) + row)
    }
}
