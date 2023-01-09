package org.daiv.persister

interface TypeNameable {
    val typeName: String
}

interface Nameable {
    fun insertHead(): String {
        return name
    }

    val name: String
}

interface NullableElement {
    val isNullable: Boolean
}

interface Headerable : Nameable, TypeNameable, NullableElement {
    fun toHeader(): String {
        return "$name $typeName ${if (!isNullable) "NOT NULL" else ""}"
    }
}

enum class NativeType(override val typeName: String) : TypeNameable {
    INT("Int"), STRING("TEXT"), LONG("Long"), BOOLEAN("Int"), DOUBLE("real"), ENUM("String")
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

interface LongValueGetter<HIGHERCLASS : Any> : GetValue<HIGHERCLASS, Long?> {
    override fun getValue(databaseReader: DatabaseReader, counter: Int): Long? {
        return databaseReader.nextLong(counter)
    }
}

interface BooleanValueGetter<HIGHERCLASS : Any> : GetValue<HIGHERCLASS, Boolean?> {
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

interface StringValueGetter<HIGHERCLASS : Any> : GetValue<HIGHERCLASS, String?> {
    override fun asString(higherClass: HIGHERCLASS): String {
        val get = get(higherClass)
        return if (get == null) "null" else "\"${get}\""
    }
}


data class NativeTypeHandler<HIGHERCLASS : Any, T>(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    private val valueGetter: GetValue<HIGHERCLASS, T>
) : TypeNameable by type, Nameable, NullableElement, Headerable {

    fun insertValue(higherClass: HIGHERCLASS): String {
        return valueGetter.asString(higherClass)
    }

    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        val t = valueGetter.getValue(databaseRunner.databaseReader, databaseRunner.count)
        return databaseRunner.copy(count = databaseRunner.count + 1, list = databaseRunner.list + t)
    }
}

data class DatabaseRunner(val databaseReader: DatabaseReader, val count: Int, val list: List<Any?>) {
    fun next(typeHandler: NativeTypeHandler<*, *>): DatabaseRunner {
        return typeHandler.getValue(this)
    }
}