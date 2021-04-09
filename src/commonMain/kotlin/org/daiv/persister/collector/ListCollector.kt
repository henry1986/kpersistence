package org.daiv.persister.collector

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder

class ListSimpleTypeValueAdder(override val collectedValues: DBMutableCollector = DBMutableCollector()) : ValueAdder, ValueCollector, Beginable by collectedValues {

    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
        val row = collectedValues.new()
        row.add(DBEntry("key", Int.serializer().descriptor, index))
        row.add(DBEntry("value", descriptor, value))
    }

    override fun toString(): String {
        return collectedValues.toString()
    }
}

//class ListComplexTypeFilter(override val collectedValues: MutableList<CollectedValue> = mutableListOf()):ValueAdder, ValueCollector{
//    override fun addValue(descriptor: SerialDescriptor, index: Int, value: Any?) {
//        val list = descriptor.elementNames.toList()
//        if (index >= list.size) {
//            throw IndexOutOfBoundsException("index $index to high for list $list")
//        }
//        val name = list[index]
//        return DefaultValueFilter(descriptor, true, name.prefix(prefix), collectedValues)
//    }
//}

class ListEncoderStrategyFactory(
    val collectedValues: DBMutableCollector,
    val elementAdder: ElementAdder,
) : EncoderStrategyFactory {
    override fun build(isCollection: Boolean): EncoderStrategy {
        val l = ListCollector(elementAdder, collectedValues, isCollection)
        return l
    }
}

//fun SerializationStrategy<*>.isSimpleSerializer() = Int.serializer() == this || this == Long.serializer() || this == String.serializer() || Boolean.serializer()

private val simpleSerializers = listOf(
    Int.serializer(),
    Long.serializer(),
    String.serializer(),
    Boolean.serializer(),
    Short.serializer(),
    Byte.serializer(),
    Char.serializer()
)

fun SerializationStrategy<*>.isSimpleSerializer(): Boolean {
    return simpleSerializers.any { this == it }
}

data class ListCollector private constructor(
    val elementAdder: ElementAdder,
    val valueAdder: ListSimpleTypeValueAdder,
    override val isCollection: Boolean
) : EncoderStrategy, ElementAdder by elementAdder, ValueAdder by valueAdder, Beginable by valueAdder {

    constructor(
        elementAdder: ElementAdder,
        collectedValues: DBMutableCollector,
        isCollection: Boolean
    ) : this(elementAdder, ListSimpleTypeValueAdder(collectedValues), isCollection)

    override fun <T> encodeSubInstance(
        serializersModule: SerializersModule,
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T,
        isCollection: Boolean
    ) {
        if (serializer.isSimpleSerializer()) {
            addValue(serializer.descriptor, index, value)
            return
        } else {
            val d = DataCollector(valueAdder.collectedValues, serializer.descriptor, elementAdder, true, null, true, isCollection)
            val p = PEncoder(serializersModule, ObjectEncoderStrategyFactory(descriptor, index, d))
            serializer.serialize(p, value)
        }
//        val p = PEncoder(serializersModule) {
//            DataCollector(DefaultValueFilter(descriptor, true, null, valueAdder.collectedValues), elementAdder, true)
//        }
//        serializer.serialize(
//            p,
//            value
//        )
    }
}
