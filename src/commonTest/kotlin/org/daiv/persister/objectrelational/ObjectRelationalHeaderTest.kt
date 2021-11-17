package org.daiv.persister.objectrelational

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ObjectRelationalHeaderTest {
    data class TestSimple public constructor(val x: Int, val y: String) {

        companion object : ObjectRelationalMapper<TestSimple> {
            override fun hashCodeX(t: TestSimple) = t.x * 31
            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("x".headIntKey())
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headInt()),
                    listOf()
                )
            }
            override val objectRelationalWriter: ObjectRelationalWriter<TestSimple> by lazy {
                val keys = listOf("x".writeKey<TestSimple> { x })
                ObjectRelationalWriterData(
                    keys,
                    listOf("x".writeValue<TestSimple> { x }),
                    listOf()
                )
            }

            override val objectRelationalReader: ObjectRelationalReader<TestSimple> by lazy {
                val keys = listOf("x".readInt())
                ObjectRelationalReaderData(
                    "TestSimple",
                    keys,
                    listOf("x".readInt())
                ) { TestSimple(get(0), get(1)) }
            }

        }
    }

    data class TestComplex public constructor(val p: Int, val x: Float, val s: TestSimple) {

        companion object : ObjectRelationalMapper<TestComplex> {
            override fun hashCodeX(t: TestComplex) = t.p * 31
            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("p".headIntKey())
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("p".headInt()),
                    listOf({TestSimple.objectRelationalHeader})
                )
            }
            override val objectRelationalWriter: ObjectRelationalWriter<TestComplex> by lazy {
                val keys = listOf("p".writeKey<TestComplex> { p })
                ObjectRelationalWriterData(
                    keys,
                    listOf("p".writeValue<TestComplex> { p }),
                    listOf(TestSimple.writerMap { s })
                )
            }

            override val objectRelationalReader: ObjectRelationalReader<TestComplex> by lazy {
                val keys = listOf("p".readInt())
                ObjectRelationalReaderData(
                    "TestComplex",
                    keys,
                    listOf("p".readInt())
                ) { TestComplex(get(0), get(1), get(2)) }
            }

        }
    }

    data class TestListComplex public constructor(val p: Int, val x: Float, val list: List<TestSimple>) {

        companion object : ObjectRelationalMapper<TestListComplex> {
            override fun hashCodeX(t: TestListComplex) = t.p * 31
            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("p".headIntKey())
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("p".headInt()),
                    listOf({ listHeader(keyEntries.prefix("ref_"), TestSimple.objectRelationalHeader) })
                )
            }
            override val objectRelationalWriter: ObjectRelationalWriter<TestListComplex> by lazy {
                val keys = listOf("p".writeKey<TestListComplex> { p })
                ObjectRelationalWriterData(
                    keys,
                    listOf("p".writeValue<TestListComplex> { p }),
                    listOf(TestSimple.writerListMap { list })
                )
            }

            override val objectRelationalReader: ObjectRelationalReader<TestListComplex> by lazy {
                val keys = listOf("p".readInt())
                ObjectRelationalReaderData(
                    "TestListComplex",
                    keys,
                    listOf("p".readInt())
                ) { TestListComplex(get(0), get(1), get(2)) }
            }
        }
    }

    data class TestHashCode public constructor(val x: Int, val y: String) {

        companion object : ObjectRelationalMapper<TestHashCode> {
            override fun hashCodeX(t: TestHashCode) = t.x * 31
            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = hashCodeEntries()
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headInt(), "y".headString()),
                    listOf()
                )
            }
            override val objectRelationalWriter: ObjectRelationalWriter<TestHashCode> by lazy {
                val keys = HashCodeWriteEntry.asKey(TestHashCode)
                ObjectRelationalWriterData(
                    keys,
                    listOf("x".writeValue<TestHashCode> { x }, "y".writeValue<TestHashCode> { y }),
                    listOf()
                )
            }

            override val objectRelationalReader: ObjectRelationalReader<TestHashCode> by lazy {
                val keys = hashCodeRead()
                ObjectRelationalReaderData(
                    "TestHashCode",
                    keys,
                    listOf("x".readInt(), "y".readString())
                ) { TestHashCode(get(0), get(1)) }
            }
        }
    }

    fun ObjectRelationalHeader.createTable(): String {
        val head = keyHead(null) + head()
        val createTable = head.map { "${it.name} ${it.type}" }.joinToString(", ", "CREATE TABLE TestComplex (", ");")
        return createTable
    }

    val hashCodeCounterGetter = object : HashCodeCounterGetter {
        override fun <T> getCounter(
            objectRelationalWriter: ObjectRelationalWriter<T>,
            objectRelationalReader: ObjectRelationalReader<T>,
            hashCode: Int
        ): Int {
            return 0
        }
    }

    fun <T> ObjectRelationalWriter<T>.insert(t: T) {
        val ret = this.write(emptyList(), t, hashCodeCounterGetter)
        println("ret: $ret")
    }

    @Test
    fun testCreateTable() {
        val createTable = TestComplex.objectRelationalHeader.createTable()
        assertEquals("CREATE TABLE TestComplex (p Int, x Float, s_x Int);", createTable)
    }

    @Test
    fun testWriterWrite() {
        val obj = TestComplex(5, 0.2f, TestSimple(6, "Hello"))
        val writer = TestComplex.objectRelationalWriter
        val got = writer.write(emptyList(), obj, hashCodeCounterGetter)
        assertEquals(listOf(WriteRow(listOf(WriteEntry("x", 0.2f, false), WriteEntry("s_x", 6, false)))), got)
    }

    @Test
    fun testWriterWriteKey() {
        val obj = TestComplex(5, 0.2f, TestSimple(6, "Hello"))
        val writer = TestComplex.objectRelationalWriter
        val got = writer.writeKey(null, obj, hashCodeCounterGetter)
        assertEquals(listOf(WriteEntry("p", 5, true)), got)
    }

    @Test
    fun testReader() {
        val reader = TestSimple.objectRelationalReader
        val read = reader.read(ReadCollection(ListNativeReads(listOf(listOf(5, "Hello"))), object : DataRequester {
            override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
                TODO("Not yet implemented")
            }
        }))
        assertEquals(TestSimple(5, "Hello"), read)
    }

    @Test
    fun testReaderComplex() {
        val reader = TestComplex.objectRelationalReader
        val map = mapOf<Pair<List<ReadEntry>, ObjectRelationalReader<*>>, NativeReads>(
            (listOf(ReadEntry(5)) to TestSimple.objectRelationalReader) to ListNativeReads(listOf(listOf(5, "Hello")))
        )
        val read = reader.read(ReadCollection(ListNativeReads(listOf(listOf(6, 0.2f, 5))), object : DataRequester {
            override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
                return map[key to objectRelationalMapper]?.let {
                    objectRelationalMapper.read(ReadCollection(it, this))
                } ?: throw NullPointerException("no value for $key and $objectRelationalMapper")
            }
        }))
        assertEquals(TestComplex(6, 0.2f, TestSimple(5, "Hello")), read)
    }

    @Test
    fun testReaderList() {
        val reader = TestListComplex.objectRelationalReader
        val map = mapOf<Pair<List<ReadEntry>, ObjectRelationalReader<*>>, ListNativeReads>(
            (listOf(ReadEntry(5)) to TestSimple.objectRelationalReader) to ListNativeReads(listOf(listOf(5, "Hello"))),
            (listOf(ReadEntry(7)) to TestSimple.objectRelationalReader) to ListNativeReads(listOf(listOf(7, "World"))),
            (listOf(ReadEntry(6)) to ListObjectReader(
                TestListComplex.objectRelationalReader,
                emptyList(),
                TestSimple.objectRelationalReader
            )) to ListNativeReads(listOf(listOf(6, 1, 7), listOf(6, 0, 5))),
        )
        val read = reader.read(ReadCollection(ListNativeReads(listOf(listOf(6, 0.2f, 5))), object : DataRequester {
            override fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T {
                return map[key to objectRelationalMapper]?.let {
                    objectRelationalMapper.read(ReadCollection(it.copy(), this))
                } ?: throw NullPointerException("no value for $key and $objectRelationalMapper")
            }
        }))
        assertEquals(TestListComplex(6, 0.2f, listOf(TestSimple(5, "Hello"), TestSimple(7, "World"))), read)
    }

    @Test
    fun testListEquals() {
        val l = ListObjectReader(TestListComplex.objectRelationalReader, emptyList(), TestSimple.objectRelationalReader)
        val l2 =
            ListObjectReader(
                TestListComplex.objectRelationalReader,
                listOf(ReadEntryTask("5") { 6 }),
                TestSimple.objectRelationalReader
            )
        assertEquals(l, l2)
    }


    @Test
    fun testReadEntryTaskEquals() {
        val r1 = ReadEntryTask("hello") { 7 }
        val r2 = ReadEntryTask("hello") { 7 }
        assertEquals(r1, r1)
        assertNotEquals(r1, r2)
    }
}
