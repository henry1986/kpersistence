package org.daiv.sql

import mu.KotlinLogging
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.File
import java.sql.*


interface SimpleDatabase {
    /**
     * closes the database connection
     */
    fun close()

    /**
     * opens the database connection
     */
    fun open()

    /**
     * deletes the database file completely
     */
    fun delete(): Boolean

    fun deleteAndRestart():Boolean
}

interface DatabaseInterface : SimpleDatabase {
    val statement: Statement
    fun commit()
    fun identifier(): String
    fun tableNameEscapeSequence(): String
}
internal val dbOpen = MarkerFactory.getMarker("DB_OPEN")
internal val dbClose = MarkerFactory.getMarker("DB_CLOSE")
private val persisterMarker = MarkerFactory.getMarker("Persister")

class ReadWriter(val databaseInterface: DatabaseInterface){
    companion object {
        private val logger = KotlinLogging.logger { }
    }
    val dbMarkerRead: Marker = MarkerFactory.getDetachedMarker("READ")
    val dbMarkerWrite = MarkerFactory.getDetachedMarker("WRITE")
    val dbMarkerCreate = MarkerFactory.getDetachedMarker("CREATE")

    internal fun <T : Any> read(query: String, func: (ResultSet) -> T): T {
        try {

            logger.trace(dbMarkerRead, query)
            val statement = databaseInterface.statement
            logger.debug(query)
            val result = statement.executeQuery(query)
            val ret = func(result)
            result.close()
            statement.close()
            return ret
        } catch (e: SQLException) {
            throw RuntimeException("query: $query", e)
        }
    }

//    internal fun read(query: String): ResultSet {
//        try {
//
//            logger.trace(dbMarkerRead, query)
//            val statement = databaseInterface.statement
//            logger.debug(query)
//            val result = statement.executeQuery(query)
////            result.close()
////            statement.close()
//            return result
//        } catch (e: SQLException) {
//            throw RuntimeException("query: $query", e)
//        }
//    }

    internal fun write(query: String) {
        try {
            if (query.startsWith("CREATE")) {
                logger.debug(dbMarkerCreate, query)
            } else {
                logger.debug(dbMarkerWrite, query)
            }
            logger.trace { "execute query: $query" }
            val statement = databaseInterface.statement
            logger.trace { "got statement" }
            statement.execute(query)
            logger.trace { "executed query" }
            statement.close()
            logger.trace { "close statement" }
        } catch (e: SQLException) {
            throw RuntimeException("query: $query", e)
        }
    }

}

/**
 * class that automatically opens a connection - to create a new connection,
 * a new instance must be created. Calling [open] does nothing
 */
class DatabaseHandler constructor(val path: String) : DatabaseInterface {
    override fun identifier() = path
    override fun tableNameEscapeSequence() = "`"

    val logger = KotlinLogging.logger {}

    private lateinit var connection: Connection

    private fun initConnection(){
        try {
            Class.forName("org.sqlite.JDBC")
        } catch (e: ClassNotFoundException) {
            throw e
        }
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:$path")
            if (!connection!!.isClosed) {
                logger.info(dbOpen, "...Connection established to $path")
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }
    }

    init {
        initConnection()
        Runtime.getRuntime()
            .addShutdownHook(object : Thread() {
                override fun run() {
                    close()
                }
            })
    }

    override val statement: Statement
        get() {
            try {
                if (connection == null) {
                    throw NullPointerException("Database connection to $path not opened")
                }
                return connection!!.createStatement()
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }

        }

    override fun close() {
        try {
            if (!connection!!.isClosed && connection != null) {
                connection!!.close()
                if (connection!!.isClosed)
                    logger.info(dbClose, "Connection to database $path closed")
            }
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    }

    override fun open() {
    }

    override fun deleteAndRestart():Boolean{
        connection.close()
        val ret = File(path).delete()
        initConnection()
        return ret
    }

    override fun delete(): Boolean {
        return File(path).delete()
    }

    override fun commit() {
        try {
            connection!!.commit()
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    }
}
