package org.daiv.persister.objectrelational

import kotlin.reflect.KType

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

class SetObjectWriter<E>(val elementMapper: () -> KeyWriter<E>) : RowWriter<Set<E>?> {
    override fun write(
        higherKeys: List<WriteEntry>,
        t: Set<E>?,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteRow> {
        return t?.mapIndexed { i, it ->
            WriteRow(
                higherKeys + elementMapper().writeKey(
                    "value_",
                    it,
                    hashCodeCounterGetter
                )
            )
        } ?: emptyList()
    }
}

class MapObjectWriter<K, V>(
    val keyMapper: () -> KeyWriter<K>,
    val elementMapper: () -> KeyWriter<V>
) : RowWriter<Map<K, V>?> {

//    private val keys by lazy { keys.map { it.copy(name = "ref_".build(it.name)) } }

    override fun write(
        higherKeys: List<WriteEntry>,
        t: Map<K, V>?,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteRow> {
        return t?.map { it ->
            WriteRow(
                higherKeys
                        + keyMapper().writeKey("key_", it.key, hashCodeCounterGetter)
                        + elementMapper().writeKey("value_", it.value, hashCodeCounterGetter)
            )
        } ?: emptyList()
    }
}

class NativeObjectWriter(private val type: KType) : KeyWriter<Any>, PrefixBuilder {
    override fun writeKey(prefix: String?, t: Any, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry> {
        return listOf(WriteEntry(prefix.build(type.typeName()!!), t, true))
    }
}

class ListObjectWriter<E>(
    val elementMapper: () -> KeyWriter<E>
) : RowWriter<List<E>?> {

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
}
