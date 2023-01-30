package org.daiv.persister.sql.command

import kotlin.reflect.KProperty1

fun Any?.forKey(vararg p: KProperty1<*, *>) = PropertySelectKey(p.asList(), this)

data class PropertySelectKey(val keys: List<KProperty1<*, *>>, val value: Any?) {
    fun toSelectKey() = SelectKey(keys.map { it.name }, value)
}

data class SelectKey(val keys: List<String>, val value: Any?)
