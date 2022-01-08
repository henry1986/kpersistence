package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.ObjectRelationalMapper

class SQLInterpreter<T : Any>(val objectRelationalMapper: ObjectRelationalMapper<T>, val cormMap: CormMap) {

    fun select(tableName: String, propertyNames: List<String>): String {
        val ret = objectRelationalMapper.objectRelationalHeader.allHeads(null, null)
            .filter { propertyNames.find { property -> property == it.parameterList.first().name } != null }
            .joinToString(" AND ") { "${it.name} = ?" }
        return "SELECT * FROM $tableName WHERE $ret;"
    }

}