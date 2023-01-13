@file:Suppress("DataClassPrivateConstructor")

package org.daiv.persister

interface GetValueFromDB<T> : ReadFromDB, DatabaseReaderValueGetter {
    override fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        val t = getValue(databaseRunner.databaseReader, databaseRunner.count)
        return databaseRunner.next(count = databaseRunner.count + 1, row = DRow(databaseRunner.rows.last().list + t))
    }
}

interface NativeTypeColumn : Columnable {
    override val numberOfColumns: Int
        get() = 1
}

fun <R> List<R>.asMap(): Map<Int, R> {
    return mapIndexed { i, it -> i to it }.toMap()
}

data class ListNativeTypeHandler<T> private constructor(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    private val mapValue: MapValue<T>,
) : ColTypeHandler<T>, TypeNameable by type, NativeHeader,
    ValueInserter<T> by mapValue, ToValueable<T> by mapValue, GetValueFromDB<T>, NativeTypeColumn {
    constructor(
        type: NativeType,
        name: String,
        isNullable: Boolean,
    ) : this(type, name, isNullable, DecoratorFactory.getDecorator(type, DefaultValueMapper()))

    override val numberOfColumns: Int = 1

    override fun mapName(name: String): ListNativeTypeHandler<T> {
        return copy(name = nextName(name))
    }
}

data class NativeTypeHandler<HIGHER : Any, T> private constructor(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    val getValue: GetValue<HIGHER, T>,
    val mapValue: MapValue<T>,
) : TypeHandler<HIGHER, T>, TypeNameable by type, NativeHeader,
    ValueInserter<T> by mapValue, ToValueable<T> by mapValue, ValueInserterWithGetter<HIGHER, T>,
    GetValue<HIGHER, T> by getValue,
    GetValueFromDB<T>, NativeTypeColumn {
    constructor(
        type: NativeType,
        name: String,
        isNullable: Boolean,
        getValue: GetValue<HIGHER, T>
    ) : this(type, name, isNullable, getValue, DecoratorFactory.getDecorator(type, DefaultValueMapper()))

    override fun mapName(name: String): NativeTypeHandler<HIGHER, T> {
        return copy(name = nextName(name))
    }
}
