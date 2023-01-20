package org.daiv.persister.sql.command

import org.daiv.persister.Row

data class InsertTableData(val header: Row, val values: List<Row>)
data class CreateTableData(val header: Row, val primaryKey: Row)
object SelectAllTableData

data class Column(val keyName: String, val value: String)

interface HeaderValuePair {
    val columns:List<Column>

    fun mergeLists() = columns.map {
        "${it.keyName} = ${it.value}"
    }

    fun asAndConcatenatedString() = mergeLists().joinToString(" AND ")
    fun asCommaSeparatedString() = mergeLists().joinToString(", ")
}

data class DefaultHeaderValuePair(override val columns:List<Column>) : HeaderValuePair {
    constructor(vararg c:Column):this(c.asList())
}

data class UpdateSelectKeyTableData(val keyOfValuesToChange: HeaderValuePair, val changedValue: HeaderValuePair) :
    HeaderValuePair by keyOfValuesToChange {
}

interface Command {
    fun selectAll(selectAllTableData: SelectAllTableData): String
    fun selectKey(selectKeyTableData: HeaderValuePair): String
    fun deleteKey(selectKeyTableData: HeaderValuePair): String
    fun insert(insertTableData: InsertTableData): String
    fun createTable(createTableData: CreateTableData): String
    fun updateTable(updateSelectKeyTableData: UpdateSelectKeyTableData): String
}

class DefaultCommand(val tableName: String) : Command {
    override fun selectAll(selectAllTableData: SelectAllTableData): String {
        return "SELECT * FROM `${tableName}`;"
    }

    override fun selectKey(headerValuePair: HeaderValuePair): String {
        return "SELECT * FROM `${tableName}` WHERE ${headerValuePair.asAndConcatenatedString()};"
    }

    override fun deleteKey(headerValuePair: HeaderValuePair): String {
        return "DELETE FROM `${tableName}` WHERE ${headerValuePair.asAndConcatenatedString()};"
    }

    override fun insert(insertTableData: InsertTableData): String {
        return "INSERT INTO `${tableName}` (${insertTableData.header.toCommaSeparation()}) VALUES ${
            insertTableData.values.joinToString("), (", "(", ")") { it.toCommaSeparation() }
        };"
    }

    override fun createTable(createTableData: CreateTableData): String {
        return "CREATE TABLE IF NOT EXISTS `${tableName}`" +
                " (${createTableData.header.toCommaSeparation()}," +
                " PRIMARY KEY(${createTableData.primaryKey.toCommaSeparation()}));"
    }

    override fun updateTable(updateSelectKeyTableData: UpdateSelectKeyTableData): String {
        return "UPDATE `$tableName` SET ${updateSelectKeyTableData.changedValue.asCommaSeparatedString()} WHERE ${updateSelectKeyTableData.asAndConcatenatedString()};"
    }
}
