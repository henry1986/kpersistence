package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.daiv.persister.MoreKeys
import org.daiv.persister.table.default
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

interface JavaParseable<T : Any> : ClassParseable {
    val noNative: List<PropertyMapper<T, Any?>>
    fun <T : Any> Collection<KParameter>.toWriteEntry(clazz: KClass<T>, isKey: Boolean) = mapNotNull { parameter ->
        val prop = clazz.declaredMemberProperties.find { it.name == parameter.name }!!
        val typeName = prop.returnType.typeName()
        val propGetter: T.() -> Any? = {
            prop.isAccessible = true
            prop.get(this)
        }
        when {
            typeName.isNative() -> listOf(DefaultPreWriteEntry(parameter.name!!, isKey, propGetter))
            !typeName.isCollection() ->
                noNative.find { it.p == prop }!!.mapper.objectRelationalWriter.preWriteKey(prop.name!!, propGetter)
            typeName.isCollection() && isKey -> throw RuntimeException("a collection cannot be a key")
            else -> {
                null
            }
        }
    }.flatten()
}

class CORM<T : Any>(val clazz: KClass<T>, override val noNative: List<PropertyMapper<T, Any?>>) :
    ObjectRelationalMapper<T>,
    JavaParseable<T> {
    fun Collection<KParameter>.noCollectionMembers() =
        filter { !it.type.typeName().isCollection() }

    override fun hashCodeX(t: T): Int {
        TODO("Not yet implemented")
    }

    val moreKeys = clazz.findAnnotation<MoreKeys>().default()
    override val objectRelationalHeader: ObjectRelationalHeader by lazy {
        println("member: ${clazz.primaryConstructor?.parameters?.map { it.name }}")
        val all = clazz.primaryConstructor?.parameters?.mapIndexed { i, it ->
            val typeName = it.type.typeName()!!
            val x = when {
                typeName.isNative() -> listOf(HeadEntry(it.name!!, typeName, moreKeys.amount > i))
                typeName.isCollection() -> emptyList()
                else -> it.name!!.headValue(noNative.find { propertyMapper ->
                    propertyMapper.p.returnType.type<Any>() == it.type.type<Any>()
                }!!.mapper)
            }
            x
        }?.flatten()
        val map = all?.groupBy { it.isKey } ?: emptyMap()
        ObjectRelationalHeaderData(
            map[true] ?: emptyList(),
            map[false] ?: emptyList(),
            noNative.map { it.mapper.objectRelationalHeader })
    }

    override val objectRelationalWriter: ObjectRelationalWriterData<T> by lazy {
        fun List<IndexedValue<KParameter>>.withoutIndex() = map { it.value }
        fun Map<Boolean, List<IndexedValue<KParameter>>>.toWriteEntry(isKey: Boolean) =
            get(isKey)?.withoutIndex()?.noCollectionMembers()?.toWriteEntry(clazz, isKey) ?: emptyList()

        val keysBase =
            clazz.primaryConstructor?.parameters?.withIndex()?.groupBy { it.index < moreKeys.amount } ?: emptyMap()

        val keys = keysBase.toWriteEntry(true)
        val others = keysBase.toWriteEntry(false)

        ObjectRelationalWriterData(keys, others, noNative.map {
            it.writerMap()
        })
    }

    fun KClassifier.toNative(nativeReads: NativeReads) = when (this) {
        Int::class -> {
            nativeReads.readInt()
        }
        Double::class -> {
            nativeReads.readDouble()
        }
        String::class -> {
            nativeReads.readString()
        }
        else -> {
            null
        }
    }

    private val parameters = clazz.primaryConstructor?.parameters!!
    private val keyParameters = parameters.take(moreKeys.amount)
    private val otherParameters = parameters.drop(moreKeys.amount)

    override val objectRelationalReader: ObjectRelationalReader<T> by lazy {
        fun Collection<KParameter>.toReadEntryTasks() = noCollectionMembers().map { p ->
            ReadEntryTask(p.name!!) {
                p.type.type<T>().toNative(nativeReads)
            }
        }

        val keys = keyParameters.toReadEntryTasks()
        val others = otherParameters.toReadEntryTasks()
        val builder: ReadMethod.() -> T =
            {
                val array = this.list.map { it.any }.toTypedArray()
                array.forEach {
                    println("it: $it")
                }
                clazz.objectInstance ?: clazz.primaryConstructor!!.call(*array)
            }
        ObjectRelationalReaderData(clazz.simpleName ?: "no class name", keys, others, builder)
    }
}

suspend fun <T : Any> KClass<T>.objectRelationMapper(map: CalculationMap): CORM<T> {
    val noNative = map.createNoNative(this)
    return CORM(this, noNative)
}

