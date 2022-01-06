package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.ObjectRelationalMapper

class SQLInterpreter<T : Any>(val objectRelationalMapper: ObjectRelationalMapper<T>, val cormMap: CormMap) {

    fun select(tableName: String, propertyNames: List<String>): String {
        val whereClause = propertyNames.joinToString(" AND ") { "$it = ?" }
        val n = objectRelationalMapper.objectRelationalHeader.allHeads(null)
        println("r: $n")
        return "SELECT * FROM $tableName WHERE $whereClause;"
    }

}