package org.daiv.persister.objectrelational

import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.isAccessible


data class PropertyMapper<R, out T>(
    val parameter: Parameter,
    private val p: KProperty1<R, T>,
    private val writer: suspend () -> RowWriter<T>
) {
    private fun KProperty1<R, T>.writerMap(c: suspend () -> RowWriter<T>): ObjectRelationalWriterMap<R, T> =
        ObjectRelationalWriterMap(c) {
            isAccessible = true
            val r = get(this)
            r
        }

    val propGetter: R.() -> T? = {
        p.isAccessible = true
        p.get(this)
    }

    fun writerMap(): ObjectRelationalWriterMap<R, out T> = p.writerMap(writer)
}

