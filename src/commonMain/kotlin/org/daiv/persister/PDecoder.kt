package org.daiv.persister

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

class PDecoder(override val serializersModule: SerializersModule) : Decoder {

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return PCDecoder(serializersModule, this)
    }
    override fun decodeBoolean(): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeByte(): Byte {
        TODO("Not yet implemented")
    }

    override fun decodeChar(): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDouble(): Double {
        TODO("Not yet implemented")
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        TODO("Not yet implemented")
    }

    override fun decodeFloat(): Float {
        TODO("Not yet implemented")
    }

    override fun decodeInt(): Int {
        TODO("Not yet implemented")
    }

    override fun decodeLong(): Long {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNotNullMark(): Boolean {
        TODO("Not yet implemented")
    }

    @ExperimentalSerializationApi
    override fun decodeNull(): Nothing? {
        TODO("Not yet implemented")
    }

    override fun decodeShort(): Short {
        TODO("Not yet implemented")
    }

    override fun decodeString(): String {
        TODO("Not yet implemented")
    }
}