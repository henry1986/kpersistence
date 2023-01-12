package org.daiv.persister


interface ListTypeReader<KEY, LISTHOLDER, LISTELEMENT, LISTKEY> {
    fun getList(listHolder: LISTHOLDER): Map<LISTKEY, LISTELEMENT>
    fun getPrimaryKey(listHolder: LISTHOLDER): KEY
}

data class ListTypeHandler<PRIMARYKEY, LISTHOLDER : Any, LISTELEMENT, LISTKEY>(
    val primaryHandler: TypeHandler<LISTHOLDER, PRIMARYKEY, *>,
    val keyHandler: TypeHandler<LISTHOLDER, LISTKEY, *>,
    val valueHandler: TypeHandler<LISTHOLDER, LISTELEMENT, *>,
    val listReader: ListTypeReader<PRIMARYKEY, LISTHOLDER, LISTELEMENT, LISTKEY>
) : InsertHeadable, Headerable {
    private val nativeTypes = listOf(primaryHandler, keyHandler, valueHandler)
    override fun insertHead(): String {
        return nativeTypes.joinToString(", ") { it.insertHead() }
    }

    override fun toHeader(): String {
        return nativeTypes.joinToString(", ") { it.toHeader() }
    }

    fun insertValue(t: LISTHOLDER): String {
        val list = listReader.getList(t)
        val key = primaryHandler.insertValue(listReader.getPrimaryKey(t))
        return list.keys.joinToString(", ") {
            "$key, ${keyHandler.insertValue(it)}, ${valueHandler.insertValue(list[it])}"
        }
    }

    private fun getValues(databaseRunner: DatabaseRunner): DatabaseRunner {
        return valueHandler.getValue(keyHandler.getValue(primaryHandler.getValue(databaseRunner)))
    }

    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        var dbr = getValues(databaseRunner)
        while (dbr.databaseReader.next()) {
            dbr = getValues(dbr)
        }
        return dbr
    }
}
