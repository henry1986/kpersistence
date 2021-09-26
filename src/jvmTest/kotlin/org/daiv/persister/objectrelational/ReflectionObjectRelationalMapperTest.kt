package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.test.assertEquals

class ReflectionObjectRelationalMapperTest {

    data class SimpleObject(val x: Int, val y: String)

    @Test
    fun testReflection() {
        val p = SimpleObject::class.declaredMemberProperties
        val s = SimpleObject(5, "World")
        val list = listOf(1, 5, 9)
        runBlocking {
            list.asFlow().map {
                GlobalScope.launch {
                    println("before delay $it")
                    delay(100)
                    println("after delay $it")
                    delay(100)
                    println("after delay $it")
                }
            }.toList().forEach { it.join() }
        }
    }

    @Test
    fun testSimpleHeader() {
        val mapper = SimpleObject::class.objectRelationMapper().objectRelationalHeader
        val head = listOf(HeadEntry("x", "Int", true), HeadEntry("y", "String", false))
        assertEquals(head, mapper.head())
        assertEquals(head.take(1), mapper.keyHead(null))
        assertEquals(head.take(1).map { it.copy("s_${it.name}") }, mapper.keyHead("s"))
    }

    @Test
    fun testWrite() {
        val mapper = SimpleObject::class.objectRelationMapper().objectRelationalWriter
        val s = SimpleObject(5, "Hello")
        val keys = mapper.writeKey(null, s, HashCodeCounterGetter.nullGetter)
        val others = mapper.write(emptyList(), s, HashCodeCounterGetter.nullGetter)
        assertEquals(listOf(WriteEntry("x", 5, true)), keys)
        assertEquals(listOf(WriteRow(listOf(WriteEntry("y", "Hello", false)))), others)
    }

    fun List<Any>.nativeReads() = object : NativeReads {
        var readIndex = 0
        fun read(): Any {
            val ret = get(readIndex)
            this.readIndex++
            return ret
        }

        override fun readInt(): Int {
            return read() as Int
        }

        override fun readBoolean(): Boolean {
            return read() as Boolean
        }

        override fun readDouble(): Double {
            return read() as Double
        }

        override fun readString(): String {
            return read() as String
        }

        override fun readByte(): Byte {
            return read() as Byte
        }

        override fun readFloat(): Float {
            return read() as Float
        }

        override fun readLong(): Long {
            return read() as Long
        }

        override fun readShort(): Short {
            return read() as Short
        }

        override fun readChar(): Char {
            return read() as Char
        }

        override fun nextRow(): Boolean {
            return false
        }
    }

    val dataRequester: DataRequester = object : DataRequester {
        override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
            return objectRelationalMapper.read(ReadCollection(listOf<Any>().nativeReads(), this))
        }
    }

    @Test
    fun testRead() {
        val reader = SimpleObject::class.objectRelationMapper().objectRelationalReader
        val read = listOf(5, "Hello")
        val readCollection = ReadCollection(read.nativeReads(), dataRequester)
        val key = reader.readKey(readCollection)
        assertEquals(listOf(ReadEntry(5)), key)
        val others = reader.read(ReadCollection(read.nativeReads(), dataRequester))
        assertEquals(SimpleObject(5, "Hello"), others)
    }
}