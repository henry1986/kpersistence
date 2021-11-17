package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeys
import org.daiv.persister.MoreKeysData
import org.daiv.persister.table.default
import org.daiv.persister.table.prefix
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

interface JavaParseable<T : Any> : ClassParseable {
    val noNative: List<PropertyMapper<T, Any?>>
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

interface ParameterBuilder : ClassParseable {
    fun <T : Any> propertyGetter(prop: KProperty1<T, *>): T.() -> Any? {
        val propGetter: T.() -> Any? = {
            prop.isAccessible = true
            prop.get(this)
        }
        return propGetter
    }
}

interface Parameter : ClassParseable {
    val name: String
    val type: KType
    fun isNative(): Boolean = type.typeName().isNative()
    fun isList() = type.typeName().isList()
    fun isMap() = type.typeName().isMap()
    fun isSet() = type.typeName().isSet()
    fun isSetOrList() = isSet() || isList()
    fun isCollection() = type.typeName().isCollection()
    val propGetter: Any.() -> Any?

    fun genericNotNativeType(): List<KType>
}

data class ClassHeaderData(val clazz: KClass<*>, val parameters: List<Parameter>) {
    val moreKeys = clazz.findAnnotation<MoreKeys>().default()
    val keyParameters = parameters.take(moreKeys.amount)
    val otherParameters = parameters.drop(moreKeys.amount)

    companion object : ClassParseable {
        fun toParameters(clazz: KClass<*>): ClassHeaderData {
            val x = clazz.primaryConstructor!!.parameters.map {
                val prop = clazz.declaredMemberProperties.find { parameter -> it.name == parameter.name }!!
                when {
                    !it.type.typeName().isCollection() -> SimpleParameter.fromKParameter(it, prop)
                    it.type.typeName().isList() || it.type.typeName().isSet() -> {
                        ParameterWithOneGeneric.fromKParameter(it, prop)
                    }
                    it.type.typeName().isMap() -> ParameterWithTwoGenerics.fromKParameter(it, prop)
                    else -> throw RuntimeException("unknown type: $it")
                }
            }
            return ClassHeaderData(clazz, x)
        }
    }
}

data class SimpleParameter(
    override val name: String,
    override val type: KType,
    override val propGetter: Any.() -> Any?
) : Parameter, PrefixBuilder {

    fun headEntry(prefix: String?, isKey: Boolean) = HeadEntry(name.prefix(prefix), type.typeName()!!, isKey)

    override fun genericNotNativeType(): List<KType> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SimpleParameter

        if (name != other.name) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    companion object : ParameterBuilder {
        fun <T : Any> fromKParameter(parameter: KParameter, prop: KProperty1<T, *>): SimpleParameter {
            return SimpleParameter(parameter.name!!, parameter.type, propertyGetter(prop) as Any.() -> Any?)
        }
    }

}

data class ParameterWithOneGeneric(
    override val name: String,
    override val type: KType,
    val genericType: KType,
    override val propGetter: Any.() -> Any?
) : Parameter {

    fun isGenericNative() = genericType.typeName().isNative()

    override fun genericNotNativeType(): List<KType> =
        if (isGenericNative()) emptyList() else listOf(genericType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParameterWithOneGeneric

        if (name != other.name) return false
        if (type != other.type) return false
        if (genericType != other.genericType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + genericType.hashCode()
        return result
    }

    companion object : ParameterBuilder {
        fun <T : Any> fromKParameter(parameter: KParameter, prop: KProperty1<T, *>): ParameterWithOneGeneric {
            return ParameterWithOneGeneric(
                parameter.name!!,
                parameter.type,
                parameter.type.arguments[0].type!!,
                propertyGetter(prop) as Any.() -> Any?
            )
        }
    }
}

data class ParameterWithTwoGenerics(
    override val name: String,
    override val type: KType,
    val genericType: KType,
    val genericType2: KType,
    override val propGetter: Any.() -> Any?
) : Parameter {

    fun isGenericNative() = genericType.typeName().isNative()
    fun isGeneric2Native() = genericType.typeName().isNative()

    fun genericNotNative1() = if (isGenericNative()) null else genericType
    fun genericNotNative2() = if (isGeneric2Native()) null else genericType2

    override fun genericNotNativeType() = listOfNotNull(genericNotNative1(), genericNotNative2())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParameterWithTwoGenerics

        if (name != other.name) return false
        if (type != other.type) return false
        if (genericType != other.genericType) return false
        if (genericType2 != other.genericType2) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + genericType.hashCode()
        result = 31 * result + genericType2.hashCode()
        return result
    }

    companion object : ParameterBuilder {
        fun <T : Any> fromKParameter(parameter: KParameter, prop: KProperty1<T, *>): ParameterWithTwoGenerics {
            return ParameterWithTwoGenerics(
                parameter.name!!,
                parameter.type,
                parameter.type.arguments[0].type!!,
                parameter.type.arguments[1].type!!,
                propertyGetter(prop) as Any.() -> Any?
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
    val classHeaderData: ClassHeaderData,
    override val noNative: List<PropertyMapper<T, Any?>>,
    val keys: Map<KClass<*>, () -> CORM<out Any>>,

    ) : ObjectRelationalMapper<T>, JavaParseable<T>, ClassParameter<T> by classParameter {
    fun Collection<KParameter>.noCollectionMembers() =
        filter { !it.type.typeName().isCollection() }

    override fun hashCodeX(t: T): Int {
        TODO("Not yet implemented")
    }

    override val objectRelationalHeader: ObjectRelationalHeader by lazy {
        val all = classHeaderData.parameters.mapIndexed { i, it ->
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
        val collectionValues =
            classHeaderData.parameters.flatMap { it.genericNotNativeType() }
//        val x = collectionParamaters.map { { listHeader(keyEntries) } }
        ObjectRelationalHeaderData(
            keyEntries,
            map[false] ?: emptyList(),
            noNative.map { { it.mapper.objectRelationalHeader } })
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

