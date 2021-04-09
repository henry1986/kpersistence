package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule

class KeyGetterValueFilter(
    val descriptor: SerialDescriptor,
    override val prefix: String?,
    override val collectedValues: DBMutableCollector = DBMutableCollector()
) : KeyValueAdder, ElementAdder, Beginable by collectedValues {

    override fun is2Add(index: Int) = descriptor.isKey(index)
    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        addValue(descriptor, index, value)
    }
}

data class KeyCollector private constructor(
    val descriptor: SerialDescriptor,
    private val keyGetterValueFilter: KeyGetterValueFilter,
    override val isCollection: Boolean
) : ValueAdder by keyGetterValueFilter, ElementAdder by keyGetterValueFilter, EncoderStrategy, Beginable by keyGetterValueFilter {
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
    init {
        begin()
    }
}
