package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.table.CollectionCacheKey
import org.daiv.persister.table.DefaultCacheKey
import org.daiv.persister.table.ElementCache


class ElementCollector(val elementCache: ElementCache, val parentKey: ClassKey, override val isCollection: Boolean) : EncoderStrategy,
                                                                                                                      Beginable by Beginable.noBegin {
    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {

    }

    init {
        begin()
    }

    override fun values(): List<DBEntry> = emptyList()


    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        fun ClassKey.serialize() =
            serializer.serialize(PEncoder(serializersModule) { isCollection -> ElementCollector(elementCache, this, isCollection) }, value)

        value?.let {
            val key = serializer.getKeys(it)
            val cacheKey = if (key.isCollection) CollectionCacheKey(serializer, descriptor) else DefaultCacheKey(serializer)
            if (key.isCollection) {
                when (value) {
                    is List<*> -> {
                    }
                    is Set<*> -> {
                    }
                    is Map<*, *> -> {
                    }
                }
                key.serialize()
            } else if (!elementCache.exists(cacheKey, key)) {
                elementCache.set(cacheKey, key, value)
                key.serialize()
            }
        }
    }

    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
    }
}
