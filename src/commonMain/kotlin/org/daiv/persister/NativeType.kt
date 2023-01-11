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
