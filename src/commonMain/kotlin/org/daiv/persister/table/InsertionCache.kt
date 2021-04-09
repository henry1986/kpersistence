package org.daiv.persister.table

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import org.daiv.coroutines.*
import org.daiv.persister.collector.ClassKey


class InsertionCache(val scopeContextable: ScopeContextable) {
    private val actor = ActorableInterface(scopeContextable)
    private val map = mutableMapOf<SerialDescriptor, CalculationSuspendableMap<*, InsertionResult>>()

    private inner class Insert(val o: Any, val serialDescriptor: SerialDescriptor, val valueCreation: suspend (Any) -> InsertionResult) :
        ActorRunnable {
        override suspend fun run() {
            val x = tryDirectGet(serialDescriptor) ?: run {
                val calc = CalculationSuspendableMap(scopeContextable, valueCreation)
                map[serialDescriptor] = calc as CalculationSuspendableMap<*, InsertionResult>
                calc
            }
            x as CalculationSuspendableMap<Any, InsertionResult>
            x.launchOnNotExistence(o) {}
        }

    }

    private fun tryDirectGet(serialDescriptor: SerialDescriptor): CalculationSuspendableMap<*, InsertionResult>? {
        return map[serialDescriptor]
    }

    fun insert(o: Any, serialDescriptor: SerialDescriptor, valueCreation: suspend (Any) -> InsertionResult) {
        val calc = tryDirectGet(serialDescriptor)
        if(calc == null){
            actor.runEvent(Insert(o, serialDescriptor, valueCreation))
        } else {
            calc as CalculationSuspendableMap<Any, InsertionResult>
            calc.launchOnNotExistence(o){}
        }
    }


    suspend fun join() {
        actor.waitOnDone {
            map.values.forEach { it.join() }
        }
    }

    fun all() = map.values.toList()
}

class ElementObjectCache(val cacheKey: CacheKey) {
    private val map = mutableMapOf<ClassKey, Any?>()

    operator fun set(key: ClassKey, any: Any) {
        map[key] = any
    }

    fun exists(key: ClassKey) = map.containsKey(key)

    operator fun get(key: List<Any?>) = map[key]

    fun all() = map.toList()
}

data class DefaultCacheKey(override val serializer: SerializationStrategy<*>) : CacheKey
data class CollectionCacheKey(override val serializer: SerializationStrategy<*>, val mainKeyDescriptor: SerialDescriptor) :
    CacheKey

interface CacheKey {
    val serializer: SerializationStrategy<*>
}

class ElementCache {
    private val map = mutableMapOf<CacheKey, ElementObjectCache>()

    fun exists(serializer: CacheKey, key: ClassKey): Boolean {
        return map[serializer]?.exists(key) ?: false
    }

    fun set(serializer: CacheKey, key: ClassKey, any: Any) {
        (map[serializer] ?: ElementObjectCache(serializer).also {
            map[serializer] = it
        }).let {
            it.set(key, any)
        }
    }

    fun all() = map.values.toList()
}
