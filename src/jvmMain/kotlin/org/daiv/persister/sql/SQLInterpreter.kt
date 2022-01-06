package org.daiv.persister.sql

import org.daiv.persister.objectrelational.CormMap
import org.daiv.persister.objectrelational.ObjectRelationalMapper
import kotlin.reflect.KProperty1

class SQLInterpreter<T : Any>(val objectRelationalMapper: ObjectRelationalMapper<T>, val cormMap: CormMap) {

    fun select(propertyNames: List<String>): Double {
        
    }

}