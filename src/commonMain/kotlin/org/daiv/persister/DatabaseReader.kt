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
        return list[counter][i - 1]
    }

    override fun getLong(i: Int): Long? {
        return list[counter][i - 1] as Long
    }

    override fun next(): Boolean {
        if (counter < list.size) {
            counter++
            return true
        }
        return false
    }
}