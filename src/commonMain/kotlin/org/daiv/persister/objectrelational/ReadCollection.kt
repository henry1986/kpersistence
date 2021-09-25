package org.daiv.persister.objectrelational

data class ReadCollection(val nativeReads: NativeReads, val dataRequester: DataRequester) {
    fun <T> requestValue(reader: ObjectRelationalReader<T>): T {
        return dataRequester.requestData(reader.readKey(this), reader)
    }
}

data class ListNativeReads(val list: List<List<Any?>>, private var index: Int = 0, var counter: Int = 0) : NativeReads {
    private var currentList: List<Any?> = list[index]
    fun get(): Any? {
        val counter = this.counter
        this.counter++
        if(counter >= currentList.size ){
            throw IndexOutOfBoundsException("counter $counter to big for list: $currentList")
        }
        return currentList[counter]
    }

    override fun readInt(): Int {
        return get() as Int
    }

    override fun readBoolean(): Boolean {
        return get() as Boolean
    }

    override fun readDouble(): Double {
        return get() as Double
    }

    override fun readString(): String {
        return get() as String
    }

    override fun readByte(): Byte {
        return get() as Byte
    }

    override fun readFloat(): Float {
        return get() as Float
    }

    override fun readLong(): Long {
        return get() as Long
    }

    override fun readShort(): Short {
        return get() as Short
    }

    override fun readChar(): Char {
        return get() as Char
    }

    override fun nextRow(): Boolean {
        if (index == list.size) {
            return false
        }
        val index = this.index
        this.index++
        currentList = list[index]
        counter = 0
        return true
    }
}

//fun testRead() {
//    val c = ListHolderEx.objectRelationalReader
//    val l = listOf<Any?>()
//    val l2 = listOf<Any?>(listOf(5, 0, 0, 2))
//    val readCollection = ReadCollection(, object : DataRequester {
//        override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
//            return objectRelationalMapper.read(ReadCollection(, object : DataRequester {}))
//        }
//    })
//    val read = c.read(readCollection)
//}