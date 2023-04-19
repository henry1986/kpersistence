package org.daiv.persister.sql.command

import kotlin.reflect.KProperty1

/**
 * Creates a [PropertySelectKey] object that represents one or more columns of a database table and the
 * corresponding value(s).
 *
 * @param p A list of [KProperty1] objects that represent property references to the columns of the table
 * that the [SelectKey] targets.
 * @return A [PropertySelectKey] object that represents the columns and the value(s) that the [SelectKey] targets.
 */
fun<T:Any?> T.forKey(vararg p: KProperty1<*, T>) = PropertySelectKey(p.asList(), this)

data class PropertySelectKey(val keys: List<KProperty1<*, *>>, val value: Any?) {
    fun toSelectKey() = SelectKey(keys.map { it.name }, value)
}

/**
 * Represents a key that targets one or more columns of a database table and the corresponding value(s).
 *
 * @param keys A list of column names that the [SelectKey] targets.
 * @param value The value(s) associated with the columns that the [SelectKey] targets.
 */
data class SelectKey(val keys: List<String>, val value: Any?)
