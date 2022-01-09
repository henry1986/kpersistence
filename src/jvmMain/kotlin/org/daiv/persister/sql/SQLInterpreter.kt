package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.HashCodeCounterGetter
import org.daiv.persister.objectrelational.ObjectRelationalMapper


class SQLInterpreter<T : Any>(
    val objectRelationalMapper: ObjectRelationalMapper<T>,
    val cormMap: CormMap,
    val hashCodeCounterGetter: HashCodeCounterGetter = HashCodeCounterGetter.nullGetter
) {

    fun select(tableName: String, propertyNames: List<String>): String {
        val ret = objectRelationalMapper.objectRelationalHeader.allHeads(null, null)
            .filter { propertyNames.find { property -> property == it.parameterList.first().name } != null }
            .joinToString(" AND ") { "${it.name} = ?" }
        return "SELECT * FROM $tableName WHERE $ret;"
    }

    fun insert(ts: List<T>): Pair<String, List<String>> {
        if (ts.isEmpty()) {
            return "" to emptyList()
        }
        val writer = objectRelationalMapper.objectRelationalWriter
        val rows = ts.map { writer.writeRow(null, it, hashCodeCounterGetter) }
        val names = rows.first().joinToString(", ") { "${it.name}" }
        val values = rows.map { row -> row.joinToString(", ") { "${it.value}" } }
        return names to values
    }


}
