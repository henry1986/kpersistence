package org.daiv.persister.table

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.DBEntry
import org.daiv.persister.collector.DBMutableCollector
import org.daiv.persister.collector.DataCollector
import org.daiv.persister.collector.ElementAdder

data class InsertionResult(val values: List<DBEntry>, val serialDescriptor: SerialDescriptor) {
    fun head(): List<String> {
        return if (values.isEmpty()) {
            serialDescriptor.getNativeDescriptorNames()
        } else {
            values.map { it.name }
        }
    }

    fun toValues() = values.map { it.value }
}


fun <T> SerializationStrategy<T>.insertObject(readCache: InsertionCache, o: T) {
    readCache.insert(o as Any, descriptor) { current ->
//        println("here")
        val parent = o is Collection<*>
        val collectedValues = DBMutableCollector()
        val encoder = PEncoder(SerializersModule { }) {  DataCollector(collectedValues, descriptor, InsertElementAdder(readCache), false, null, parent, it)}
        serialize(encoder, current as T)
        InsertionResult(collectedValues.done().toEntries(), descriptor)
    }
}

class InsertElementAdder(val readCache: InsertionCache) : ElementAdder {
    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        serializer.insertObject(readCache, value)
    }
}

fun List<InsertionResult>.toInsertCommand(): String {
    val values = joinToString(", ") { it.toValues().joinToString(", ", "(", ")") }
    val f = first()
    val names = f.serialDescriptor.getNativeDescriptorNames()
    return "INSERT INTO `${f.serialDescriptor.tableName()}` (${names.joinToString(", ")}) VALUES $values;"
}

class InsertTable(val readCache: InsertionCache) {


    suspend fun persistCache(): List<String> {
        readCache.join()
        return readCache.all().mapNotNull { cache ->
            val x = cache.all()
            if (x.isEmpty()) {
                null
            } else {
                x.toInsertCommand()
            }
        }
//        val x = readCache.get(serializer)?.all() ?: emptyList()
//        val names = if (x.isEmpty()) {
//            serializer.descriptor.getNativeDescriptorNames()
//        } else {
//            x.first().head()
//        }
//        val values = x.joinToString(", ") { it.toValues().joinToString(", ", "(", ")") }
//        val insert = "INSERT INTO ${serializer.descriptor.tableName()} (${names.joinToString(", ")}) VALUES $values;"
//        return insert
    }

    fun <T : Any> insert(list: List<T>, serializer: KSerializer<T>) {
        list.forEach {
            serializer.insertObject(readCache, it)
        }
    }
}

