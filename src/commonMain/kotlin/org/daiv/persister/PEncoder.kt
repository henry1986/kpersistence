package org.daiv.persister

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.collector.EncoderStrategyFactory

class PEncoder(
    override val serializersModule: SerializersModule,
    val valueAdder: EncoderStrategyFactory,
) : Encoder {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val encoderStrategy = valueAdder.build(false)
//        encoderStrategy.begin()
        return PCEncoder(serializersModule, encoderStrategy, false)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        val encoderStrategy = valueAdder.build(true)
//        encoderStrategy.begin()
        return PCEncoder(serializersModule, encoderStrategy, true)
    }

    override fun encodeBoolean(value: Boolean) {
        TODO("Not yet implemented")
    }

    override fun encodeByte(value: Byte) {
        TODO("Not yet implemented")
    }

    override fun encodeChar(value: Char) {
        TODO("Not yet implemented")
    }

    override fun encodeDouble(value: Double) {
        TODO("Not yet implemented")
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        TODO("Not yet implemented")
    }

    override fun encodeFloat(value: Float) {
        TODO("Not yet implemented")
    }

    override fun encodeInt(value: Int) {
//        TODO("Not yet implemented")
    }

    override fun encodeLong(value: Long) {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun encodeNull() {
        TODO("Not yet implemented")
    }

    override fun encodeShort(value: Short) {
        TODO("Not yet implemented")
    }

    override fun encodeString(value: String) {
        TODO("Not yet implemented")
    }
}
