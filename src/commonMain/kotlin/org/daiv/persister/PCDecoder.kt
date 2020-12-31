package org.daiv.persister

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.SerializersModule

class PCDecoder(override val serializersModule: SerializersModule, val decoder: Decoder) : CompositeDecoder {

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        return deserializer.deserialize(decoder)
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        TODO("Not yet implemented")
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        TODO("Not yet implemented")
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        TODO("Not yet implemented")
    }

    var counter = 0
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (counter < 2) {
            println("first: ${descriptor.serialName}")
            descriptor.elementNames.forEach {
                println("second: ${it}")
            }
            val ret = counter
            counter++
            return ret
        }
        return DECODE_DONE
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        TODO("Not yet implemented")
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
        println("first: ${descriptor.serialName}")
        println("index: ${index}")
        descriptor.elementNames.forEach {
            println("second: ${it}")
        }
        println("decoded int")
        return 5
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        TODO("Not yet implemented")
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        TODO("Not yet implemented")
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        println("first: ${descriptor.serialName}")
        println("index: ${index}")
        descriptor.elementNames.forEach {
            println("second: ${it}")
        }
        println("decoded string")
        return "Hello"
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}