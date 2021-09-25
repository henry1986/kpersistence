package org.daiv.persister.collector.encoder

import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import org.daiv.coroutines.CalculationCollectionWrite
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.*
import org.daiv.persister.table.InsertionCache
import org.daiv.persister.table.InsertionResult
import org.daiv.persister.table.tableDescriptor

suspend fun <T> List<T>.toRows(
    objectKey: List<DBEntry>,
    module: SerializersModule,
    serializer: SerializationStrategy<List<T>>,
    cache: InsertionCache
): InsertionResult {

    val s = CollectionEncoderStrategy(this, objectKey, cache)
    serializer.serialize(PEncoder(module) { s }, this)
    return s.insertionResult(serializer.descriptor)
}

suspend fun <T> List<T>.toRowsOuter(
    objectKey: List<DBEntry>,
    module: SerializersModule,
    serializer: SerializationStrategy<List<T>>,
    cache: InsertionCache
): InsertionResult {
    val channel = Channel<InsertionResult>()
    cache.insert(this, serializer.descriptor, {
        toRows(objectKey, module, serializer, cache)
    }) {
        channel.send(it)
    }
    return channel.receive()
}

class CollectionEncoderStrategy(val t: Any, val objectKey: List<DBEntry>, val cache: InsertionCache) : EncoderStrategy {
    companion object {
        private val logger = KotlinLogging.logger("org.daiv.persister.collector.CollectionEncoderStrategy")
    }

    val collection = DBMutableCollector()
    val write = CalculationCollectionWrite("CollectionEncoder - $t", cache)

    suspend fun insertionResult(descriptor: SerialDescriptor): InsertionResult {
        write.join()
        logger.trace { "insertResult: $descriptor" }
        return InsertionResult(collection.done().rows, descriptor.tableDescriptor())
    }

    private fun DBMutableRow.addObjectKey() = objectKey.forEach {
        add(it)
    }


    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        collection.newRow(index).add(DBEntry("value", Int.serializer().descriptor, value, false))
    }

    private fun DBMutableCollector.newRow(index: Int): DBMutableRow {
        return new().apply {
            addObjectKey()
            addIndex(index)
        }
    }

    override fun values(): List<DBEntry> {
        return collection.done().toEntries()
    }

    private fun DBMutableRow.addIndex(index: Int) = add(DBEntry("index", Int.serializer().descriptor, index, true))

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        if (serializer.isSimpleSerializer()) {
            addValue(descriptor, index, value)
            return
        }

        write.insert(value, serializer.descriptor, {
            it.innerToEntries(serializersModule, serializer, cache)
        }) {
            val entries = it.keys().map { it.prefix("value", false) }
            collection.newRow(index).addEntries(entries)
        }
    }

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
    }
}
