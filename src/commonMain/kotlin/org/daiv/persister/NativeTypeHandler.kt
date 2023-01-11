package org.daiv.persister

data class NativeTypeHandler<HIGHER : Any, T>(
    private val type: NativeType,
    override val name: String,
    override val isNullable: Boolean,
    val mapValue: MapValue<T>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T, NativeTypeHandler<HIGHER, T>>, TypeNameable by type, Nameable, ValueInserter<T> by mapValue {

    override fun insertHead(): String {
        return name
    }

    override fun toHeader(): String {
        return "$name $typeName ${if (!isNullable) "NOT NULL" else ""}"
    }

    override fun mapName(name: String): NativeTypeHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    override fun toInsert(any: HIGHER?): String {
        if (any == null)
            return "null"
        return insertValue(getValue.get(any))
    }

    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        val t = mapValue.getValue(databaseRunner.databaseReader, databaseRunner.count)
        return databaseRunner.copy(count = databaseRunner.count + 1, list = databaseRunner.list + t)
    }
}
