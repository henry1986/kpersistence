package org.daiv.persister

interface DatabaseReader {
    fun next(i:Int):Any?
    fun nextLong(i:Int):Long?
}

class DefaultDatabaseReader(val list:List<Any?>):DatabaseReader{
    override fun next(i: Int): Any? {
        return list[i - 1]
    }

    override fun nextLong(i: Int): Long? {
        return list[i - 1] as Long
    }
}