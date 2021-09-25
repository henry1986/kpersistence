package org.daiv.persister.objectrelational

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.daiv.persister.MoreKeys
import org.daiv.persister.table.default
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor


class CORM<T : Any>(val clazz: KClass<T>, val map: CalculationMap) : ObjectRelationalMapper<T> {
    fun <T : Any> Collection<KProperty1<T, *>>.noCollectionMembers() =
        filter { !it.returnType.typeName().isCollection() }

    override fun hashCodeX(t: T): Int {
        TODO("Not yet implemented")
    }

    suspend fun <R, T : Any> KProperty1<R, T>.toMapper(map: CalculationMap, clazz: KClass<T>) =
        PropertyMapper(this, map.getValue(clazz))

    val noNative = runBlocking {
        clazz.declaredMemberProperties.map { it to it.returnType.type<Any>() }
            .filter { !it.second.simpleName.isNative() && !it.second.simpleName.isCollection() }.asFlow()
            .map { (it as KProperty1<T, Any>).toMapper(map, it.second) }.toList()
    }

    val moreKeys = clazz.findAnnotation<MoreKeys>().default()
    override val objectRelationalHeader: ObjectRelationalHeader by lazy {
        val others = clazz.declaredMemberProperties.mapIndexed { i, it ->
            val typeName = it.returnType.typeName()!!
            val x = when {
                typeName.isNative() -> listOf(HeadEntry(it.name, typeName, moreKeys.amount >= i + 1))
                typeName.isCollection() -> emptyList()
                else -> typeName.headValue(runBlocking { map.getValue(it.returnType.type()) })
            }
            x
        }.flatten()
        val keys = others.takeWhile { it.isKey }
        ObjectRelationalHeaderData(keys, others, noNative.map { it.mapper.objectRelationalHeader })
    }

    override val objectRelationalWriter: ObjectRelationalWriter<T> by lazy {
        fun List<IndexedValue<KProperty1<T, *>>>.withoutIndex() = map { it.value }
        fun Map<Boolean, List<IndexedValue<KProperty1<T, *>>>>.toWriteEntry(isKey: Boolean) =
            get(isKey)?.withoutIndex()?.noCollectionMembers()?.toWriteEntry(isKey) ?: emptyList()

        val keysBase = clazz.declaredMemberProperties.withIndex().groupBy { it.index < moreKeys.amount }

        val keys = keysBase.toWriteEntry(true)
        val others = keys + keysBase.toWriteEntry(false)

        ObjectRelationalWriterData(keys, others, noNative.map {
            it.writerMap()
        })
    }

    fun KType.toNative(nativeReads: NativeReads) = when(this){
        Int::class -> {nativeReads.readInt()}
        Double::class -> {nativeReads.readDouble()}
        else -> {null}
    }

    override val objectRelationalReader: ObjectRelationalReader<T> by lazy {
        val keys = clazz.declaredMemberProperties.noCollectionMembers().map { p ->
            ReadEntryTask(p.name) {
                p.returnType.toNative(nativeReads)
            }
        }
        val builder: ReadMethod.() -> T =
            {
                clazz.objectInstance ?: clazz.primaryConstructor!!.call(*(this.list.toTypedArray()))
            }
        ObjectRelationalReaderData("", keys, listOf(), builder)
    }

}

fun <T : Any> KClass<T>.objectRelationMapper(): CORM<T> {
    return CORM(this, CalculationMap())
}

