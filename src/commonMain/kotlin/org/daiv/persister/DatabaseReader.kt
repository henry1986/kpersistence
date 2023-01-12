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
        return list[counter].getOrElse(i - 1){
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