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

interface GetValue<HIGHERCLASS : Any, T> {
    fun get(higherClass: HIGHERCLASS): T

    fun asString(higherClass: HIGHERCLASS): String {
        return get(higherClass).toString()
    }

    fun getValue(databaseReader: DatabaseReader, counter: Int): Any? {
        return databaseReader.next(counter)
    }
}

object DecoratorFactory {
    fun <HIGHERCLASS : Any> getDecorator(
        type: NativeType,
        getValue: GetValue<HIGHERCLASS, *>
    ): GetValue<HIGHERCLASS, *> {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            NativeType.BOOLEAN -> BooleanValueGetterDecorator(getValue as GetValue<HIGHERCLASS, Boolean?>)
            NativeType.LONG -> LongValueGetterDecorator(getValue as GetValue<HIGHERCLASS, Long?>)
            NativeType.STRING -> StringValueGetterDecorator(getValue as GetValue<HIGHERCLASS, String?>)
            else -> getValue
        }
    }
}

class LongValueGetterDecorator<HIGHERCLASS : Any>(val getValue: GetValue<HIGHERCLASS, Long?>) :
    GetValue<HIGHERCLASS, Long?> by getValue {
    override fun getValue(databaseReader: DatabaseReader, counter: Int): Long? {
        return databaseReader.nextLong(counter)
    }
}

data class BooleanValueGetterDecorator<HIGHERCLASS : Any>(val getValue: GetValue<HIGHERCLASS, Boolean?>) :
    GetValue<HIGHERCLASS, Boolean?> by getValue {
    override fun asString(higherClass: HIGHERCLASS) = when (get(higherClass)) {
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


data class StringValueGetterDecorator<HIGHERCLASS : Any>(val getValue: GetValue<HIGHERCLASS, String?>) :
    GetValue<HIGHERCLASS, String?> by getValue {
    override fun asString(higherClass: HIGHERCLASS): String {
        val get = get(higherClass)
        return if (get == null) "null" else "\"${get}\""
    }
}

interface TypeHandler<T : TypeHandler<T>> : InsertHeadable, NullableElement, Headerable {
    fun mapName(name: String): T
}


data class NativeTypeHandler<HIGHERCLASS : Any, T>(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    private val valueGetter: GetValue<HIGHERCLASS, T>
) : TypeHandler<NativeTypeHandler<HIGHERCLASS, T>>, TypeNameable by type, Nameable {

    override fun insertHead(): String {
        return name
    }

    override fun toHeader(): String {
        return "$name $typeName ${if (!isNullable) "NOT NULL" else ""}"
    }

    override fun mapName(name: String): NativeTypeHandler<HIGHERCLASS, T> {
        return copy(name = "${name}_${this.name}")
    }

    fun insertValue(higherClass: HIGHERCLASS): String {
        return valueGetter.asString(higherClass)
    }

    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        val t = valueGetter.getValue(databaseRunner.databaseReader, databaseRunner.count)
        return databaseRunner.copy(count = databaseRunner.count + 1, list = databaseRunner.list + t)
    }
}

data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val list: List<Any?>)
