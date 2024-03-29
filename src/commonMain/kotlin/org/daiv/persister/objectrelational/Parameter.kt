package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeysData
import org.daiv.persister.objectrelational.TestXZ.Companion.headValue
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun KType.utype() = classifier as KClass<*>
fun <T : Any> KType.type() = classifier as KClass<T>
fun KType.typeName() = type<Any>().simpleName

enum class KeyType {
    NO_KEY, NORM;

    val isKey: Boolean
        get() = this != NO_KEY

    companion object {
        fun keyType(index: Int, moreKeysData: MoreKeysData): KeyType {
            return if (index < moreKeysData.amount) NORM else NO_KEY
        }
    }
}

interface Parameter : ClassParseable, PrefixBuilder {
    val receiverClass: KClass<*>
    val name: String
    val type: KType
    val keyType: KeyType
    val chdMap: CHDMap
    val typeName: String

    fun isNative(): Boolean = type.typeName().isNative()
    fun isList(): Boolean = type.typeName().isList()
    fun isMap(): Boolean = type.typeName().isMap()
    fun isSet(): Boolean = type.typeName().isSet()
    fun isSetOrList(): Boolean = isSet() || isList()
    fun isCollection(): Boolean = type.typeName().isCollection()

    fun headEntry(
        index: Int,
        moreKeys: MoreKeysData,
        keys: Map<KClass<*>, () -> ObjectRelationalMapper<out Any>>
    ): List<HeadEntry>

    fun genericNotNativeType(): List<KType>
    fun dependentClasses(): List<KClass<*>>
    fun head(prefix: String?, isKey: Boolean, parameter: List<Parameter>): List<HeadEntry>
    fun runEquals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SimpleParameter

        if (receiverClass != other.receiverClass) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (keyType != other.keyType) return false

        return true
    }

    fun runHashCode(): Int {
        var result = receiverClass.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + keyType.hashCode()
        return result
    }
}

class SimpleParameter(
    override val receiverClass: KClass<*>,
    override val name: String,
    override val type: KType,
    override val keyType: KeyType,
    override val chdMap: CHDMap
) : Parameter {
    override val typeName: String = type.typeName()!!

    override fun headEntry(
        index: Int,
        moreKeys: MoreKeysData,
        keys: Map<KClass<*>, () -> ObjectRelationalMapper<out Any>>
    ): List<HeadEntry> {
        if (isNative()) {
            return listOf(HeadEntry(this, name, typeName, moreKeys.amount > index))
        } else {
            return name.headValue(keys[type.utype()]?.invoke()?.objectRelationalHeader!!, this)
        }
    }

    override fun genericNotNativeType(): List<KType> = emptyList()

    override fun dependentClasses() = if (isNative()) emptyList() else listOf(type.utype())
    override fun head(prefix: String?, isKey: Boolean, parameters: List<Parameter>): List<HeadEntry> {
        val prefixedName = prefix.build(name)
        return if (isNative()) {
            listOf(HeadEntry(listOf(this) + parameters, prefixedName, type.typeName()!!, isKey))
        } else {
            chdMap.directGet(type.utype()).keyHead(prefixedName, isKey, listOf(this) + parameters)
        }
    }

    override fun toString(): String {
        return "${receiverClass.simpleName}::$name ${type.typeName()} $keyType"
    }

    override fun equals(other: Any?) = runEquals(other)
    override fun hashCode() = runHashCode()
}

interface ParameterWithGeneric:Parameter{
    override fun headEntry(
        index: Int,
        moreKeys: MoreKeysData,
        keys: Map<KClass<*>, () -> ObjectRelationalMapper<out Any>>
    ): List<HeadEntry> {
        return emptyList()
    }


    override fun head(prefix: String?, isKey: Boolean, parameters: List<Parameter>): List<HeadEntry> {
        return emptyList()
    }
    override fun dependentClasses(): List<KClass<*>> = genericNotNativeType().map { it.utype() }

}

class ParameterWithOneGeneric(
    override val receiverClass: KClass<*>,
    override val name: String,
    override val type: KType,
    override val keyType: KeyType,
    override val chdMap: CHDMap,
    val genericType: KType,
) : ParameterWithGeneric {

    override val typeName: String = type.typeName()!!

    fun isGenericNative() = genericType.typeName().isNative()

    override fun genericNotNativeType(): List<KType> =
        if (isGenericNative()) emptyList() else listOf(genericType)

    override fun equals(other: Any?) = runEquals(other)

    override fun hashCode() = runHashCode()
}

class ParameterWithTwoGenerics(
    override val receiverClass: KClass<*>,
    override val name: String,
    override val type: KType,
    override val keyType: KeyType,
    override val chdMap: CHDMap,
    val genericType: KType,
    val genericType2: KType,
) : ParameterWithGeneric {

    override val typeName: String = type.typeName()!!

    fun isGenericNative() = genericType.typeName().isNative()
    fun isGeneric2Native() = genericType.typeName().isNative()

    fun genericNotNative1() = if (isGenericNative()) null else genericType
    fun genericNotNative2() = if (isGeneric2Native()) null else genericType2

    override fun genericNotNativeType() = listOfNotNull(genericNotNative1(), genericNotNative2())

    override fun equals(other: Any?) = runEquals(other)

    override fun hashCode() = runHashCode()
}
