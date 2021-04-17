package org.daiv.persister.table

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.collector.*

interface TableDescriptor {
    fun tableName(): String
    fun tableHeader(): List<String>
}

data class InsertionResult(val values: List<DBRow>, val tableDescriptor: TableDescriptor) : TableDescriptor by tableDescriptor {
    fun toValues() = values.joinToString(", ") { it.entries.map { it.value }.joinToString(", ", "(", ")") }
}

data class SerialTableDescriptor(val serialDescriptor: SerialDescriptor) : TableDescriptor {
    override fun tableName() = serialDescriptor.tableName()
    override fun tableHeader() = serialDescriptor.getNativeDescriptorNames()
}

fun SerialDescriptor.tableDescriptor() = SerialTableDescriptor(this)

fun dataCollectorEncoder(
    collectedValues: DBMutableCollector,
    descriptor: SerialDescriptor,
    elementAdder: ElementAdder,
    module: SerializersModule,
    parent: Boolean
) =
    PEncoder(module) {
        DataCollector(
            collectedValues,
            descriptor,
            elementAdder,
            true,
            false,
            null,
            parent,
            it
        )
    }

fun <T> SerializationStrategy<T>.createObject(current: T, elementAdder: ElementAdder, module: SerializersModule): DBCollection {
    val parent = current is Collection<*>
    val collectedValues = DBMutableCollector()
    val encoder = dataCollectorEncoder(collectedValues, descriptor, elementAdder, module, parent)
    serialize(encoder, current)
    return collectedValues.done()
}

fun <X> SerialDescriptor.insertObject(
    insertionCache: InsertionCache,
    o: X,
    func: (X) -> DBCollection
) {
    insertionCache.insert(o, this) { current ->
        InsertionResult(func(current).rows, this.tableDescriptor())
    }
}

fun <T> SerializationStrategy<List<T>>.createListCollection(
    collectionInsertData: CollectionInsertData<T>,
    elementAdder: ElementAdder,
    module: SerializersModule,
): DBCollection {
    val dbCollector = DBMutableCollector { it.add(collectionInsertData.key) }
    val p = PEncoder(
        module,
        ListEncoderStrategyFactory(dbCollector, elementAdder)
    )
    serialize(p, collectionInsertData.o)
    return dbCollector.done()
}

data class CollectionInsertData<T>(val key: DBEntry, val o: List<T>)


class InsertElementAdder(val cache: InsertionCache, val module: SerializersModule) : ElementAdder {
    override fun <T> addElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        addElement(serializer, value)
    }

    fun <T> addList(serializer: SerializationStrategy<List<T>>, value: CollectionInsertData<T>) {
        serializer.descriptor.insertObject(cache, value) {
            serializer.createListCollection(it, this, module)
        }
    }

    fun <T> addElement(serializer: SerializationStrategy<T>, value: T) {
        serializer.descriptor.insertObject(cache, value) {
            serializer.createObject(it, this, module)
        }
    }
}

fun List<InsertionResult>.toInsertCommand(): String? {
    if (isEmpty()) {
        return null
    }
    val values = joinToString(", ") { it.toValues() }
    val f = first()
    val names = f.values.first().entries.map { it.name }
    return "INSERT INTO `${f.tableName()}` (${names.joinToString(", ")}) VALUES $values;"
}

class InsertTable(val readCache: InsertionCache, val module: SerializersModule) {


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
        val adder = InsertElementAdder(readCache, module)
        list.forEach {
            adder.addElement(serializer, it)
            //            serializer.insertObject(readCache, InsertElementAdder(readCache, module), module, it)
        }
    }
}

