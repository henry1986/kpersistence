package org.daiv.persister.sql.command

import org.daiv.persister.*


inline fun <reified T : Any> CommandReceiver.table(
    tableName: String = T::class.simpleName!!,
    moreKeysData: MoreKeysData,
    members: List<MemberValueGetter<T, *>>,
    valueFactory: ValueFactory<T>
) = DefaultTable(
    tableName,
    this,
    DefaultCommandImplementer(
        tableName,
        DefaultFlatClass(T::class, moreKeysData, members).createObjectType(valueFactory)
    )
)

data class DefaultCommandImplementer<MEMBER : Any>(
    val tableName: String,
    override val obj: ObjectTypeHandler<MEMBER>,
    override val command: Command = DefaultCommand(tableName),
) : CommandImplementer<MEMBER>

interface CommandReceiver {
    fun write(string: String)
    fun <T : Any> read(query: String, func: (DatabaseReader) -> T): T
}

interface CommandImplementer<MEMBER : Any> {
    val command: Command
    val obj: ObjectTypeHandler<MEMBER>

    fun persist(): String {
        return command.createTable(CreateTableData(obj.toHeader(), obj.keyNames()))
    }

    fun insert(list: List<MEMBER>): String {
        return command.insert(InsertTableData(obj.insertHead(), list.map { obj.insertValue(it) }))
    }

    fun select(key: List<Any>): String {
        throw RuntimeException()
//        return command.selectKey(DefaultHeaderValuePair(obj.keyNames(), obj.mapValueToRow( key)))
    }
}

class DefaultTable<MEMBER : Any>(
    override val tableName: String,
    private val commandReceiver: CommandReceiver,
    val commandImplementer: CommandImplementer<MEMBER>
) : Table<MEMBER> {
    override fun persist() {
        commandReceiver.write(commandImplementer.persist())
    }

    override fun insert(list: List<MEMBER>) {
        TODO("Not yet implemented")
    }

    override fun select(key: List<Any?>): List<MEMBER> {
        TODO("Not yet implemented")
    }

    override fun selectAll(): List<MEMBER> {
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
    val tableName: String
    fun persist()
    fun insert(list: List<HOLDER>)
    fun select(key: List<Any?>): List<HOLDER>
    fun selectAll(): List<HOLDER>
    fun delete(key: List<Any?>)
    fun update(updateData: UpdateData)
}