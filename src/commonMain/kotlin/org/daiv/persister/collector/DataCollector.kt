package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.table.*

data class DBEntry(val name: String, val serialDescriptor: SerialDescriptor, val value: Any?)

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

class DBMutableCollector : Beginable, EntryConsumer {
    private val list: MutableList<DBMutableRow> = mutableListOf()
    override fun begin() {
        list.add(DBMutableRow(mutableListOf()))
    }

    fun new():DBMutableRow{
        val row=DBMutableRow(mutableListOf())
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


interface SubAdder {
    fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    )
}

interface ValueAdder : Valueable {
    fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?)
}

interface ElementAdder {
    fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T)

    companion object {
        val noAdd = object : ElementAdder {
            override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {

            }
        }
    }
}

interface EncoderStrategy : ValueAdder, SubAdder, ElementAdder {
    val isCollection: Boolean
}

fun interface EncoderStrategyFactory {
    fun build(isCollection: Boolean): EncoderStrategy
}

interface Prefixable {
    val prefix: String?
}

interface Valueable {
    fun values(): List<DBEntry>
}

interface ValueCollector : Valueable {
    val collectedValues: EntryConsumer
    override fun values(): List<DBEntry> {
        return collectedValues.entries()
    }
}

interface KeyValueAdder : Prefixable, ValueAdder, Valueable, ValueCollector {

    fun is2Add(index: Int): Boolean
    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        if (is2Add(index)) {
            val name = descriptor.elementNames.toList()[index].prefix(prefix)
            val subDescriptor = descriptor.elementDescriptors.toList()[index]
            collectedValues.add(DBEntry(name, subDescriptor, value))
        }
    }
}


fun SerialDescriptor.isKey(index: Int) = moreKeys().amount > index

interface Beginable {
    fun begin()

    companion object {
        val noBegin = object : Beginable {
            override fun begin() {
            }
        }
    }
}

class DefaultValueFilter(
    val descriptor: SerialDescriptor,
    val keyOnly: Boolean,
    val isCollection: Boolean,
    override val prefix: String?,
    override val collectedValues: EntryConsumer
) : KeyValueAdder {
    override fun is2Add(index: Int) = !keyOnly || descriptor.isKey(index)
    fun sub(descriptor: SerialDescriptor, index: Int, isCollection: Boolean): DefaultValueFilter {
        return if (this.isCollection) {
            collectedValues.add(DBEntry("key", Int.serializer().descriptor, index))
            DefaultValueFilter(descriptor, true, isCollection, "value".prefix(prefix), collectedValues)
        } else {
            val list = descriptor.elementNames.toList()
            if (index >= list.size) {
                throw IndexOutOfBoundsException("index $index to high for list $list")
            }
            val name = list[index]
            DefaultValueFilter(descriptor, true, isCollection, name.prefix(prefix), collectedValues)
        }
    }
}

data class DataCollector internal constructor(
    private val valueFilter: DefaultValueFilter,
    val collector:DBMutableCollector,
    val elementAdder: ElementAdder,
    val parentIsCollection: Boolean,
    override val isCollection: Boolean
) : ValueAdder by valueFilter, EncoderStrategy, ElementAdder by elementAdder {
    constructor(
        collectedValues: DBMutableCollector,
        descriptor: SerialDescriptor,
        elementAdder: ElementAdder,
        keyOnly: Boolean,
        prefix: String?,
        parentIsCollection: Boolean,
        isCollection: Boolean
    ) : this(
        DefaultValueFilter(descriptor, keyOnly, isCollection, prefix, collectedValues.new()),
        collectedValues,
        elementAdder,
        parentIsCollection,
        isCollection
    )

    fun next(descriptor: SerialDescriptor, index: Int, isCollection: Boolean) =
        copy(
            valueFilter = valueFilter.sub(descriptor, index, isCollection),
            parentIsCollection = this.isCollection,
            isCollection = isCollection
        )

    init {
//        begin()
    }

    /**
     * get the key of the subinstance
     */
    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        if(valueFilter.is2Add(index)) {
            if (value !is List<*>) {
                val p = PEncoder(serializersModule, ObjectEncoderStrategyFactory(descriptor, index, this))
                serializer.serialize(p, value)
            }
        }
    }
}

class ObjectEncoderStrategyFactory(val descriptor: SerialDescriptor, val index: Int, val dataCollector: DataCollector) :
    EncoderStrategyFactory {
    override fun build(isCollection: Boolean): EncoderStrategy {
        return dataCollector.next(descriptor, index, isCollection)
    }
}



