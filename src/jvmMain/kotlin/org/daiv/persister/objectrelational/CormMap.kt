package org.daiv.persister.objectrelational

import kotlinx.coroutines.withContext
import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.coroutines.ScopeContextable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

class CormMap(val scopeContextable: ScopeContextable = DefaultScopeContextable()) : ClassParseable {
    val chdMap = CHDMap<JClassHeaderData> { a, _ -> DefaultJClassHeaderData.toParameters(a, this) }
    val calculationCollection = CalculationSuspendableMap<KClass<*>, CORM<*>>("") {
        it.objectRelationMapper(this)
    }

    suspend fun <T : Any> getValue(clazz: KClass<T>): CORM<T> {
        return calculationCollection.getValue(clazz) as CORM<T>
    }

    /**
     * creates PropertyMapper for every Object, that is not a native or a collection type.
     */
    suspend fun <T : Any> createPropertyMapper(classHeaderData: JClassHeaderData): List<PropertyMapper<T, *>> =
        withContext(scopeContextable.context) {
            (classHeaderData.parameters.filter { it.type.typeName().isCollection() }?.flatMap { parameter ->
                classHeaderData.clazz.declaredMemberProperties.filter { it.name == parameter.name }
                    .map { parameter.buildPropertyMapper(it as KProperty1<T, *>, this@CormMap) }
            })
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
