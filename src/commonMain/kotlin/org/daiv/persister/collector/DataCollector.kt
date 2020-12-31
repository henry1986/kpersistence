package org.daiv.persister.collector

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.MoreKeys
import org.daiv.persister.MoreKeysData
import org.daiv.persister.PEncoder
import org.daiv.persister.table.ElementCache
import org.daiv.persister.table.default
import org.daiv.persister.table.moreKeys
import org.daiv.persister.table.prefix
import kotlin.reflect.KClass

data class CollectedValue(val name: String, val serialDescriptor: SerialDescriptor, val value: Any?)

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

interface ValueAdder {
    fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?)
}

interface ElementAdder {
    fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T)
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
    fun values(): List<CollectedValue>
}

interface ValueFilter : Prefixable, ValueAdder, Valueable {
    val collectedValues: MutableList<CollectedValue>
    override fun values() = collectedValues.toList()

    fun is2Add(index: Int): Boolean
    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        if (is2Add(index)) {
            val name = descriptor.elementNames.toList()[index].prefix(prefix)
            val subDescriptor = descriptor.elementDescriptors.toList()[index]
            collectedValues.add(CollectedValue(name, subDescriptor, value))
        }
    }
}

abstract class AbstractValueFilter(override val prefix: String?, override val collectedValues: MutableList<CollectedValue>) : ValueFilter

fun SerialDescriptor.isKey(index: Int) = moreKeys().amount > index

class DefaultValueFilter(
    val descriptor: SerialDescriptor,
    val keyOnly: Boolean,
    prefix: String?,
    collectedValues: MutableList<CollectedValue>
) : AbstractValueFilter(prefix, collectedValues) {
    override fun is2Add(index: Int) = !keyOnly || descriptor.isKey(index)
    fun sub(descriptor: SerialDescriptor, index: Int): DefaultValueFilter {
        val name = descriptor.elementNames.toList()[index]
        return DefaultValueFilter(descriptor, true, name.prefix(prefix), collectedValues)
    }
}

data class DataCollector private constructor(
    private val valueFilter: DefaultValueFilter,
    override val isCollection: Boolean
) : ValueAdder by valueFilter, EncoderStrategy, Valueable by valueFilter {
    constructor(descriptor: SerialDescriptor, keyOnly: Boolean, prefix: String?, isCollection: Boolean)
            : this(DefaultValueFilter(descriptor, keyOnly, prefix, mutableListOf()), isCollection)

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {

    }

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        serializer.serialize(PEncoder(serializersModule) { DataCollector(valueFilter.sub(descriptor, index), it) }, value)
    }
}

class KeyGetterValueFilter(val descriptor: SerialDescriptor, override val prefix: String?) : ValueFilter, ElementAdder {
    override val collectedValues: MutableList<CollectedValue> = mutableListOf()
    override fun is2Add(index: Int) = descriptor.isKey(index)
    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        addValue(descriptor, index, value)
    }
}

data class KeyCollector private constructor(
    val descriptor: SerialDescriptor,
    private val keyGetterValueFilter: KeyGetterValueFilter,
    override val isCollection: Boolean
) : ValueAdder by keyGetterValueFilter, ElementAdder by keyGetterValueFilter, EncoderStrategy, Valueable by keyGetterValueFilter {
    constructor(descriptor: SerialDescriptor, prefix: String?, isCollection: Boolean) : this(
        descriptor,
        KeyGetterValueFilter(descriptor, prefix),
        isCollection
    )

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
    }
}


class ElementCollector(val elementCache: ElementCache, val parentKey: ClassKey, override val isCollection: Boolean) : EncoderStrategy {
    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {

    }

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        fun ClassKey.serialize() =
            serializer.serialize(PEncoder(serializersModule) { isCollection -> ElementCollector(elementCache, this, isCollection) }, value)

        value?.let {
            val key = serializer.getKeys(it)
            if (key.isCollection) {
                when (value) {
                    is List<*> -> {

                    }
                    is Set<*> -> {
                    }
                    is Map<*, *> -> {
                    }
                }
                key.serialize()
            } else if (!elementCache.exists(serializer, key)) {
                elementCache.set(serializer, key, value)
                key.serialize()
            }
        }
    }

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
    }
}
