package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeys
import org.daiv.persister.MoreKeysData
import org.daiv.persister.table.default
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
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
                noNative.find { it.p == prop }!!.mapper.objectRelationalWriter.preWriteKey(
                    prop.name!!,
                    isKey,
                    propGetter
                )
            typeName.isCollection() && isKey -> throw RuntimeException("a collection cannot be a key")
            else -> {
                null
            }
        }
    }.flatten()
}

interface ClassParameter<T : Any> {
    val clazz: KClass<T>
    val parameters: List<KParameter>
    val keyParameters: List<KParameter>
    val otherParameters: List<KParameter>
    val moreKeys: MoreKeysData
}

class ClassParameterImpl<T : Any>(override val clazz: KClass<T>) : ClassParameter<T> {
    override val moreKeys = clazz.findAnnotation<MoreKeys>().default()
    override val parameters = clazz.primaryConstructor?.parameters!!
    override val keyParameters = parameters.take(moreKeys.amount)
    override val otherParameters = parameters.drop(moreKeys.amount)
}

class CORM<T : Any>(
    val classParameter: ClassParameter<T>,
    override val noNative: List<PropertyMapper<T, Any?>>,
    val keys: Map<KClass<*>, CORM<out Any>>,

    ) : ObjectRelationalMapper<T>, JavaParseable<T>, ClassParameter<T> by classParameter {
    fun Collection<KParameter>.noCollectionMembers() =
        filter { !it.type.typeName().isCollection() }

    override fun hashCodeX(t: T): Int {
        TODO("Not yet implemented")
    }

    override val objectRelationalHeader: ObjectRelationalHeader by lazy {
        val all = classParameter.parameters.mapIndexed { i, it ->
            val typeName = it.type.typeName()!!
            val x = when {
                typeName.isNative() -> listOf(HeadEntry(it.name!!, typeName, moreKeys.amount > i))
                typeName.isCollection() -> emptyList()
                else -> it.name!!.headValue(noNative.find { propertyMapper ->
                    propertyMapper.p.returnType.type<Any>() == it.type.type<Any>()
                }!!.mapper)
            }
            x
        }.flatten()
        val map = all.groupBy { it.isKey }
        val keyEntries = map[true] ?: emptyList()
        val collectionParamaters = classParameter.parameters.filter { it.type.typeName().isCollection() }
        val x = collectionParamaters.map { { listHeader(keyEntries, it.type.arguments[0].type) } }
        ObjectRelationalHeaderData(
            keyEntries,
            map[false] ?: emptyList(),
            noNative.map { { it.mapper.objectRelationalHeader } } + x)
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


    private fun readEntryTask(p: KParameter): List<ReadEntryTask> {
        val type = p.type.type<T>()
        return listOf(ReadEntryTask(p.name!!) {
            when (type) {
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
                    keys[type]?.objectRelationalReader?.let {
                        val key = it.readKey(this)
                        val data = dataRequester.requestData(key, it)
                        data
                    }
                }
            }
        })
    }


    override val objectRelationalReader: ObjectRelationalReader<T> by lazy {
        fun Collection<KParameter>.toReadEntryTasks(): List<ReadEntryTask> =
            noCollectionMembers().flatMap { p: KParameter ->
                readEntryTask(p)
            }

        val keys = keyParameters.toReadEntryTasks()
        val others = otherParameters.toReadEntryTasks()
        val builder: ReadMethod.() -> T =
            {
                val array = this.list.map { it.any }.toTypedArray()
                println("clazz: $clazz")
                array.forEach {
                    println("it: $it")
                }
                clazz.objectInstance ?: clazz.primaryConstructor!!.call(*array)
            }
        ObjectRelationalReaderData(clazz.simpleName ?: "no class name", keys, others, builder)
    }
}

suspend fun <T : Any> KClass<T>.objectRelationMapper(map: CalculationMap): CORM<T> {
    println("get mapper for: $this")
    val noNative = map.createNoNative(this)
    println("2 get mapper for: $this")
    val classParameter = ClassParameterImpl(this)
    println("3 get mapper for: $this")
    val keys = map.createKeys(classParameter)
    println("4 get mapper for: $this")
    return CORM(classParameter, noNative, keys)
}

