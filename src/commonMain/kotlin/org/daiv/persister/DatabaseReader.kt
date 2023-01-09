package org.daiv.persister

interface DatabaseReader {
    fun next(i:Int):Any?
    fun nextLong(i:Int):Long?
}