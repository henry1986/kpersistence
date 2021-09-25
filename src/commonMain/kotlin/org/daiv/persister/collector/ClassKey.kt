package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder

enum class KeyType {
    DEFAULT
}

interface ClassKey {
    val isCollection: Boolean
    fun collectedValues(): List<DBEntry>
}

data class ClassKeyImpl(val type: KeyType, val list: List<DBEntry>, override val isCollection: Boolean) : ClassKey {

    override fun collectedValues(): List<DBEntry> {
        return list
    }
}

fun <T : Any> SerializationStrategy<T>.getKeys(any: T): ClassKey {
    val module = SerializersModule { }
    val collector = DBMutableCollector()
    val encoder = PEncoder(module) { KeyCollector(descriptor, null, collector) }
    serialize(encoder, any)
    return ClassKeyImpl(KeyType.DEFAULT, collector.entries(), false)
}
