package org.daiv.persister.collector.encoder

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import org.daiv.coroutines.CalculationCollectionWrite
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.DBEntry
import org.daiv.persister.collector.DBMutableCollector
import org.daiv.persister.collector.DBMutableRow
import org.daiv.persister.collector.EncoderStrategy
import org.daiv.persister.table.InsertionCache
import org.daiv.persister.table.InsertionResult
import org.daiv.persister.table.tableDescriptor

suspend fun <K, V> Map<K, V>.toRows(
    objectKey: List<DBEntry>,
    module: SerializersModule,
    serializer: SerializationStrategy<Map<K, V>>,
    cache: InsertionCache
): InsertionResult {
    val s = MapEncoderStrategy(this, objectKey, cache)
    serializer.serialize(PEncoder(module) { s }, this)
    return s.insertionResult(serializer.descriptor)
}


interface ObjectKeyAdder {
    val collection: DBMutableCollector
    val objectKey: List<DBEntry>

    private fun addObjectKey() = objectKey.forEach {
        collection.add(it)
    }

    private fun addIndex(index: Int) = collection.add(DBEntry("index", Int.serializer().descriptor, index, true))

    fun newRow(index: Int): DBMutableRow {
        return collection.new().apply {
            addObjectKey()
            addIndex(index)
        }
    }
}

data class TestPersistable(val x:Int)

class MapEncoderStrategy(val t: Any?, val objectKey: List<DBEntry>, val cache: InsertionCache) : EncoderStrategy {

    private val logger = KotlinLogging.logger("org.daiv.persister.collector.encoder.MapEncoderStrategy - $t")
    val collection = DBMutableCollector()
    val write = CalculationCollectionWrite("CollectionEncoder - $t", cache)

    suspend fun insertionResult(descriptor: SerialDescriptor): InsertionResult {
        write.join()
        logger.trace { "insertResult: $descriptor" }
        return InsertionResult(collection.done().rows, descriptor.tableDescriptor())
    }

    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        println("addValue: $descriptor, $index, $value")
    }

    override fun values(): List<DBEntry> {
        return emptyList()
    }

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        println("encodeSubInstance $serializersModule, $descriptor, $index, $serializer, $value, $isCollection")
    }

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
    }
}