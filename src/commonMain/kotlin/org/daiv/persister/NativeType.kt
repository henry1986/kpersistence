package org.daiv.persister

import kotlin.reflect.KClassifier

interface TypeNameable {
    val typeName: String
}

interface InsertHeadable {
    fun insertHead(): String
}

interface Nameable {
    val name: String
}

interface NullableElement {
    val isNullable: Boolean
}

interface Headerable {
    fun toHeader(): String
}


enum class NativeType(override val typeName: String) : TypeNameable {
    INT("INT"), STRING("TEXT"), LONG("LONG"), BOOLEAN("INT"), DOUBLE("REAL"), ENUM("STRING")
}


interface DatabaseReaderValueGetter {
    fun getValue(databaseReader: DatabaseReader, counter: Int): Any? {
        return databaseReader.next(counter)
    }
}

interface MapValue<T> : ValueInserter<T>, DatabaseReaderValueGetter
class DefaultValueMapper<LOWERTYPE> : MapValue<LOWERTYPE> {
    override fun insertValue(t: LOWERTYPE?): String {
        return t.toString()
    }
}

fun interface GetValue<HIGHER : Any, LOWER> {
    fun get(higher: HIGHER): LOWER?
}


interface Member {
    val clazz: KClassifier?
    val name: String
    val isMarkedNullable: Boolean

    fun getType(): NativeType? {
        return when (clazz) {
            Int::class -> NativeType.INT
            Long::class -> NativeType.LONG
            String::class -> NativeType.STRING
            Double::class -> NativeType.DOUBLE
            Boolean::class -> NativeType.BOOLEAN
            else -> null
        }
    }

    fun isNative() = getType() != null
}

interface MemberValueGetter<HIGHERCLASS : Any, LOWERTYPE> : Member, GetValue<HIGHERCLASS, LOWERTYPE> {
    fun getLowerMembers(): List<MemberValueGetter<*, *>>
}


inline fun <HIGHERCLASS : Any, reified LOWERTYPE> memberValueGetter(
    name: String,
    isMarkedNullable: Boolean,
    members: List<MemberValueGetter<*, *>> = emptyList(),
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = memberValueGetterCreator(name, isMarkedNullable, members, func).toNativeTypeHandler()

inline fun <HIGHERCLASS : Any, reified LOWERTYPE> memberValueGetterCreator(
    name: String,
    isMarkedNullable: Boolean,
    members: List<MemberValueGetter<*, *>> = emptyList(),
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = NativeTypeMapperCreator(DefaultMemberValueGetter(LOWERTYPE::class, name, isMarkedNullable, members, func))

class DefaultMemberValueGetter<HIGHERCLASS : Any, LOWERTYPE>(
    override val clazz: KClassifier?,
    override val name: String,
    override val isMarkedNullable: Boolean,
    val members: List<MemberValueGetter<*, *>> = emptyList(),
    val func: HIGHERCLASS.() -> LOWERTYPE
) : MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return higher.func()
    }

    override fun getLowerMembers(): List<MemberValueGetter<*, *>> {
        return members
    }
}

class ObjectTypeMapperCreator<HIGHERCLASS : Any, LOWERTYPE : Any>(val member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>) {

//    fun toObject(moreKeys: MoreKeysData): ObjectTypeHandler<HIGHERCLASS, LOWERTYPE> =
//        ObjectTypeHandler<HIGHERCLASS, LOWERTYPE>(member.isMarkedNullable, moreKeys, member.isMarkedNullable)
}

class NativeTypeMapperCreator<HIGHERCLASS : Any, LOWERTYPE>(val member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>) {

    private val type: NativeType = member.getType() ?: throw RuntimeException("unknown type: ${member.clazz}")

    val mapValue = DecoratorFactory.getDecorator(type, DefaultValueMapper<LOWERTYPE>())

    fun toNativeTypeHandler(): NativeTypeHandler<HIGHERCLASS, LOWERTYPE> {
        val n= NativeTypeHandler<HIGHERCLASS, LOWERTYPE>(
            type,
            member.name,
            member.isMarkedNullable,
            mapValue,
            member,
        )
        return n
    }
}

object DecoratorFactory {
    fun <T> getDecorator(
        type: NativeType,
        getValue: MapValue<T>
    ): MapValue<T> {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            NativeType.BOOLEAN -> BooleanValueGetterDecorator(getValue as MapValue<Boolean?>) as MapValue<T>
            NativeType.LONG -> LongValueGetterDecorator(getValue as MapValue<Long?>) as MapValue<T>
            NativeType.STRING -> StringValueGetterDecorator(getValue as MapValue<String?>) as MapValue<T>
            else -> getValue
        }
    }
}

class LongValueGetterDecorator(val getValue: MapValue<Long?>) :
    MapValue<Long?> by getValue {
    override fun getValue(databaseReader: DatabaseReader, counter: Int): Long? {
        return databaseReader.nextLong(counter)
    }
}

data class BooleanValueGetterDecorator(val getValue: MapValue<Boolean?>) :
    MapValue<Boolean?> by getValue {
    override fun insertValue(t: Boolean?) = when (t) {
        null -> "null"
        true -> "1"
        else -> "0"
    }

    override fun getValue(databaseReader: DatabaseReader, counter: Int): Boolean? {
        return when (databaseReader.next(counter)) {
            null -> null
            1 -> true
            else -> false
        }
    }
}


data class StringValueGetterDecorator(val getValue: MapValue<String?>) :
    MapValue<String?> by getValue {
    override fun insertValue(t: String?): String {
        return if (t == null) "null" else "\"$t\""
    }
}

interface ValueInserter<T> {
    fun insertValue(t: T?): String
}

interface ValueInserterMapper<HIGHER : Any, T> : ValueInserter<T> {
    fun toInsert(any: HIGHER?): String
}

interface TypeHandler<HIGHER : Any, T, TYPEHANDLER : TypeHandler<HIGHER, T, TYPEHANDLER>> : InsertHeadable,
    NullableElement, Headerable,
    ValueInserterMapper<HIGHER, T> {
    fun mapName(name: String): TYPEHANDLER
    fun map(name: String): TypeHandler<HIGHER, *, *> {
        return mapName(name)
    }
}


data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val list: List<Any?>)
