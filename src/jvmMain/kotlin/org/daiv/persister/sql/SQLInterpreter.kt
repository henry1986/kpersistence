package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.ObjectRelationalMapper

class SQLInterpreter<T : Any>(val objectRelationalMapper: ObjectRelationalMapper<T>, val cormMap: CormMap) {

    fun select(tableName: String, propertyNames: List<String>): String {
        val whereClause = propertyNames.joinToString(" AND ") { "$it = ?" }
        val p = objectRelationalMapper.classHeaderData.parameters
        val parameters = propertyNames.map { property ->
            p.find { property == it.name } ?: throw RuntimeException("did not find a parameter with name $property")
        }
        return "SELECT * FROM $tableName WHERE $whereClause;"
    }

}