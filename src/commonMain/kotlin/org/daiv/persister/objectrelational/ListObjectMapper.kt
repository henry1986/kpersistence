package org.daiv.persister.objectrelational

class ListObjectReader<E>(
    private val keyMapper: ObjectRelationalReader<*>,
    private val keys: List<ReadEntryTask>,
    private val elementMapper: ObjectRelationalReader<E>
) : ObjectRelationalReader<List<E>> {

    override fun read(readCollection: ReadCollection): List<E> {
        val l = mutableListOf<Pair<Int, E>>()
        while (readCollection.nativeReads.nextRow()) {
            keys.map { it.readKey(readCollection) }
            val index = readCollection.nativeReads.readInt()
            val e = readCollection.dataRequester.requestData(elementMapper.readKey(readCollection), elementMapper)
            l.add(index to e)
        }
        l.sortBy { it.first }
        return l.map { it.second }
    }

    override fun readKey(readCollection: ReadCollection): List<ReadEntry> {
        keys.map { it.readKey(readCollection) }
        val index = readCollection.nativeReads.readInt()
        return emptyList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ListObjectReader<*>

        if (keyMapper != other.keyMapper) return false
        if (elementMapper != other.elementMapper) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keyMapper.hashCode()
        result = 31 * result + elementMapper.hashCode()
        return result
    }

    override fun toString(): String {
        return "ListObjectReader(keyMapper=$keyMapper, elementMapper=$elementMapper)"
    }


}

fun <T> List<T>.hashCodeX(hashCodeX: T.(Int) -> Int): Int {
    var hashCode = 1
    var prime = 31
    var i = 0
    while (i < size) {
        hashCode = hashCode * prime + get(i).hashCodeX(i)
        i++
    }
    return hashCode
}

fun listHeader(masterEntries: List<HeadEntry>, elementMapper: ObjectRelationalHeader) =
    ObjectRelationalHeaderData(
        masterEntries + HeadEntry(masterEntries.first().parameterList.first(), "index", "Int", true),
        elementMapper.keyHead("value_", null).noKey(),
        listOf({ elementMapper })
    )


class ListObjectWriter<E>(
    val elementMapper: () -> ObjectRelationalWriter<E>
) : ObjectRelationalWriter<List<E>?> {

//    private val keys by lazy { keys.map { it.copy(name = "ref_".build(it.name)) } }

    override fun write(
        higherKeys: List<WriteEntry>,
        t: List<E>?,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteRow> {
        return t?.mapIndexed { i, it ->
            WriteRow(
                higherKeys + WriteEntry("index", i, true) + elementMapper().writeKey(
                    "value_",
                    it,
                    hashCodeCounterGetter
                )
            )
        } ?: emptyList()
    }

    override fun writeRow(
        prefix: String?,
        t: List<E>?,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteEntry> {
        return emptyList()
    }

    override fun writeKey(
        prefix: String?,
        t: List<E>?,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteEntry> {
        return emptyList()
    }


    override suspend fun subs(t: List<E>?, taskReceiver: TaskReceiver, hashCodeCounterGetter: HashCodeCounterGetter) {
        t?.forEach {
            it?.let { taskReceiver.task(it, emptyList(), elementMapper()) }
        }
    }

    override fun <R> preWriteKey(prefix: String?, isKey: Boolean, func: R.() -> List<E>?): List<PreWriteEntry<R>> {
        TODO()
    }
}
