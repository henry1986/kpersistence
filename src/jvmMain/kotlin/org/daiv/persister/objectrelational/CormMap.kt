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

    private suspend fun <R, T> KProperty1<R, T>.toMapper(clazz: KClass<Any>) =
        PropertyMapper(this, getValue(clazz) as ObjectRelationalMapper<T>)

    suspend fun <T : Any> createNoNative(clazz: KClass<T>): List<PropertyMapper<T, Any?>> =
        withContext(scopeContextable.context) {
            (clazz.primaryConstructor?.parameters?.map {
                val type = it.type.type<Any>()
                val nextType = when {
                    type.simpleName.isNative() -> type
                    type.simpleName.isList() || type.simpleName.isSet() -> {
                        val type = it.type.arguments[0].type!!
                        if (type.typeName().isNative()) {

                        } else {

                        }
                        type.type()
                    }
                    type.simpleName.isMap() -> it.type.arguments[0].type!!.type()
                    else -> type
                }
                it to nextType
            }
                ?: throw NullPointerException("there is no argument or no primary constructor for clazz: $clazz"))
                .filter { !it.second.simpleName.isNative() && !it.second.simpleName.isCollection() }.asFlow()
                .map { pair ->
                    clazz.declaredMemberProperties.find { it.name == pair.first.name }!!.toMapper(pair.second)
                }.toList()
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
