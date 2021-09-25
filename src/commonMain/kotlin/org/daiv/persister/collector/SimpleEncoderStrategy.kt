package org.daiv.persister.collector

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import org.daiv.coroutines.CalculationCollectionWrite
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.encoder.toRows
import org.daiv.persister.table.InsertionCache
import org.daiv.persister.table.InsertionResult
import org.daiv.persister.table.tableDescriptor


internal suspend fun <T> T.innerToEntries(
    module: SerializersModule,
    serializer: SerializationStrategy<T>,
    cache: InsertionCache
): InsertionResult {
    val s = SimpleEncoderStrategy(this, cache)
    serializer.serialize(PEncoder(module) { s }, this)
    return s.insertionResult(serializer.descriptor)
}

suspend fun <T> T.toEntries(module: SerializersModule, serializer: SerializationStrategy<T>, cache: InsertionCache): InsertionResult {
    val channel = Channel<InsertionResult>()
    cache.insert(this, serializer.descriptor, {
        innerToEntries(module, serializer, cache)
    }) {
        channel.send(it)
    }
    return channel.receive()
}

class SimpleEncoderStrategy(val t: Any?, val cache: InsertionCache) : EncoderStrategy {
    val collection = DBMutableCollector()
    val write = CalculationCollectionWrite("encoder - $t", cache)

    companion object {
        private val logger = KotlinLogging.logger("org.daiv.persister.collector.SimpleEncoderStrategy")
    }

    init {
        collection.new()
    }

    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        val name = descriptor.name(index)
        val sub = descriptor.subDescriptor(index)
        collection.add(DBEntry(name, sub, value, descriptor.isKey(index)))
    }

    override fun values(): List<DBEntry> {
        return collection.done().toEntries()
    }

    suspend fun insertionResult(descriptor: SerialDescriptor): InsertionResult {
        write.join()
        val done = collection.done()
        val rows = done.rows
        val i = InsertionResult(rows, descriptor.tableDescriptor())
        val keys = i.keys()
        done.listBuilderList.map {
            it(keys)
        }
        logger.trace { "insertionResult $rows" }
        return i
    }

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        if (value !is List<*>) {
            write.insert(value, serializer.descriptor, {
                it.innerToEntries(serializersModule, serializer, cache)
            }) { insertionResult ->
                insertionResult.entries(descriptor, index).forEach {
                    collection.add(it)
                }
            }
        } else {
            collection.insertionResultBuilder { entries ->
                write.insert(value, serializer.descriptor, {
                    it.toRows(entries, serializersModule, serializer as SerializationStrategy<List<Any?>>, cache)
                }) { }
            }
        }
    }

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
    }
}
