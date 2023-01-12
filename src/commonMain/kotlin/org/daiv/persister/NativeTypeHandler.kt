@file:Suppress("DataClassPrivateConstructor")

package org.daiv.persister

interface GetValueFromDB<T> : ReadFromDB, MapValue<T> {
    override fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        val t = getValue(databaseRunner.databaseReader, databaseRunner.count)
        return databaseRunner.copy(count = databaseRunner.count + 1, list = databaseRunner.list + t)
    }
}

fun <R> List<R>.asMap() :Map<Int, R>{
    return mapIndexed{i, it -> i to it}.toMap()
}

data class ListNativeTypeHandler<HIGHER : Any, T>private constructor(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    private val mapValue: MapValue<T>,
) : ColTypeHandler<HIGHER, T, ListNativeTypeHandler<HIGHER, T>>, TypeNameable by type, NativeHeader,
    ValueInserter<T> by mapValue, GetValueFromDB<T> {
    constructor(
        type: NativeType,
        name: String,
        isNullable: Boolean,
    ) : this(type, name, isNullable, DecoratorFactory.getDecorator(type, DefaultValueMapper()))

    override fun mapName(name: String): ListNativeTypeHandler<HIGHER, T> {
        return copy(name = nextName(name))
    }
}

data class NativeTypeHandler<HIGHER : Any, T> private constructor(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    val getValue: GetValue<HIGHER, T>,
    val mapValue: MapValue<T>,
) : TypeHandler<HIGHER, T, NativeTypeHandler<HIGHER, T>>, TypeNameable by type, NativeHeader,
    ValueInserter<T> by mapValue, ValueInserterWithGetter<HIGHER, T>, GetValue<HIGHER, T> by getValue,
    GetValueFromDB<T> {
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
