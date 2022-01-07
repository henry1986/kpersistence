package org.daiv.persister.objectrelational

import org.daiv.persister.MoreKeysData
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ObjectRelationalHeaderTest {
    data class TestSimple public constructor(val x: Int, val y: String) {

        companion object : ObjectRelationalMapper<TestSimple> {
            override fun hashCodeX(t: TestSimple) = t.x * 31

            val cgh by lazy { CodeGenHelper(TestSimple::class, emptyMap()) }
            val p1 = cgh.simpleKey<Int>("x")
            val p2 = cgh.simple<String>("y")
            override val classHeaderData: ClassHeaderData =
                ClassHeaderData(
                    cgh.clazz,
                    listOf(p1, p2),
                    MoreKeysData(1)
                )

            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("x".headIntKey(p1))
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headInt(p2)),
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
                    TestSimple::class,
                    keys,
                    listOf("x".readInt())
                ) { TestSimple(get(0), get(1)) }
            }
        }
    }

    data class TestComplex constructor(val p: Int, val x: Float, val s: TestSimple) {

        companion object : ObjectRelationalMapper<TestComplex> {
            override fun hashCodeX(t: TestComplex) = t.p * 31

            val cgh by lazy {
                CodeGenHelper(
                    TestComplex::class,
                    mapOf(TestSimple::class to { TestSimple.classHeaderData })
                )
            }
            val p1 = cgh.simpleKey<Int>("p")
            val p2 = cgh.simple<Float>("x")
            val p3 = cgh.simple<TestSimple>("s")
            override val classHeaderData: ClassHeaderData =
                ClassHeaderData(
                    cgh.clazz,
                    listOf(p1, p2, p3),
                    MoreKeysData(1)
                )

            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("p".headIntKey(p1))
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headFloat(p2)) + TestSimple.objectRelationalHeader.keyHead("s", p3),
                    listOf({ TestSimple.objectRelationalHeader })
                )
            }

            override val objectRelationalWriter: ObjectRelationalWriter<TestComplex> by lazy {
                val keys = listOf("p".writeKey<TestComplex> { p })
                ObjectRelationalWriterData(
                    keys,
                    listOf("p".writeValue { p }),
                    listOf(TestSimple.writerMap { s })
                )
            }

            override val objectRelationalReader: ObjectRelationalReader<TestComplex> by lazy {
                val keys = listOf("p".readInt())
                ObjectRelationalReaderData(
                    TestComplex::class,
                    keys,
                    listOf("p".readInt())
                ) { TestComplex(get(0), get(1), get(2)) }
            }

        }
    }

    data class TestListComplex constructor(val p: Int, val x: Float, val list: List<TestSimple>) {

        companion object : ObjectRelationalMapper<TestListComplex> {
            override fun hashCodeX(t: TestListComplex) = t.p * 31

            val cgh by lazy {
                CodeGenHelper(
                    TestListComplex::class,
                    mapOf(TestSimple::class to { TestSimple.classHeaderData })
                )
            }
            val p1 = cgh.simpleKey<Int>("p")
            val p2 = cgh.simple<Float>("x")
            val p3 = cgh.list<TestSimple>("list")
            override val classHeaderData: ClassHeaderData =
                ClassHeaderData(
                    cgh.clazz,
                    listOf(p1, p2, p3),
                    MoreKeysData(1)
                )

            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = listOf("p".headIntKey(p1))
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headFloat(p2)),
                    listOf({ listHeader(keyEntries.prefix("ref_", p3), TestSimple.objectRelationalHeader) })
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
                    TestListComplex::class,
                    keys,
                    listOf("p".readInt())
                ) { TestListComplex(get(0), get(1), get(2)) }
            }
        }
    }

    data class TestHashCode public constructor(val x: Int, val y: String) {

        companion object : ObjectRelationalMapper<TestHashCode> {
            override fun hashCodeX(t: TestHashCode) = t.x * 31
            val cgh by lazy { CodeGenHelper(TestHashCode::class, emptyMap()) }

            val p1 = cgh.simpleKey<Int>("x")
            val p2 = cgh.simple<String>("y")
            override val classHeaderData: ClassHeaderData =
                ClassHeaderData(
                    cgh.clazz,
                    listOf(p1, p2),
                    MoreKeysData(1)
                )
            override val objectRelationalHeader: ObjectRelationalHeader by lazy {
                val keyEntries = hashCodeEntries(p1)
                ObjectRelationalHeaderData(
                    keyEntries,
                    listOf("x".headInt(p1), "y".headString(p2)),
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
                    TestHashCode::class,
                    keys,
                    listOf("x".readInt(), "y".readString())
                ) { TestHashCode(get(0), get(1)) }
            }
        }
    }

    fun ObjectRelationalHeader.createTable(parameter: Parameter): String {
        val head = keyHead(null, parameter) + headOthers()
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
        val createTable = TestComplex.objectRelationalHeader.createTable(
            SimpleParameter(
                TestComplex::class, "p", typeOf<Int>(), KeyType.NORM, CodeGenHelper(
                    TestComplex::class,
                    mapOf()
                ).map
            )
        )
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
