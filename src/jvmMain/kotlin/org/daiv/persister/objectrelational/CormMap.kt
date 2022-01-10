package org.daiv.persister.objectrelational

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.coroutines.ScopeContextable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class CormMap(val scopeContextable: ScopeContextable = DefaultScopeContextable()) : ClassParseable {
    val chdMap = CHDMap(JClassHeaderData::toParameters)
    val calculationCollection = CalculationSuspendableMap<KClass<*>, CORM<*>>("") {
        it.objectRelationMapper(this)
    }

    suspend fun <T : Any> getValue(clazz: KClass<T>): CORM<T> {
        return calculationCollection.getValue(clazz) as CORM<T>
    }

    private suspend fun <R, T> KProperty1<R, T>.toMapper(parameter: Parameter, clazz: KClass<Any>) =
        PropertyMapper(parameter, this, (getValue(clazz) as ObjectRelationalMapper<T>).objectRelationalWriter)

    /**
     * creates PropertyMapper for every Object, that is not a native or a collection type.
     */
    suspend fun <T : Any> createPropertyMapper(clazz: KClass<T>): List<PropertyMapper<T, Any?>> =
        withContext(scopeContextable.context) {
            println("create NoNative: $clazz")
            (clazz.primaryConstructor?.parameters?.map {
                val type = it.type.type<Any>()
                println("create NoNative parameter: $type")
                val nextType = when {
                    type.simpleName.isNative() -> type
                    type.simpleName.isList() || type.simpleName.isSet() -> {
                        val genericType = it.type.arguments[0].type!!
                        if (genericType.typeName().isNative()) {

                        } else {

                        }
                        genericType.type()
                    }
                    type.simpleName.isMap() -> it.type.arguments[0].type!!.type()
                    else -> type
                }
                println("nextType: $nextType")
                it to nextType
            }
                ?: throw NullPointerException("there is no argument or no primary constructor for clazz: $clazz"))
                .filter { !it.second.simpleName.isNative() && !it.second.simpleName.isCollection() }.asFlow()
                .map { pair ->
                    println("pair: $pair")
                    clazz.declaredMemberProperties.find { it.name == pair.first.name }!!.toMapper(pair.second)
                }.toList()
        }

    /**
     * creates PropertyMapper for every Object, that is not a native or a collection type.
     */
    suspend fun <T : Any> createCollections(classHeaderData: ClassHeaderData): List<PropertyMapper<T, *>> =
        withContext(scopeContextable.context) {
            val clazz = classHeaderData.clazz
            println("create Collection: $clazz")
            val x = (classHeaderData.parameters.filter { it.type.typeName().isCollection() }?.flatMap { paramater ->
                val mappped = clazz.declaredMemberProperties.filter { it.name == paramater.name }
                    .map {
                        it as KProperty1<T, List<out Any>>
                        PropertyMapper(paramater, it) {
                            val value = getValue(paramater.genericNotNativeType().first().type()).objectRelationalWriter
                            val x: RowWriter<List<Any>?> = ListObjectWriter { value }
                            x
                        }
                    }
                mappped
            })
            x
        }

    fun createKeys(classParameter: ClassParameter<*>): Map<KClass<*>, () -> CORM<out Any>> {
        val filter = classParameter.parameters.filter {

            val typeName = it.type.typeName() ?: throw NullPointerException("did not find a typeName for $it")
            !typeName.isNative() && !typeName.isCollection()
        }
        println("filter:$filter")
        return filter.associate {
            val type = it.type.utype()
            type to {
                calculationCollection.tryDirectGet(type)
                    ?: throw NullPointerException("type $type was currently not created")
            }
        }
    }
}
