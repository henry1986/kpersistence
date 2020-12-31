package org.daiv.persister.table

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.CollectedValue
import org.daiv.persister.collector.DataCollector
import org.daiv.persister.collector.getKeys

data class InsertionResult<T : Any>(val values: List<CollectedValue>, val serializer: KSerializer<T>) {
    fun head(): List<String> {
        return if (values.isEmpty()) {
            serializer.descriptor.getNativeDescriptorNames()
        } else {
            values.map { it.name }
        }
    }

    fun toValues() = values.map { it.value }
}

fun <T : Any> KSerializer<T>.insertObject(readCache: InsertionCache, o: T) {
    getKeys(o).let { keys ->
        readCache.get(keys, this) ?: run {
            var d: DataCollector? = null
            val encoder = PEncoder(SerializersModule { }) { d = DataCollector(descriptor, false, null, it); d!! }
            serialize(encoder, o)
            val collector = d!!
            readCache.set(keys, InsertionResult(collector.values(), this))
        }
    }
}
//        val key = CacheKey(serializer.descriptor.serialName, ExecutionKey.INSERT_VALUE)
//        if (readCache.get(key) != null) {
//            return
//        }
//

//        val collector = DataCollector(serializer.descriptor, false, null)
//        val encoder = PEncoder(SerializersModule { }, collector)
//        serializer.serialize(encoder, o)
//        return InsertionResult(collector.values(), serializer)
//        readCache.set(key, collector)

class InsertTable(val readCache: InsertionCache) {

    fun persistCache(): List<String> {
        return readCache.all().mapNotNull { cache ->
            val x = cache.all()
            if (x.isEmpty()) {
                null
            } else {
                val values = x.joinToString(", ") { it.toValues().joinToString(", ", "(", ")") }
                val f = x.first()
                val names = f.serializer.descriptor.getNativeDescriptorNames()
                "INSERT INTO ${f.serializer.descriptor.tableName()} (${names.joinToString(", ")}) VALUES $values;"
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

