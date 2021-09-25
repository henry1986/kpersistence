package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.table.*

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

interface EncoderStrategy : ValueAdder, SubAdder, ElementAdder

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
    fun isKey(descriptor: SerialDescriptor, index: Int): Boolean

    fun is2Add(index: Int): Boolean
    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        if (is2Add(index)) {
            val name = descriptor.elementNames.toList()[index].prefix(prefix)
            val subDescriptor = descriptor.elementDescriptors.toList()[index]
            collectedValues.add(DBEntry(name, subDescriptor, value, isKey(descriptor, index)))
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

fun SerialDescriptor.name(index: Int) =
    elementNames.toList().getOrNull(index) ?: throw NullPointerException("there is no name for index: $index at $this")

fun SerialDescriptor.subDescriptor(index: Int) =
    elementDescriptors.toList().getOrNull(index) ?: throw NullPointerException("there is no subDescriptor for index: $index at $this")

class DefaultValueFilter(
    val descriptor: SerialDescriptor,
    val asKey: Boolean,
    val keyOnly: Boolean,
    override val prefix: String?,
    override val collectedValues: EntryConsumer
) : KeyValueAdder {
    override fun isKey(descriptor: SerialDescriptor, index: Int) = asKey && descriptor.isKey(index)
    override fun is2Add(index: Int) = !keyOnly || descriptor.isKey(index)
    fun sub(descriptor: SerialDescriptor, index: Int, isCollection: Boolean): DefaultValueFilter {
        val list = descriptor.elementNames.toList()
        if (index >= list.size) {
            throw IndexOutOfBoundsException("index $index to high for list $list")
        }
        val name = list[index]
        return DefaultValueFilter(descriptor, isKey(descriptor, index), true, name.prefix(prefix), collectedValues)
    }
}

data class DataCollector internal constructor(
    private val valueFilter: DefaultValueFilter,
    val collector: DBMutableCollector,
    val elementAdder: ElementAdder,
    val parentIsCollection: Boolean,
) : ValueAdder by valueFilter, EncoderStrategy, ElementAdder by elementAdder {
    constructor(
        collectedValues: DBMutableCollector,
        descriptor: SerialDescriptor,
        elementAdder: ElementAdder,
        asKey: Boolean,
        keyOnly: Boolean,
        prefix: String?,
        parentIsCollection: Boolean,
    ) : this(
        DefaultValueFilter(descriptor, asKey, keyOnly, prefix, collectedValues.new()),
        collectedValues,
        elementAdder,
        parentIsCollection,
    )

    fun next(descriptor: SerialDescriptor, index: Int, isCollection: Boolean) =
        copy(valueFilter = valueFilter.sub(descriptor, index, isCollection))

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
        if (valueFilter.is2Add(index)) {
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



