package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeys
import org.daiv.persister.MoreKeysData
import org.daiv.persister.table.default
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

interface JavaParseable<T : Any> : ClassParseable {
    val noNative: List<JParameter>
    val propertyMapper: List<PropertyMapper<T, Any?>>
    fun <T : Any> Collection<KParameter>.toWriteEntry(clazz: KClass<T>, isKey: Boolean): List<PreWriteEntry<T>> =
        mapNotNull { parameter ->
            val prop: KProperty1<T, *> = clazz.declaredMemberProperties.find { it.name == parameter.name }!!
            val typeName = prop.returnType.typeName()
            val propGetter: T.() -> Any? = {
                prop.isAccessible = true
                prop.get(this)
            }
            when {
                typeName.isNative() -> listOf(DefaultPreWriteEntry(parameter.name!!, isKey, propGetter))
                !typeName.isCollection() ->
                    propertyMapper.find { it.p == prop }!!.writer.preWriteKey(
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

interface ParameterBuilder : ClassParseable {
    fun <T : Any> propertyGetter(prop: KProperty1<T, *>): T.() -> Any? {
        val propGetter: T.() -> Any? = {
            prop.isAccessible = true
            prop.get(this)
        }
        return propGetter
    }
}

interface JClassHeaderData : ClassHeaderData {
    override val parameters: List<PParameter>
    override val keyParameters: List<PParameter>
    override val otherParameters: List<PParameter>
}

class DefaultJClassHeaderData private constructor(
    override val parameters: List<PParameter>,
    private val classHeaderData: ClassHeaderData
) : JClassHeaderData, ClassHeaderData by classHeaderData {
    override val keyParameters: List<PParameter> = classHeaderData.keyParameters as List<PParameter>
    override val otherParameters: List<PParameter> = classHeaderData.otherParameters as List<PParameter>

    companion object : ClassParseable {
        fun toParameters(clazz: KClass<*>, cormMap: CormMap): JClassHeaderData {
            val chdMap = cormMap.chdMap
            val moreKeys: MoreKeysData = clazz.findAnnotation<MoreKeys>().default()
            val x = clazz.primaryConstructor!!.parameters.mapIndexed { i, it ->
                when {
                    !it.type.typeName().isCollection() ->
                        SimpleJParameter.fromKParameter(
                            clazz,
                            it,
                            KeyType.keyType(i, moreKeys),
                            chdMap
                        )
                    it.type.typeName().isList() || it.type.typeName().isSet() -> {
                        JParameterWithOneGeneric.fromKParameter(clazz, it, KeyType.keyType(i, moreKeys), chdMap)
                    }
                    it.type.typeName().isMap() ->
                        JParameterWithTwoGenerics.fromKParameter(clazz, it, KeyType.keyType(i, moreKeys), chdMap)
                    else -> throw RuntimeException("unknown type: $it")
                }
            }
            val t = x.map {
                val prop = clazz.declaredMemberProperties.find { parameter -> it.name == parameter.name }!!
                DefaultPParameter(it.buildPropertyMapper(prop, cormMap), it)
            }
            return DefaultJClassHeaderData(t, DefaultClassHeaderData(clazz, t, moreKeys))
        }
    }
}

interface PParameter : Parameter {
    val propertyMapper: PropertyMapper<*, *>
    fun <T> toWriteEntry(): List<PreWriteEntry<T>>
}

class SimplePParameter(override val propertyMapper: PropertyMapper<*, *>, val parameter: JParameter) : PParameter,
    Parameter by parameter {
    override fun <T> toWriteEntry(): List<PreWriteEntry<T>> {
        return if (isNative()) {
            listOf(DefaultPreWriteEntry(listOf(this), keyType.isKey, propGetter))
        } else {

        }
    }
}

class GenericPParameter(override val propertyMapper: PropertyMapper<*, *>, val parameter: JParameter) : PParameter,
    Parameter by parameter {
    override fun <T> toWriteEntry(): List<PreWriteEntry<T>> {
        return emptyList()
    }
}


interface JParameter : Parameter {
    fun <T : Any> buildPropertyMapper(it: KProperty1<T, *>, cormMap: CormMap): PropertyMapper<T, Any?>
}

class SimpleJParameter(private val simpleParameter: SimpleParameter) : JParameter, Parameter by simpleParameter {
    override fun <T : Any> buildPropertyMapper(it: KProperty1<T, *>, cormMap: CormMap): PropertyMapper<T, Any?> {
        val p = PropertyMapper(this, it) {
            val value = cormMap.getValue(type.utype())
            value.objectRelationalWriter as RowWriter<Any?>
        }
        return p
    }

    override fun <T> toWriteEntry(): List<PreWriteEntry<T>> {
        return listOf(DefaultPreWriteEntry(listOf(this), keyType.isKey, propGetter))
    }

    companion object {
        fun fromKParameter(
            receiverClass: KClass<*>,
            parameter: KParameter,
            isKey: KeyType,
            chdMap: CHDMap<JClassHeaderData>
        ): JParameter {
            return SimpleJParameter(
                SimpleParameter(
                    receiverClass,
                    parameter.name!!,
                    parameter.type,
                    isKey,
                    chdMap,
                )
            )
        }
    }
}

class JParameterWithOneGeneric(val generic: ParameterWithGeneric) : JParameter, Parameter by generic {

    override fun <T : Any> buildPropertyMapper(it: KProperty1<T, *>, cormMap: CormMap): PropertyMapper<T, Any?> {
        it as KProperty1<T, List<out Any>>
        return PropertyMapper(this, it) {
            val value = cormMap.getValue(genericNotNativeType().first().type()).objectRelationalWriter
            if (type.typeName().isList()) {
                val x: RowWriter<Any?> = ListObjectWriter { value } as RowWriter<Any?>
                x
            } else {
                val s = SetObjectWriter { value }
                val x: RowWriter<Any?> = s as RowWriter<Any?>
                x
            }
        }
    }

    companion object {
        fun fromKParameter(
            receiverClass: KClass<*>,
            parameter: KParameter,
            keyType: KeyType,
            chdMap: CHDMap<JClassHeaderData>
        ): JParameter {
            return JParameterWithOneGeneric(
                ParameterWithOneGeneric(
                    receiverClass,
                    parameter.name!!,
                    parameter.type,
                    keyType,
                    chdMap,
                    parameter.type.arguments[0].type!!,
                )
            )
        }
    }
}


class JParameterWithTwoGenerics(val generic: ParameterWithTwoGenerics) : JParameter, Parameter by generic {

    override fun <T : Any> buildPropertyMapper(it: KProperty1<T, *>, cormMap: CormMap): PropertyMapper<T, Any?> {
        TODO("Not yet implemented")
    }

    companion object {
        fun fromKParameter(
            receiverClass: KClass<*>,
            parameter: KParameter,
            keyType: KeyType,
            chdMap: CHDMap<JClassHeaderData>
        ): JParameter {
            return JParameterWithTwoGenerics(
                ParameterWithTwoGenerics(
                    receiverClass,
                    parameter.name!!,
                    parameter.type,
                    keyType,
                    chdMap,
                    parameter.type.arguments[0].type!!,
                    parameter.type.arguments[1].type!!,
                )
            )
        }
    }
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
    override val classHeaderData: JClassHeaderData,
    override val noNative: List<Parameter>,
    override val propertyMapper: List<PropertyMapper<T, Any?>>,
    val keys: Map<KClass<*>, () -> CORM<out Any>>,

    ) : ObjectRelationalMapper<T>, JavaParseable<T>, ClassParameter<T> by classParameter {

    fun Collection<KParameter>.noCollectionMembers() =
        filter { !it.type.typeName().isCollection() }

    override fun hashCodeX(t: T): Int {
        TODO("Not yet implemented")
    }

    override val objectRelationalHeader: ObjectRelationalHeader by lazy {
        val all = classHeaderData.parameters.mapIndexed { i, it -> it.headEntry(i, moreKeys, keys) }.flatten()
        val map = all.groupBy { it.isKey }
        val keyEntries = map[true] ?: emptyList()
        val collectionParamaters = classParameter.parameters.filter { it.type.typeName().isCollection() }
        val collectionValues =
            classHeaderData.parameters.flatMap { it.genericNotNativeType() }
//        val x = collectionParamaters.map { { listHeader(keyEntries) } }
        val noNativeHeaders = noNative.map {
            { keys[it.type.utype()]?.invoke()?.objectRelationalHeader!! }
        }
        ObjectRelationalHeaderData(
            keyEntries,
            map[false] ?: emptyList(),
            noNativeHeaders
        )
    }

    override val objectRelationalWriter: ObjectRelationalWriterData<T> by lazy {
//        fun List<IndexedValue<KParameter>>.withoutIndex() = map { it.value }
//        fun Map<Boolean, List<IndexedValue<KParameter>>>.toWriteEntry(isKey: Boolean) =
//            get(isKey)?.withoutIndex()?.noCollectionMembers()?.toWriteEntry(clazz, isKey) ?: emptyList()


        val keys = classHeaderData.keyParameters.map { it.toWriteEntry() }
        val others = classHeaderData.keyParameters.map { it.toWriteEntry() }

        println("no native for $clazz: $noNative")

        ObjectRelationalWriterData(keys, others, noNative.map {
            val t = propertyMapper.find { p -> it == p.parameter }!!
            t.writerMap()
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
                    keys[type]?.let {
                        val corm = it().objectRelationalReader
                        val key = corm.readKey(this)
                        val data = dataRequester.requestData(key, corm)
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
        ObjectRelationalReaderData(clazz, keys, others, builder)
    }
}

suspend fun <T : Any> KClass<T>.objectRelationMapper(map: CormMap): CORM<T> {
    val classHeaderData = map.chdMap.getValue(this)
    println("get mapper for: $this")
    val noNative = map.createNoNative(this)
    println("2 get mapper for: $this")
    val classParameter = ClassParameterImpl(this)
    println("3 get mapper for: $this")
    val keys = map.createKeys(classParameter)
    println("4 get mapper for: $this")
    return CORM(
        classParameter,
        classHeaderData,
        noNative,
        keys
    )
}

