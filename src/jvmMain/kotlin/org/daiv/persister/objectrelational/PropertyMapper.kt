package org.daiv.persister.objectrelational

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.coroutines.ScopeContextable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
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

fun <T : Any> KType.type() = classifier as KClass<T>
fun KType.utype() = classifier as KClass<*>
fun KType.typeName() = type<Any>().simpleName

class CHDMap(val scopeContextable: ScopeContextable = DefaultScopeContextable()) : ClassParseable {
    val calculationCollection: CalculationSuspendableMap<KClass<*>, ClassHeaderData> =
        CalculationSuspendableMap<KClass<*>, ClassHeaderData>("") {
            val header = ClassHeaderData.toParameters(it, this)
            header.dependentClasses().map { launch(it) }
            header
        }

    private fun launch(clazz: KClass<*>) {
        calculationCollection.launchOnNotExistence(clazz) {}
    }

    suspend fun getValue(clazz: KClass<*>): ClassHeaderData {
        return calculationCollection.getValue(clazz)
    }

    suspend fun getAndJoin(clazz: KClass<*>): ClassHeaderData {
        val value = getValue(clazz)
        join()
        return value
    }

    suspend fun join() {
        calculationCollection.join()
    }

    fun directGet(clazz: KClass<*>): ClassHeaderData {
        return calculationCollection.tryDirectGet(clazz)
            ?: throw NullPointerException("for class $clazz is no classHeaderData created")
    }
}

