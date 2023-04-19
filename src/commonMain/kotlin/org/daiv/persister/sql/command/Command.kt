package org.daiv.persister.sql.command

import org.daiv.persister.Row

data class InsertTableData(val header: Row, val values: List<Row>)
data class CreateTableData(val header: Row, val primaryKey: Row)
object SelectAllTableData

/**
 * Represents a column for SQL queries.
 *
 * @property keyName The name of the column.
 * @property value The value of the column.
 */
data class Column(val keyName: String, val value: String)

/**
 * Represents a header-value pair in a SQL query.
 */
interface HeaderValuePair {
    val columns: List<Column>

    /**
     * Converts the list of columns to a list of key-value pairs separated by "=".
     *
     * @return A list of key-value pairs separated by "=".
     */
    fun toKeyValuePairs() = columns.map { "${it.keyName} = ${it.value}" }

    /**
     * Joins the key-value pairs in the list with the word "AND".
     *
     * @return A string that represents the joined key-value pairs with "AND".
     */
    fun asAndConcatenatedString() = toKeyValuePairs().joinToString(" AND ")

    /**
     * Joins the key-value pairs in the list with a comma.
     *
     * @return A string that represents the joined key-value pairs with a comma.
     */
    fun asCommaSeparatedString() = toKeyValuePairs().joinToString(", ")
}
data class DefaultHeaderValuePair(override val columns:List<Column>) : HeaderValuePair {
    constructor(vararg c:Column):this(c.asList())
}

data class UpdateSelectKeyTableData(val keyOfValuesToChange: HeaderValuePair, val changedValue: HeaderValuePair) :
    HeaderValuePair by keyOfValuesToChange {
}

/**
 * The `Command` interface defines the methods that must be implemented by any database command.
 *
 * The interface provides methods for selecting and modifying database tables, creating new tables, and performing
 * various other database operations. Implementing classes must provide an implementation for each of the interface
 * methods.
 */
interface Command {
    /**
     * Generates a SQL `SELECT` statement that selects all rows and columns from a table.
     *
     * @param selectAllTableData An object that contains information about the table to select from.
     * @return A SQL `SELECT` statement.
     */
    fun selectAll(selectAllTableData: SelectAllTableData): String

    /**
     * Generates a SQL `SELECT` statement that selects a row from a table based on a given set of column values.
     *
     * @param selectKeyTableData An object that contains information about the table and columns to select from.
     * @return A SQL `SELECT` statement.
     */
    fun selectKey(selectKeyTableData: HeaderValuePair): String

    /**
     * Generates a SQL `DELETE` statement that deletes a row from a table based on a given set of column values.
     *
     * @param selectKeyTableData An object that contains information about the table and columns to delete from.
     * @return A SQL `DELETE` statement.
     */
    fun deleteKey(selectKeyTableData: HeaderValuePair): String

    /**
     * Generates a SQL `INSERT` statement that inserts a row of data into a table.
     *
     * @param insertTableData An object that contains information about the table and data to insert.
     * @return A SQL `INSERT` statement.
     */
    fun insert(insertTableData: InsertTableData): String

    /**
     * Generates a SQL `CREATE TABLE` statement that creates a new table in the database.
     *
     * @param createTableData An object that contains information about the table to create.
     * @return A SQL `CREATE TABLE` statement.
     */
    fun createTable(createTableData: CreateTableData): String

    /**
     * Generates a SQL `UPDATE` statement that updates a row in a table based on a given set of column values.
     *
     * @param updateSelectKeyTableData An object that contains information about the table and columns to update.
     * @return A SQL `UPDATE` statement.
     */
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
