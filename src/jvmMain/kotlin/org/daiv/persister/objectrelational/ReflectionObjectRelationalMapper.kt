package org.daiv.persister.objectrelational

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.daiv.coroutines.CalculationCollection
import org.daiv.coroutines.CalculationSuspendableMap
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class ReflectionObjectRelationalMapper {
}

data class ClassObjectRelationalMapper<T : Any>(val clazz: KClass<T>, val objectRelationalMapper: ObjectRelationalMapper<T>) {
}

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

fun <T : Any> KClass<T>.objectRelationMapper(): ClassObjectRelationalMapper<T> {
    class CalculationMap() {
        val calculationCollection = CalculationSuspendableMap<KClass<*>, ClassObjectRelationalMapper<*>>("") { it.objectRelationMapper() }
        suspend fun <T : Any> getValue(clazz: KClass<T>): ClassObjectRelationalMapper<T> {
            return calculationCollection.getValue(clazz) as ClassObjectRelationalMapper<T>
        }
    }

    val map = CalculationMap()

    val ret = ClassObjectRelationalMapper(this, object : ObjectRelationalMapper<T> {
        fun <T : Any> Collection<KProperty1<T, *>>.noCollectionMembers() = filter { !it.returnType.typeName().isCollection() }

        override fun hashCodeX(t: T): Int {
            TODO("Not yet implemented")
        }

        suspend fun <R, T : Any> KProperty1<R, T>.toMapper(map: CalculationMap, clazz: KClass<T>) =
            PropertyMapper(this, map.getValue(clazz).objectRelationalMapper)

        val noNative = runBlocking {
            declaredMemberProperties.map { it to it.returnType.type<Any>() }
                .filter { !it.second.simpleName.isNative() && !it.second.simpleName.isCollection() }.asFlow()
                .map { (it as KProperty1<T, Any>).toMapper(map, it.second) }.toList()
        }

        override val objectRelationalHeader: ObjectRelationalHeader by lazy {
            val keys = declaredMemberProperties.map {
                val typeName = it.returnType.typeName()!!
                val x = when {
                    typeName.isNative() -> listOf(HeadEntry(it.name, typeName, true))
                    typeName.isCollection() -> emptyList()
                    else -> typeName.headValue(runBlocking { map.getValue(it.returnType.type()).objectRelationalMapper })
                }
                x
            }.flatten()
            ObjectRelationalHeaderData(keys, keys, noNative.map { it.mapper.objectRelationalHeader })
        }

        override val objectRelationalWriter: ObjectRelationalWriter<T> by lazy {
            val keys = declaredMemberProperties.noCollectionMembers().toWriteEntry(true)
            val others = declaredMemberProperties.noCollectionMembers().toWriteEntry(false)

            ObjectRelationalWriterData(keys, others, noNative.map {
                it.writerMap()
            })
        }

        override val objectRelationalReader: ObjectRelationalReader<T> by lazy {
            val keys = declaredMemberProperties.noCollectionMembers().map { ReadEntryTask("") }
            ObjectRelationalReaderData("", keys, listOf())
        }

    })
    map[this] = ret
    return ret
}

