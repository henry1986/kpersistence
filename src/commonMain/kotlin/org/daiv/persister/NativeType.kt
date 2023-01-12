package org.daiv.persister

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
        return databaseReader.get(counter)
    }
}

interface MapValue<T> : ValueInserter<T>, DatabaseReaderValueGetter
class DefaultValueMapper<LOWERTYPE>() : MapValue<LOWERTYPE> {
    override fun insertValue(t: LOWERTYPE?): String {
        return t.toString()
    }

    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other == null || this::class != other::class) return false
        if (other == null) return false
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
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
        return databaseReader.getLong(counter)
    }

    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other == null || this::class != other::class) return false
        if (other == null) return false
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

class BooleanValueGetterDecorator(val getValue: MapValue<Boolean?>) :
    MapValue<Boolean?> by getValue {
    override fun insertValue(t: Boolean?) = when (t) {
        null -> "null"
        true -> "1"
        else -> "0"
    }

    override fun getValue(databaseReader: DatabaseReader, counter: Int): Boolean? {
        return when (databaseReader.get(counter)) {
            null -> null
            1 -> true
            else -> false
        }
    }

    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other == null || this::class != other::class) return false
        if (other == null) return false
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}


class StringValueGetterDecorator(val getValue: MapValue<String?>) :
    MapValue<String?> by getValue {
    override fun insertValue(t: String?): String {
        return if (t == null) "null" else "\"$t\""
    }

    override fun equals(other: Any?): Boolean {
//        if (this === other) return true
//        if (other == null || this::class != other::class) return false
        if (other == null) return false
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

interface ValueInserter<T> {
    fun insertValue(t: T?): String
}

interface ValueInserterMapper<HIGHER : Any, T> : ValueInserter<T> {
    fun toInsert(any: HIGHER?): String
}

interface ValueInserterWithGetter<HIGHER : Any, T> : ValueInserterMapper<HIGHER, T>, GetValue<HIGHER, T> {
    override fun toInsert(any: HIGHER?): String {
        if (any == null)
            return "null"
        return insertValue(get(any))
    }
}

interface ReadFromDB {
    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner
}

interface NativeHeader : TypeNameable, Nameable, NullableElement, Headerable, InsertHeadable {
    override fun insertHead(): String {
        return name
    }

    override fun toHeader(): String {
        return "$name $typeName ${if (!isNullable) "NOT NULL" else ""}"
    }
}

interface TypeHandler<HIGHER : Any, T, TYPEHANDLER : TypeHandler<HIGHER, T, TYPEHANDLER>> :
    ColTypeHandler<HIGHER, T, TYPEHANDLER>, ValueInserterMapper<HIGHER, T>{
    override fun map(name: String): TypeHandler<HIGHER, *, *> {
        return mapName(name)
    }

}

interface ColTypeHandler<HIGHER : Any, T, TYPEHANDLER : ColTypeHandler<HIGHER, T, TYPEHANDLER>> : InsertHeadable,
    NullableElement, Headerable, ReadFromDB, ValueInserter<T> {
    fun mapName(name: String): TYPEHANDLER
    fun map(name: String): ColTypeHandler<HIGHER, *, *> {
        return mapName(name)
    }
}


data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val list: List<Any?>) {
    constructor(list: List<Any?>) : this(DefaultDatabaseReader.simple(list), 1, emptyList())
}
