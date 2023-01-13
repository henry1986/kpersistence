package org.daiv.persister

import kotlin.reflect.KClass


interface FoldList<T>{
    val nativeTypes: List<T>

    fun foldList(func: T.() -> Row): Row {
        return nativeTypes.fold(Row()) { r1, r2 -> r1 + r2.func() }
    }
}

interface HeaderBuilder<T> : InsertHeadable, Headerable, FoldList<T> where T : Headerable, T : InsertHeadable {

    override fun insertHead(): Row {
        return foldList { insertHead() }
    }

    override fun toHeader(): Row {
        return foldList { toHeader() }
    }
}

interface MainObjectHandler<T : Any> : ReadFromDB, HeaderBuilder<TypeHandler<T, *>>, ValueInserter<T> {
    override val nativeTypes: List<TypeHandler<T, *>>

    override fun insertValue(t: T?): Row {
        return foldList { toInsert(t) }
    }

    private fun recursiveRead(databaseRunner: DatabaseRunner, i: Int): DatabaseRunner {
        if (i < nativeTypes.size) {
            val n = nativeTypes[i]
            val next = n.getValue(databaseRunner)
            return recursiveRead(next, i + 1)
        }
        return databaseRunner
    }

    override fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        return recursiveRead(databaseRunner, 0)
    }
}

inline fun <reified T : Any> objectType(nativeTypes: List<TypeHandler<T, *>>) = ObjectTypeHandler(nativeTypes, T::class)
interface Classable<T : Any> {
    val clazz: KClass<T>
}

data class ObjectTypeHandler<T : Any>(
    override val nativeTypes: List<TypeHandler<T, *>>,
    val clazz: KClass<T>,
) : ValueInserter<T>, InsertHeadable, Headerable, MainObjectHandler<T> {

}

data class ObjectTypeRefHandler<HIGHER : Any, T : Any>(
    override val name: String,
    override val isNullable: Boolean,
    override val clazz: KClass<T>,
    val moreKeys: MoreKeysData,
    val _nativeTypes: List<TypeHandler<T, *>>,
    val getValue: GetValue<HIGHER, T>
) : TypeHandler<HIGHER, T>, Nameable, MainObjectHandler<T>, ToValueable<T>, Classable<T> {

    override val nativeTypes = _nativeTypes.take(moreKeys.amount).map { it.mapName(name) }

    override fun toValue(list: List<Any?>, tableCollector: TableCollector): T? {
        return tableCollector.getTableReader(clazz)?.readFromTable(list)
    }

    override val numberOfColumns: Int = nativeTypes.sumOf { it.numberOfColumns }

    override fun mapName(name: String): ObjectTypeRefHandler<HIGHER, T> {
        return copy(name = "${name}_${this.name}")
    }

    override fun toInsert(any: HIGHER?): Row {
        if (any == null) {
            return Row("null")
        }
        return insertValue(getValue.get(any))
    }
}
