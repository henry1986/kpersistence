package org.daiv.persister.objectrelational

import org.daiv.coroutines.CalculationSuspendableMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible


data class PropertyMapper<R, T>(val p: KProperty1<R, T>, val mapper: ObjectRelationalMapper<T>) {
    private fun KProperty1<R, T>.writerMap(c: ObjectRelationalMapper<T>): ObjectRelationalWriterMap<R, T> =
        ObjectRelationalWriterMap(c.objectRelationalWriter) {
            isAccessible = true
            val r = get(this)
            r
        }

    fun writerMap() = p.writerMap(mapper)
}

fun <T : Any> Collection<KProperty1<T, *>>.toWriteEntry(isKey: Boolean) = map {
    DefaultPreWriteEntry<T>(it.name, isKey) {
        it.isAccessible = true
        it.get(this)
    }
}

fun <T : Any> KType.type() = classifier as KClass<T>
fun KType.typeName() = type<Any>().simpleName

class CalculationMap() {
    val calculationCollection = CalculationSuspendableMap<KClass<*>, CORM<*>>("") { it.objectRelationMapper() }
    suspend fun <T : Any> getValue(clazz: KClass<T>): CORM<T> {
        return calculationCollection.getValue(clazz) as CORM<T>
    }
}
