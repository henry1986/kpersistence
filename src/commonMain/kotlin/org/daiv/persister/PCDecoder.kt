package org.daiv.persister

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import org.daiv.persister.table.moreKeys


interface NextIndexGetter {
    fun getNextIndex(descriptor: SerialDescriptor): Int
}

abstract class DefaultNextIndexGetter(val startIndex: Int) : NextIndexGetter {
    abstract fun max(descriptor: SerialDescriptor): Int

    var counter: Int = startIndex
    fun increase(): Int {
        val old = counter
        counter++
        return old
    }

    override fun getNextIndex(descriptor: SerialDescriptor): Int {
        val size = max(descriptor)
        val place = increase()
        if (place < size) {
            return place
        }
        return DECODE_DONE
    }
}

class KeyNextIndexGetter(val index: Int) : DefaultNextIndexGetter(index) {
    override fun max(descriptor: SerialDescriptor) = descriptor.moreKeys().amount + index
}

class AllNextIndexGetter : DefaultNextIndexGetter(0) {
    override fun max(descriptor: SerialDescriptor) = descriptor.elementsCount
}

interface NativeReads {
    fun readLong(index: Int): Long
    fun readInt(index: Int): Int
    fun readShort(index: Int): Short
    fun readString(index: Int): String
    fun readBoolean(index: Int): Boolean
    fun readByte(index: Int): Byte
    fun readChar(index: Int): Char
    fun readFloat(index: Int): Float
    fun readDouble(index: Int): Double
}

class ReadDataCollector {
    private val collector: MutableList<Any?> = mutableListOf()
    fun <T> add(any: T): T {
        collector.add(any)
        return any
    }

    fun all() = collector.toList()
}

class NativeReadCollector(val readData: NativeReads, val collector: ReadDataCollector) : NativeReads {
    override fun readLong(index: Int) = collector.add(readData.readLong(index))

    override fun readInt(index: Int): Int = collector.add(readData.readInt(index))

    override fun readShort(index: Int): Short = collector.add(readData.readShort(index))

    override fun readString(index: Int): String = collector.add(readData.readString(index))

    override fun readBoolean(index: Int): Boolean = collector.add(readData.readBoolean(index))

    override fun readByte(index: Int): Byte = collector.add(readData.readByte(index))

    override fun readChar(index: Int): Char = collector.add(readData.readChar(index))

    override fun readFloat(index: Int): Float = collector.add(readData.readFloat(index))

    override fun readDouble(index: Int): Double = collector.add(readData.readDouble(index))
}

interface NextRowMover {
    fun next()
}

interface ReadData : NextIndexGetter, NativeReads, NextRowMover {
//    fun <T> request(descriptor: DeserializationStrategy<T>, index: Int, keys: List<Any?>): ReadData
}



class ReadDataBuilder(val readData: ReadData, val nativeReads: NativeReads, val nextIndexGetter: NextIndexGetter) : ReadData,
                                                                                                                    NativeReads by nativeReads,
                                                                                                                    NextRowMover by readData,
                                                                                                                    NextIndexGetter by nextIndexGetter {
//    override fun <T> request(descriptor: DeserializationStrategy<T>, index: Int, keys: List<Any?>): ReadData {
//    }
}

class PCDecoder(override val serializersModule: SerializersModule, val readData: ReadData) : CompositeDecoder {

    companion object {
        private val logger = KotlinLogging.logger("org.daiv.persister.PCDecoder")
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        if (previousValue != null) {
            throw RuntimeException("Found a previous value: $previousValue")
        }
        val collector = ReadDataCollector()
        try {
            val decoder = PDecoder(
                serializersModule, ReadDataBuilder(readData, NativeReadCollector(readData, collector), KeyNextIndexGetter(index))
            )
            deserializer.deserialize(decoder)
        } catch (e: SerializationException) {
            logger.error { "received $e" }
        }
        val all = collector.all()
        all.forEach {
            logger.trace { "got: $it" }
        }
        return deserializer.deserialize(PDecoder(serializersModule, readData))
    }

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean {
        return readData.readBoolean(index)
    }

    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte {
        return readData.readByte(index)
    }

    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char {
        return readData.readChar(index)
    }

    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double {
        return readData.readDouble(index)
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        return readData.getNextIndex(descriptor)
    }

    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float {
        return readData.readFloat(index)
    }

    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int {
//        println("first: ${descriptor.serialName}")
//        println("index: ${index}")
        println("decode: $descriptor")
        descriptor.elementNames.forEach {
            println("second: ${it}")
        }
//        println("decoded int")
        return readData.readInt(index)
    }

    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long {
        return readData.readLong(index)
    }

    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short {
        return readData.readShort(index)
    }

    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String {
        println("decodeString: first: ${descriptor.serialName}")
        println("index: ${index}")
        descriptor.elementNames.forEach {
            println("second: ${it}")
        }
        println("decoded string")
        return readData.readString(index)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }
}