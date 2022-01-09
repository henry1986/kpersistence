package org.daiv.persister.objectrelational

import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.coroutines.ScopeContextable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible


data class PropertyMapper<R, T>(val p: KProperty1<R, T>, val mapper: ObjectRelationalMapper<T>) {
    private fun KProperty1<R, T>.writerMap(c: ObjectRelationalMapper<T>): ObjectRelationalWriterMap<R, T> =
        ObjectRelationalWriterMap(c.objectRelationalWriter) {
            isAccessible = true
            val r = get(this)
            r
        }

    fun writerMap() = p.writerMap(mapper)
}


data class PropertyListMapper<R, T>(val p: KProperty1<R, List<T>>, val mapper: ObjectRelationalMapper<T>) {
    private fun KProperty1<R, List<T>>.writerMap(c: ObjectRelationalMapper<T>): ObjectRelationalWriterMap<R, T> =
        ObjectRelationalWriterMap(c.objectRelationalWriter) {
            isAccessible = true
            val r = get(this)
            r
        }

    fun writerMap() = p.writerMap(mapper)
}

