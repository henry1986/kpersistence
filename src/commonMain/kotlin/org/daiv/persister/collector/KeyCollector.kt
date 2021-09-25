package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

class KeyGetterValueFilter(
    val descriptor: SerialDescriptor,
    override val prefix: String?,
    override val collectedValues: DBMutableCollector
) : KeyValueAdder, ElementAdder, Beginable by collectedValues {
    override fun isKey(descriptor: SerialDescriptor, index: Int) = descriptor.isKey(index)
    override fun is2Add(index: Int) = isKey(descriptor, index)
    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        addValue(descriptor, index, value)
    }
}

data class KeyCollector private constructor(
    val descriptor: SerialDescriptor,
    private val keyGetterValueFilter: KeyGetterValueFilter,
) : ValueAdder by keyGetterValueFilter, ElementAdder by keyGetterValueFilter, EncoderStrategy, Beginable by keyGetterValueFilter {
    constructor(descriptor: SerialDescriptor, prefix: String?, dbMutableCollector: DBMutableCollector) : this(
        descriptor,
        KeyGetterValueFilter(descriptor, prefix, dbMutableCollector),
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

    init {
        begin()
    }
}
