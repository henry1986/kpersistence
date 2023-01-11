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
}

interface MemberValueGetter<HIGHERCLASS : Any, LOWERTYPE> : Member, GetValue<HIGHERCLASS, LOWERTYPE>

inline fun <HIGHERCLASS : Any, reified LOWERTYPE> memberValueGetter(
    name: String,
    isMarkedNullable: Boolean,
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = memberValueGetterCreator(name, isMarkedNullable, func).toNativeTypeHandler()

inline fun <HIGHERCLASS : Any, reified LOWERTYPE> memberValueGetterCreator(
    name: String,
    isMarkedNullable: Boolean,
    noinline func: HIGHERCLASS.() -> LOWERTYPE
) = NativeTypeMapperCreator(DefaultMemberValueGetter(LOWERTYPE::class, name, isMarkedNullable, func))

class DefaultMemberValueGetter<HIGHERCLASS : Any, LOWERTYPE>(
    override val clazz: KClassifier?,
    override val name: String,
    override val isMarkedNullable: Boolean,
    val func: HIGHERCLASS.() -> LOWERTYPE
) : MemberValueGetter<HIGHERCLASS, LOWERTYPE> {
    override fun get(higher: HIGHERCLASS): LOWERTYPE? {
        return higher.func()
    }
}

class ObjectTypeMapperCreator<HIGHERCLASS : Any, LOWERTYPE>(val member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>){
    fun toObject():ObjectTypeHandler<HIGHERCLASS, LOWERTYPE> = ObjectTypeHandler<>()
}

class NativeTypeMapperCreator<HIGHERCLASS : Any, LOWERTYPE>(val member: MemberValueGetter<HIGHERCLASS, LOWERTYPE>) {

    private val type: NativeType = when (member.clazz) {
        Int::class -> NativeType.INT
        Long::class -> NativeType.LONG
        String::class -> NativeType.STRING
        Double::class -> NativeType.DOUBLE
        Boolean::class -> NativeType.BOOLEAN
        else -> throw RuntimeException("unknown type: ${member.clazz}")
    }

    val mapValue = DecoratorFactory.getDecorator(type, DefaultValueMapper<LOWERTYPE>())

    fun toNativeTypeHandler(): NativeTypeHandler<HIGHERCLASS, LOWERTYPE> =
        NativeTypeHandler(
            type,
            member.name,
            member.isMarkedNullable,
            mapValue,
            member,
        )
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
    fun map(name: String): TypeHandler<HIGHER, *, *>{
        return mapName(name)
    }
}


data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val list: List<Any?>)
