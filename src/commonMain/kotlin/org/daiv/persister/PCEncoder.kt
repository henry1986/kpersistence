package org.daiv.persister

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.collector.EncoderStrategy
import org.daiv.persister.collector.ValueAdder
import org.daiv.persister.table.*

class PCEncoder(
    override val serializersModule: SerializersModule,
    val valueAdder: EncoderStrategy,
    val isCollection: Boolean
) :
    CompositeEncoder,
    ValueAdder by valueAdder {


    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        addValue(descriptor, index, value)
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        addValue(descriptor, index, value)
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        addValue(descriptor, index, value)
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        addValue(descriptor, index, value)
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        addValue(descriptor, index, value)
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        addValue(descriptor, index, value)
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        addValue(descriptor, index, value)
    }

    @ExperimentalSerializationApi
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        TODO("Not yet implemented")
    }


    override fun <T> encodeSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        valueAdder.addElement(descriptor, index, serializer, value)
        valueAdder.encodeSubInstance(serializersModule, descriptor, index, serializer, value, isCollection)
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        addValue(descriptor, index, value)
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        addValue(descriptor, index, "\"$value\"")
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}