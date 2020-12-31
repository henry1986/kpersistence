package org.daiv.persister.table

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.ClassKey
import org.daiv.persister.collector.CollectedValue
import org.daiv.persister.collector.KeyCollector




class InsertionClassCache {
    private val map = mutableMapOf<ClassKey, InsertionResult<*>>()

    fun getInsertionValue(key: ClassKey): InsertionResult<*>? {
        return map.get(key)
    }

    fun setInsertionResult(key: ClassKey, insertionResult: InsertionResult<*>) {
        map[key] = insertionResult
    }

    fun all() = map.values.toList()
}


class InsertionCache {
    private val map = mutableMapOf<KSerializer<*>, InsertionClassCache>()

    fun get(key: ClassKey, serializer: KSerializer<*>): InsertionResult<*>? {
        return map[serializer]?.getInsertionValue(key)
    }

    fun get(serializer: KSerializer<*>): InsertionClassCache? {
        return map[serializer]
    }

    fun <T : Any> set(key: ClassKey, insertionResult: InsertionResult<T>) {
        (map[insertionResult.serializer] ?: InsertionClassCache().also {
            map[insertionResult.serializer] = it
        }).let {
            it.setInsertionResult(key, insertionResult)
        }
    }

    fun all() = map.values.toList()
}

class ElementObjectCache(val serializer: SerializationStrategy<*>) {
    private val map = mutableMapOf<ClassKey, Any?>()

    operator fun set(key: ClassKey, any: Any) {
        map[key] = any
    }

    fun exists(key: ClassKey) = map.containsKey(key)

    operator fun get(key: List<Any?>) = map[key]

    fun all() = map.toList()
}

class ElementCache {
    private val map = mutableMapOf<SerializationStrategy<*>, ElementObjectCache>()

    fun exists(serializer: SerializationStrategy<*>, key: ClassKey): Boolean {
        return map[serializer]?.exists(key) ?: false
    }

    fun set(serializer: SerializationStrategy<*>, key: ClassKey, any: Any) {
        (map[serializer] ?: ElementObjectCache(serializer).also {
            map[serializer] = it
        }).let {
            it.set(key, any)
        }
    }

    fun all() = map.values.toList()
}
