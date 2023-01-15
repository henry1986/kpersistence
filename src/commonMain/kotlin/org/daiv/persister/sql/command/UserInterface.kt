package org.daiv.persister.sql.command

import org.daiv.persister.DatabaseReader
import org.daiv.persister.memberValueGetter


inline fun <reified T : Any> CommandReceiver.table(tableName: String = T::class.simpleName!!) =
    DefaultTable<T>(tableName, this)

interface CommandReceiver {
    fun write(string: String)
    fun <T : Any> read(query: String, func: (DatabaseReader) -> T): T
}

class DefaultTable<HOLDER>(val tableName: String, val commandReceiver: CommandReceiver) : Table<HOLDER> {
    val command = DefaultCommand(tableName)
    override fun persist() {
        TODO("Not yet implemented")
//        commandReceiver.write(command.createTable(CreateTableData()))
    }

    override fun insert(list: List<HOLDER>) {
        TODO("Not yet implemented")
    }

    override fun select(key: List<Any?>): List<HOLDER> {
        TODO("Not yet implemented")
    }

    override fun selectAll(): List<HOLDER> {
        TODO("Not yet implemented")
    }

    override fun delete(key: List<Any?>) {
        TODO("Not yet implemented")
    }

    override fun update(updateData: UpdateData) {
        TODO("Not yet implemented")
    }

}

data class UpdateData(
    val fieldsToUpdate: List<String>,
    val newValues: List<Any?>,
    val key: List<Any>,
    val keyFields: List<Any?>? = null
)

interface Table<HOLDER> {
    fun persist()
    fun insert(list: List<HOLDER>)
    fun select(key: List<Any?>): List<HOLDER>
    fun selectAll(): List<HOLDER>
    fun delete(key: List<Any?>)
    fun update(updateData: UpdateData)
}