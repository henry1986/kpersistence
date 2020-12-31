package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder

enum class KeyType {
    DEFAULT
}

interface ClassKey {
    val isCollection: Boolean
    fun collectedValues(): List<CollectedValue>
}

data class ClassKeyImpl(val type: KeyType, val list: List<CollectedValue>, override val isCollection: Boolean) : ClassKey {

    override fun collectedValues(): List<CollectedValue> {
        return list
    }
}

fun <T : Any> SerializationStrategy<T>.getKeys(any: T): ClassKey {
    val module = SerializersModule { }
    var k: KeyCollector? = null
    val encoder = PEncoder(module) { k = KeyCollector(descriptor, null, it);k!! }
    serialize(encoder, any)
    val keyCollector = k!!
    return ClassKeyImpl(KeyType.DEFAULT, keyCollector.values(), keyCollector.isCollection)
}
