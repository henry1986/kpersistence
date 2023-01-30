package org.daiv.persister

import org.daiv.persister.sql.command.Column
import org.daiv.persister.sql.command.HeaderValuePair
import org.daiv.persister.sql.command.SelectKey
import kotlin.reflect.KProperty1

interface TypeNameable {
    val typeName: String
}

interface InsertHeadable {
    fun insertHead(): Row
}

interface Nameable {
    val name: String
    val nonMappedName: String
}

interface NullableElement {
    val isNullable: Boolean
}

interface Headerable {
    fun toHeader(): Row
}


enum class NativeType(override val typeName: String, val isNative: Boolean = true, val isCollection: Boolean = false) :
    TypeNameable {
    INT("INT"), STRING("TEXT"), LONG("LONG"), BOOLEAN("INT"), DOUBLE("REAL"), ENUM("STRING"),

    MAP("MAP", false, true), LIST("LIST", false, true), SET("SET", false, true)
}

interface DatabaseReaderValueGetter {
    fun getValue(databaseReader: DatabaseReader, counter: Int): Any? {
        return databaseReader.get(counter)
    }
}

data class ColumnValues(val holderKeys: List<Any?>, val lowerValues: List<Any?>)

interface ToValueable<T> {
    fun toValue(columnValues: ColumnValues, tableCollector: TableCollector): T?
}

interface ToStringable<T> {
    fun asString(t: T?): String
}

interface MapValue<T> : NativeValueInserter<T>, DatabaseReaderValueGetter, ToValueable<T>

class DefaultValueMapper<T>() : AbstractValueMapper<T> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        return true
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

interface AbstractValueMapper<LOWERTYPE> : MapValue<LOWERTYPE> {

    override fun toValue(columnValues: ColumnValues, tableCollector: TableCollector): LOWERTYPE {
        return columnValues.lowerValues.first() as LOWERTYPE
    }

    override fun asString(t: LOWERTYPE?): String {
        return t?.toString() ?: "null"
    }
}

object DecoratorFactory {
    fun <T> getDecorator(
        type: NativeType,
    ): MapValue<T> {
        @Suppress("UNCHECKED_CAST")
        return when (type) {
            NativeType.BOOLEAN -> BooleanValueGetterDecorator as MapValue<T>
            NativeType.LONG -> LongValueGetterDecorator as MapValue<T>
            NativeType.STRING -> StringValueGetterDecorator as MapValue<T>
            else -> DefaultValueMapper()
        }
    }
}

object LongValueGetterDecorator : AbstractValueMapper<Long?> {
    override fun getValue(databaseReader: DatabaseReader, counter: Int): Long? {
        return databaseReader.getLong(counter)
    }
}

object BooleanValueGetterDecorator : AbstractValueMapper<Boolean> {

    override fun asString(t: Boolean?): String {
        return when (t) {
            null -> "null"
            true -> "1"
            else -> "0"
        }
    }

    override fun getValue(databaseReader: DatabaseReader, counter: Int): Boolean? {
        return when (databaseReader.get(counter)) {
            null -> null
            1 -> true
            else -> false
        }
    }
}


object StringValueGetterDecorator : AbstractValueMapper<String> {

    override fun asString(t: String?): String {
        return if (t == null) "null" else "\"$t\""
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return this::class == other::class
    }

    override fun hashCode(): Int {
        return this::class.hashCode()
    }
}

interface ValueInserter<T> {
    fun insertValue(t: T?): Row
}

interface NativeValueInserter<T> : ToStringable<T>, ValueInserter<T> {
    override fun insertValue(t: T?): Row {
        return Row(asString(t))
    }
}

interface ValueInserterMapper<HIGHER : Any> {
    fun toInsert(any: HIGHER?): Row
}

interface ValueInserterWithGetter<HIGHER : Any, T> : ValueInserterMapper<HIGHER>, GetValue<HIGHER, T>,
    ValueInserter<T> {
    override fun toInsert(any: HIGHER?): Row {
        if (any == null)
            return Row("null")
        return insertValue(get(any))
    }
}

interface ReadFromDB {
    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner
}

interface NativeHeader : TypeNameable, Nameable, NullableElement, Headerable, InsertHeadable {
    override fun insertHead(): Row {
        return Row(name)
    }

    override fun toHeader(): Row {
        return Row("$name $typeName ${if (!isNullable) "NOT NULL" else ""}")
    }
}

interface TypeHandler<HIGHER : Any, T> : ColTypeHandler<T>, ValueInserterMapper<HIGHER>, ValueInserter<T> {
    override fun mapName(name: String): TypeHandler<HIGHER, T>
}

data class ColumnRef(val name: String, val columnRef: ColumnRef? = null) {
    fun buildName(): String = if (columnRef == null) name else "${name}_${columnRef.buildName()}"
}

interface NameBuilder : Nameable {
    fun nextName(name: String): String {
        return "${name}_${this.name}"
    }
}

interface Columnable {
    /**
     * describes the number of columns of this type
     */
    val numberOfColumns: Int
}


interface SelectHeader {
    fun select(list: List<SelectKey>): List<Column>
}

interface NativeSelectHeader<T>:SelectHeader, Nameable, ToStringable<T>{
    override fun select(list: List<SelectKey>): List<Column> {
        if (list.size != 1) {
            throw RuntimeException("expected a size of 1")
        }
        val select = list.first()
        val name = select.keys.joinToString("_")
        if (name != this.name) {
            throw RuntimeException("expected name ${this.name}, but was $name")
        }
        return listOf(Column(name, asString(select.value as T?)))
    }
}

interface ColTypeHandler<T> : InsertHeadable,
    NullableElement, Headerable, ReadFromDB, ValueInserter<T>, NameBuilder, Columnable, ToValueable<T>,
    SelectHeader {
    fun mapName(name: String): ColTypeHandler<T>
}

