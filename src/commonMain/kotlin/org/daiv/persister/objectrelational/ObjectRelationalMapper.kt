package org.daiv.persister.objectrelational

import org.daiv.coroutines.CalculationSuspendableMap
import org.daiv.coroutines.DefaultScopeContextable
import org.daiv.coroutines.ScopeContextable
import org.daiv.persister.MoreKeysData
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

fun Boolean.hashCodeX() = if (this) 1231 else 1237
fun Long.hashCodeX() = (this xor (this shr 32)).toInt()
fun Double.hashCodeX() = toRawBits().hashCodeX()
fun String.hashCodeX(): Int {
    var hash = 1
    for (i in (indices)) {
        hash += get(i).code * 31
    }
    return hash
}


interface IsKeyInterface<T> {
    val isKey: Boolean
    fun rebuild(isKey: Boolean): T
}


interface NoKeyEntry<T> : IsKeyInterface<T> {
    fun noKey(): T = rebuild(false)
    fun asKey(): T = rebuild(true)
}

data class HeadEntry(
    val parameterList: List<Parameter>,
    val name: String,
    val type: String,
    override val isKey: Boolean
) :
    NoKeyEntry<HeadEntry>,
    PrefixBuilder {

    constructor(parameter: Parameter, name: String, type: String, isKey: Boolean) : this(
        listOf(parameter),
        name,
        type,
        isKey
    )

    val current
        get() = parameterList.last()

    override fun rebuild(isKey: Boolean): HeadEntry = copy(isKey = isKey)

    fun withPrefix(prefix: String?, parameter: Parameter?) =
        copy(parameterList = parameter?.let {
            listOf(it) + parameterList
        } ?: parameterList, name = prefix.build(name))
}

data class WriteEntry(val name: String, val value: Any?, override val isKey: Boolean) : NoKeyEntry<WriteEntry> {
    override fun rebuild(isKey: Boolean) = copy(isKey = isKey)
}

data class WriteRow(val writeEntries: List<out WriteEntry>) {
    operator fun plus(entries: List<WriteEntry>): WriteRow {
        return WriteRow(writeEntries + entries)
    }
}

fun <T> List<out NoKeyEntry<T>>.noKey() = map { it.noKey() }
fun <T> List<out NoKeyEntry<T>>.asKey() = map { it.asKey() }

data class ReadEntry(val any: Any?)

interface HashCodeCounterGetter {
    fun <T> getCounter(
        objectRelationalWriter: ObjectRelationalWriter<T>,
        objectRelationalReader: ObjectRelationalReader<T>,
        hashCode: Int
    ): Int

    companion object {
        val nullGetter = object : HashCodeCounterGetter {
            override fun <T> getCounter(
                objectRelationalWriter: ObjectRelationalWriter<T>,
                objectRelationalReader: ObjectRelationalReader<T>,
                hashCode: Int
            ): Int {
                return 0
            }
        }
    }
}

interface PreWriteEntry<T> : NoKeyEntry<PreWriteEntry<T>> {
    val parameter: List<Parameter>
    fun writeEntry(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): Sequence<WriteEntry>
    fun <R> map(prefix: String?, isKey: Boolean, transform: R.() -> T): PreWriteEntry<R>
}

interface ClassParseable {
    fun String?.isNative() = when (this) {
        "Int", "Long", "Short", "Double", "Float", "Char", "Boolean", "String" -> true
        else -> false
    }

    fun String?.isCollection() = when (this) {
        "List", "Set", "Map" -> true
        else -> false
    }

    fun String?.isList() = when (this) {
        "List" -> true
        else -> false
    }

    fun String?.isSet() = when (this) {
        "Set" -> true
        else -> false
    }

    fun String?.isMap() = when (this) {
        "Map" -> true
        else -> false
    }
}

class CHDMap<T : ClassHeaderData>(
    val scopeContextable: ScopeContextable = DefaultScopeContextable()
    val chdFactory: (KClass<*>, CHDMap<T>) -> T,
) : ClassParseable {
    val calculationCollection: CalculationSuspendableMap<KClass<*>, ClassHeaderData> =
        CalculationSuspendableMap("") {
            val header = chdFactory(it, this)
            header.dependentClasses().map { launch(it) }
            header
        }

    private fun launch(clazz: KClass<*>) {
        calculationCollection.launchOnNotExistence(clazz) {}
    }

    suspend fun getValue(clazz: KClass<*>): ClassHeaderData {
        return calculationCollection.getValue(clazz)
    }

    suspend fun getAndJoin(clazz: KClass<*>): ClassHeaderData {
        val value = getValue(clazz)
        join()
        return value
    }

    suspend fun join() {
        calculationCollection.join()
    }

    fun directGet(clazz: KClass<*>): ClassHeaderData {
        return calculationCollection.tryDirectGet(clazz)
            ?: throw NullPointerException("for class $clazz is no classHeaderData created")
    }
}

class DefaultClassHeaderData(
    override val clazz: KClass<*>,
    override val parameters: List<Parameter>,
    override val moreKeys: MoreKeysData
) : ClassHeaderData {
    override val keyParameters = parameters.take(moreKeys.amount)
    override val otherParameters = parameters.drop(moreKeys.amount)
    override fun keyHead(prefix: String?, isKey: Boolean, parameter: List<Parameter>): List<HeadEntry> {
        return keyParameters.flatMap { it.head(prefix, isKey, parameter) }
    }

    override fun head(prefix: String?, isKey: Boolean, parameter: List<Parameter>): List<HeadEntry> {
        return otherParameters.flatMap { it.head(prefix, isKey, parameter) }
    }

    override fun dependentClasses(): List<KClass<*>> = parameters.flatMap { it.dependentClasses() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClassHeaderData

        if (clazz != other.clazz) return false

        return true
    }

    override fun hashCode(): Int {
        return clazz.hashCode()
    }
}


interface ClassHeaderData {
    val clazz: KClass<*>
    val parameters: List<Parameter>
    val moreKeys: MoreKeysData
    val keyParameters: List<Parameter>
    val otherParameters: List<Parameter>
    fun keyHead(prefix: String?, isKey: Boolean, parameter: List<Parameter>): List<HeadEntry>

    fun head(prefix: String?, isKey: Boolean, parameter: List<Parameter>): List<HeadEntry>

    fun dependentClasses(): List<KClass<*>>
}


interface ObjectRelationalMapper<T> : ClassParseable {
    fun hashCodeX(t: T): Int

    val classHeaderData: ClassHeaderData
    val objectRelationalHeader: ObjectRelationalHeader
    val objectRelationalWriter: ObjectRelationalWriter<T>
    val objectRelationalReader: ObjectRelationalReader<T>

    fun hashCodeRead() = listOf("autoId".readInt(), "autoId_counter".readInt())
    fun hashCodeEntries(parameter: Parameter) =
        listOf(HeadEntry(parameter, "autoId", "Int", true), HeadEntry(parameter, "autoId_counter", "Int", true))

    fun <T> String.writeKey(func: T.() -> Any?) = DefaultPreWriteEntry(this, true, func)
    fun <T> String.writeValue(func: T.() -> Any?) = DefaultPreWriteEntry(this, false, func)

    fun <R, T> String.writeValue(objectRelationalWriter: ObjectRelationalWriter<R>, func: T.() -> R) =
        (objectRelationalWriter.preWriteKey(this, false, func))

    fun <R, T> String.writeValue(mapper: ObjectRelationalMapper<R>, func: T.() -> R) =
        (mapper.objectRelationalWriter.preWriteKey(this, false, func))

    fun <R, T> String.writeKey(objectRelationalWriter: ObjectRelationalWriter<R>, func: T.() -> R) =
        (objectRelationalWriter.preWriteKey(this, true, func))

    fun <R, T> String.writeKey(mapper: ObjectRelationalMapper<R>, func: T.() -> R) =
        (mapper.objectRelationalWriter.preWriteKey(this, true, func))

    fun <T, R> ObjectRelationalMapper<T>.writerMap(map: R.() -> T) =
        ObjectRelationalWriterReceiverMap({ this.objectRelationalWriter }, map)

    fun <T, R> ObjectRelationalMapper<T>.writerListMap(map: R.() -> List<T>) =
        ObjectRelationalWriterMap({ ListObjectWriter { this.objectRelationalWriter } }, map)

    fun <T, R> ObjectRelationalWriter<T>.writerListMap(map: R.() -> List<T>) =
        ObjectRelationalWriterMap({ ListObjectWriter { this } }, map)

    fun String.readInt() = ReadEntryTask(this) { nativeReads.readInt() }
    fun String.readString() = ReadEntryTask(this) { nativeReads.readString() }
    fun String.readDouble() = ReadEntryTask(this) { nativeReads.readDouble() }
    fun String.readFloat() = ReadEntryTask(this) { nativeReads.readFloat() }
    fun String.readChar() = ReadEntryTask(this) { nativeReads.readChar() }
    fun String.readShort() = ReadEntryTask(this) { nativeReads.readShort() }
    fun String.readLong() = ReadEntryTask(this) { nativeReads.readLong() }

    fun <T> String.request(objectRelationalReader: ObjectRelationalReader<T>) =
        ReadEntryTask(this) { requestValue(objectRelationalReader) }

    fun <T> String.requestList(mapper: ObjectRelationalMapper<T>, keys: List<ReadEntryTask>) =
        ReadEntryTask(this) {
            val reader = ListObjectReader(
                this@ObjectRelationalMapper.objectRelationalReader,
                keys,
                mapper.objectRelationalReader
            )
            dataRequester.requestData(it, reader)
        }

    fun String.headValue(header: ObjectRelationalHeader, parameter: Parameter) =
        header.keyHead(this, parameter).noKey()

    fun <T> String.headKey(mapper: ObjectRelationalMapper<T>, parameter: Parameter) =
        mapper.objectRelationalHeader.keyHead(this, parameter).asKey()

    fun <T> ObjectRelationalMapper<T>.headList(keys: List<HeadEntry>, parameter: Parameter) =
        listHeader(keys.prefix("ref_", parameter), this.objectRelationalHeader)

    fun String.headInt(parameter: Parameter) = HeadEntry(parameter, this, "Int", false)
    fun String.headString(parameter: Parameter) = HeadEntry(parameter, this, "String", false)
    fun String.headDouble(parameter: Parameter) = HeadEntry(parameter, this, "Double", false)
    fun String.headFloat(parameter: Parameter) = HeadEntry(parameter, this, "Float", false)
    fun String.headChar(parameter: Parameter) = HeadEntry(parameter, this, "Char", false)
    fun String.headShort(parameter: Parameter) = HeadEntry(parameter, this, "Short", false)
    fun String.headLong(parameter: Parameter) = HeadEntry(parameter, this, "Long", false)

    fun String.headIntKey(parameter: Parameter) = HeadEntry(parameter, this, "Int", true)
    fun String.headStringKey(parameter: Parameter) = HeadEntry(parameter, this, "String", true)
    fun String.headDoubleKey(parameter: Parameter) = HeadEntry(parameter, this, "Double", true)
    fun String.headFloatKey(parameter: Parameter) = HeadEntry(parameter, this, "Float", true)
    fun String.headCharKey(parameter: Parameter) = HeadEntry(parameter, this, "Char", true)
    fun String.headShortKey(parameter: Parameter) = HeadEntry(parameter, this, "Short", true)
    fun String.headLongKey(parameter: Parameter) = HeadEntry(parameter, this, "Long", true)
}

data class HashCodeWriteEntry<T, S> private constructor(
    override val isKey: Boolean,
    private val objectRelationalWriter: ObjectRelationalWriter<S>,
    private val objectRelationalReader: ObjectRelationalReader<S>,
    private val getHashCode: T.() -> Int,
) : PrefixBuilder, PreWriteEntry<T> {

    private val name = "autoId"
    private val counterName = "autoId_counter"

    override fun writeEntry(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): Sequence<WriteEntry> {
        val hashCode = t.getHashCode()
        val counter = hashCodeCounterGetter.getCounter(objectRelationalWriter, objectRelationalReader, hashCode)
        return sequenceOf(
            WriteEntry(prefix.build(name), hashCode, isKey),
            WriteEntry(prefix.build(counterName), counter, isKey)
        )
    }

    override fun <R> map(prefix: String?, isKey: Boolean, transform: R.() -> T): PreWriteEntry<R> {
        return HashCodeWriteEntry(isKey, objectRelationalWriter, objectRelationalReader) { transform().getHashCode() }
    }

    override fun rebuild(isKey: Boolean) = copy(isKey = isKey)

    companion object {
        fun <T> asKey(
            objectRelationalMapper: ObjectRelationalMapper<T>,
        ) = listOf(HashCodeWriteEntry<T, T>(
            true,
            objectRelationalMapper.objectRelationalWriter,
            objectRelationalMapper.objectRelationalReader
        ) { objectRelationalMapper.hashCodeX(this) })
    }
}

data class DefaultPreWriteEntry<T>(
    override val parameter: List<Parameter>,
    override val isKey: Boolean,
    val func: T.() -> Any?
) : PrefixBuilder, PreWriteEntry<T> {
    val name: String = parameter.joinToString("_") { it.name }
    override fun writeEntry(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): Sequence<WriteEntry> {
        return sequenceOf(WriteEntry(prefix.build(name), t.func(), isKey))
    }

    override fun <R> map(prefix: String?, isKey: Boolean, transform: R.() -> T): PreWriteEntry<R> {
        return DefaultPreWriteEntry(parameter, prefix.build(name), isKey) { transform().func() }
    }

    override fun rebuild(isKey: Boolean) = copy(isKey = isKey)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DefaultPreWriteEntry<*>

        if (name != other.name) return false
        if (isKey != other.isKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isKey.hashCode()
        return result
    }

    override fun toString(): String {
        return "DefaultPreWriteEntry('$name', ${if (isKey) "isKey" else "noKey"})"
    }

}

interface TaskReceiver {
    suspend fun <R> task(r: R, higherKeys: List<WriteEntry>, mapper: RowWriter<R>)
}

interface PlainTaskReceiver {
    fun task(task: (ObjectRelationalHeader) -> Unit, mapper: ObjectRelationalHeader)
}

interface DataRequester {
    fun <T> requestData(key: List<ReadEntry>, objectRelationalMapper: ObjectRelationalReader<T>): T
}

interface NativeReads {
    fun readInt(): Int

    fun readBoolean(): Boolean

    fun readDouble(): Double

    fun readString(): String

    fun readByte(): Byte

    fun readFloat(): Float

    fun readLong(): Long

    fun readShort(): Short

    fun readChar(): Char

    fun nextRow(): Boolean
}

interface ObjectRelationalReader<T> {
    fun read(readCollection: ReadCollection): T
    fun readKey(readCollection: ReadCollection): List<ReadEntry>

}

interface PrefixBuilder {
    fun String?.build(s: String) = this?.let { "${it}_$s" } ?: s
}

interface ObjectRelationalHeader : PrefixBuilder {
    fun headOthers(): List<HeadEntry>
    fun keyHead(prefix: String?, parameter: Parameter?): List<HeadEntry>
    fun allHeads(prefix: String?, parameter: Parameter?) = keyHead(prefix, parameter) + headOthers()
    fun subHeader(plainTaskReceiver: PlainTaskReceiver, task: (ObjectRelationalHeader) -> Unit)
}

interface KeyWriter<T> {
    fun writeKey(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry>
}

interface RowWriter<T> {
    fun write(higherKeys: List<WriteEntry>, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteRow>
}

interface ObjectRelationalWriter<T> : PrefixBuilder, KeyWriter<T>, RowWriter<T> {
    fun singleRow(vararg elements: WriteEntry) = listOf(WriteRow(elements.toList()))
    fun singleRow(list: List<WriteEntry>) = listOf(WriteRow(list))
    fun row(vararg elements: WriteEntry) = WriteRow(elements.toList())


    fun writeRow(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry>

    fun <R> preWriteKey(prefix: String?, isKey: Boolean, func: R.() -> T): List<PreWriteEntry<R>>
    suspend fun subs(t: T, taskReceiver: TaskReceiver, hashCodeCounterGetter: HashCodeCounterGetter)
}

fun List<HeadEntry>.prefix(prefix: String?, parameter: Parameter?) = map { it.withPrefix(prefix, parameter) }

data class ObjectRelationalHeaderData(
    val keyEntries: List<HeadEntry>,
    val others: List<HeadEntry>,
    val headers: List<() -> ObjectRelationalHeader>
) : ObjectRelationalHeader {
    override fun headOthers(): List<HeadEntry> {
        return others
    }

    override fun keyHead(prefix: String?, parameter: Parameter?): List<HeadEntry> {
        return keyEntries.prefix(prefix, parameter)
    }

    override fun subHeader(plainTaskReceiver: PlainTaskReceiver, task: (ObjectRelationalHeader) -> Unit) {
        headers.forEach {
            plainTaskReceiver.task(task, it())
        }
//                plainTaskReceiver.task(task, TestXZ)
    }
}

interface ORWriterMap<R> {
    suspend fun sub(r: R, keys: List<WriteEntry>, taskReceiver: TaskReceiver)
}

data class ObjectRelationalWriterMap<R, T>(
    val objectRelationalWriter: suspend () -> RowWriter<T>,
    val func: R.() -> T
) :
    ORWriterMap<R> {
    override suspend fun sub(r: R, keys: List<WriteEntry>, taskReceiver: TaskReceiver) {
        taskReceiver.task(r.func(), keys, objectRelationalWriter())
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ObjectRelationalWriterMap<*, *>

        if (objectRelationalWriter != other.objectRelationalWriter) return false

        return true
    }

    override fun hashCode(): Int {
        return objectRelationalWriter.hashCode()
    }

    override fun toString(): String {
        return "ObjectRelationalWriterMap($objectRelationalWriter)"
    }

}

data class ObjectRelationalWriterReceiverMap<R, T>(
    val objectRelationalWriter: () -> ObjectRelationalWriter<T>,
    val func: R.() -> T
) : ORWriterMap<R> {
    override suspend fun sub(r: R, keys: List<WriteEntry>, taskReceiver: TaskReceiver) {
        taskReceiver.task(r.func(), keys, objectRelationalWriter())
    }
}


//data class HashCodeWriter<T>(
//    val hashCodeGetter: (HashCodeCounterGetter) -> List<WriteEntry>,
//    val others: List<PreWriteEntry<T>>,
//    val list: List<ObjectRelationalWriterMap<T, *>>
//) : ObjectRelationalWriter<T>{
//    override fun write(higherKeys: List<WriteEntry>, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteRow> {
//        return singleRow(others.flatMap { it.writeEntry(null, t, hashCodeCounterGetter) })
//    }
//
//    override fun writeKey(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry> {
//        return keys.map { it.writeEntry(prefix, t, hashCodeCounterGetter) }
//    }
//
//    override fun subs(t: T, taskReceiver: TaskReceiver, hashCodeCounterGetter: HashCodeCounterGetter) {
//        list.forEach {
//            it.sub(t, writeKey(null, t, hashCodeCounterGetter), taskReceiver)
//        }
//    }
//
//    override fun <R> preWriteKey(prefix: String?, func: R.() -> T): List<PreWriteEntry<R>> {
//        return hashCodeGetter.map { it.map(prefix, func) }
//    }
//}
data class ObjectRelationalWriterData<T>(
    val keys: List<PreWriteEntry<T>>,
    val others: List<PreWriteEntry<T>>,
    val list: List<ORWriterMap<T>>
) : ObjectRelationalWriter<T> {

    /**
     * writes only the values, that are noKeys. To write keys, use #writeKey
     */
    override fun write(
        higherKeys: List<WriteEntry>,
        t: T,
        hashCodeCounterGetter: HashCodeCounterGetter
    ): List<WriteRow> {
        return singleRow(others.flatMap { it.writeEntry(null, t, hashCodeCounterGetter) })
    }

    override fun writeRow(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry> {
        return (keys + others).flatMap { it.writeEntry(prefix, t, hashCodeCounterGetter) }
    }

    /**
     * writes only the keys. To write no key values, use #write
     */
    override fun writeKey(prefix: String?, t: T, hashCodeCounterGetter: HashCodeCounterGetter): List<WriteEntry> {
        return keys.flatMap { it.writeEntry(prefix, t, hashCodeCounterGetter) }
    }

    override suspend fun subs(t: T, taskReceiver: TaskReceiver, hashCodeCounterGetter: HashCodeCounterGetter) {
        list.forEach { o ->
            t?.let { o.sub(it, writeKey(null, it, hashCodeCounterGetter), taskReceiver) }
        }
    }

    override fun <R> preWriteKey(prefix: String?, isKey: Boolean, func: R.() -> T): List<PreWriteEntry<R>> {
        return keys.map { it.map(prefix, isKey, func) }
    }
}

//data class SelfTest(val selfTest: SelfTest?, val x: Int) {
//    companion object : ObjectRelationalMapper<SelfTest?> {
//        override fun hashCodeX(t: SelfTest?): Int {
//            return t?.selfTest?.hashCode() ?: 0
//        }
//
//        override val objectRelationalHeader: ObjectRelationalHeader by lazy {
//            val keys = listOf(HeadEntry("selfTest", "SelfTest", true))
//            ObjectRelationalHeaderData(keys)
//        }
//
//        override val objectRelationalWriter: ObjectRelationalWriter<SelfTest?> by lazy {
//            val keys = listOf(DefaultPreWriteEntry<SelfTest>("", true) { selfTest })
//            val objectr = ObjectRelationalWriterReceiverMap<SelfTest, SelfTest?>({ objectRelationalWriter }) { selfTest }
//            ObjectRelationalWriterData()
//        }
//    }
//}

class CodeGenHelper<T : Any>(val clazz: KClass<T>, val classHeaderMap: Map<KClass<*>, () -> ClassHeaderData>) {
    val map =
        CHDMap({ x, _ -> classHeaderMap[x]?.invoke() ?: throw RuntimeException("no classHeaderData for $x found") })

    inline fun <reified T> simple(name: String) = SimpleParameter(clazz, name, typeOf<T>(), KeyType.NO_KEY, map)
    inline fun <reified T> simpleHead(name: String): HeadEntry {
        val p = simple<T>(name)
        return HeadEntry(p, name, p.type.typeName()!!, false)
    }

    inline fun <reified T> simpleKeyHead(name: String): HeadEntry {
        val p = simpleKey<T>(name)
        return HeadEntry(p, name, p.type.typeName()!!, true)
    }

    inline fun <reified T> simpleKey(name: String) = SimpleParameter(clazz, name, typeOf<T>(), KeyType.NORM, map)

    inline fun <reified T> listHead(name: String): HeadEntry {
        val p = ParameterWithOneGeneric(clazz, name, typeOf<List<T>>(), KeyType.NO_KEY, map, typeOf<T>())
        return HeadEntry(p, name, p.type.typeName()!!, false)
    }

    inline fun <reified T> list(name: String) =
        ParameterWithOneGeneric(clazz, name, typeOf<List<T>>(), KeyType.NO_KEY, map, typeOf<T>())

    inline fun <reified T> listKey(name: String) =
        ParameterWithOneGeneric(clazz, name, typeOf<List<T>>(), KeyType.NORM, map, typeOf<T>())

}

data class TestXZ(val a: Int, val b: String) {
    fun hashCodeX() = Companion.hashCodeX(this)

    companion object : ObjectRelationalMapper<TestXZ> {
        val cgh = CodeGenHelper(TestXZ::class, mapOf())
        override fun hashCodeX(o: TestXZ) = o.a * 31 + o.b.hashCodeX() * 31
        val p1 = cgh.simpleKey<Int>("a")
        val p2 = cgh.simple<String>("b")
        override val classHeaderData by lazy {
            DefaultClassHeaderData(
                cgh.clazz,
                listOf(p1, p2),
                MoreKeysData(1)
            )
        }

        override val objectRelationalHeader: ObjectRelationalHeader by lazy {
            ObjectRelationalHeaderData(
                listOf(HeadEntry(p1, "a", "Int", true)),
                listOf(HeadEntry(p2, "b", "String", false)),
                emptyList()
            )
        }

        override val objectRelationalWriter: ObjectRelationalWriter<TestXZ> by lazy {
            ObjectRelationalWriterData(
                listOf(DefaultPreWriteEntry("a", true) { a }),
                listOf(DefaultPreWriteEntry("b", false) { b }),
                emptyList()
            )
        }

        override val objectRelationalReader: ObjectRelationalReader<TestXZ> by lazy {
            ObjectRelationalReaderData(
                TestXZ::class,
                listOf(ReadEntryTask("a") { nativeReads.readInt() }),
                listOf(ReadEntryTask("b") { nativeReads.readString() })
            ) { TestXZ(get(0) as Int, get(1) as String) }
        }
    }
}

data class TestComplex(val a: Int, val t: TestXZ) {
    fun hashCodeX() = Companion.hashCodeX(this)

    companion object : ObjectRelationalMapper<TestComplex> {
        override fun hashCodeX(t: TestComplex) = t.a * 31

        val cgh by lazy { CodeGenHelper(TestComplex::class, mapOf(TestXZ::class to { TestXZ.classHeaderData })) }
        val p1 = cgh.simpleKey<Int>("a")
        val p2 = cgh.simple<TestXZ>("t")
        override val classHeaderData by lazy {
            DefaultClassHeaderData(
                cgh.clazz,
                listOf(p1, p2),
                MoreKeysData(1)
            )
        }
        override val objectRelationalHeader: ObjectRelationalHeader by lazy {
            ObjectRelationalHeaderData(
                listOf("a".headInt(p1)),
                "t".headValue(TestXZ.objectRelationalHeader, p2),
                listOf({ TestXZ.objectRelationalHeader })
            )
        }

        override val objectRelationalWriter: ObjectRelationalWriter<TestComplex> by lazy {
            ObjectRelationalWriterData(
                listOf("a".writeKey { a }),
                "t".writeValue(TestXZ) { t },
                listOf(TestXZ.writerMap { t })
            )
        }

        override val objectRelationalReader: ObjectRelationalReader<TestComplex> by lazy {
            ObjectRelationalReaderData(
                TestComplex::class,
                listOf("a".readInt()),
                listOf("t".request(TestXZ.objectRelationalReader))
            ) { TestComplex(get(0) as Int, get(1) as TestXZ) }
        }
    }
}

data class ComplexList(val x: Int, val l: List<TestXZ>) {
    companion object : ObjectRelationalMapper<ComplexList> {

        override fun hashCodeX(t: ComplexList) = t.x * 31 + t.l.hashCodeX { TestXZ.hashCodeX(this) }

        val cgh by lazy { CodeGenHelper(ComplexList::class, mapOf(TestXZ::class to { TestXZ.classHeaderData })) }
        val p1 = cgh.simpleKey<Int>("x")
        val p2 = cgh.list<TestXZ>("l")
        override val classHeaderData by lazy {
            DefaultClassHeaderData(
                cgh.clazz,
                listOf(p1, p2),
                MoreKeysData(1)
            )
        }

        override val objectRelationalHeader: ObjectRelationalHeader by lazy {
            val keyEntries = listOf("x".headIntKey(p1))
            ObjectRelationalHeaderData(
                keyEntries,
                emptyList(),
                listOf({ TestXZ.headList(keyEntries, p2) })
            )
        }

        override val objectRelationalWriter: ObjectRelationalWriter<ComplexList> by lazy {
            ObjectRelationalWriterData(
                listOf("x".writeKey { x }),
                emptyList(),
                listOf(TestXZ.writerListMap { l })
            )
        }

        override val objectRelationalReader: ObjectRelationalReader<ComplexList> by lazy {
            val keys = listOf("x".readInt())
            ObjectRelationalReaderData(ComplexList::class, keys, listOf("l".requestList(TestXZ, keys))) {
                ComplexList(get(0), get(1))
            }
        }
    }
}

data class ListHolderEx(val l: List<TestXZ>) {
    companion object {
        fun hashCodeX(t: ListHolderEx) = t.l.hashCodeX { TestXZ.hashCodeX(this) }

        val objectRelationalReader: ObjectRelationalReader<ListHolderEx> by lazy {
            ObjectRelationalReaderData(
                ListHolderEx::class,
                emptyList(),
                listOf(ReadEntryTask("l") {
                    dataRequester.requestData(
                        it,
                        ListObjectReader(
                            this@Companion.objectRelationalReader,
                            emptyList(),
                            TestXZ.objectRelationalReader
                        )
                    )
                })
            ) {
                ListHolderEx(get(0))
            }
        }
    }
}

data class ReadEntryTask(val name: String, val func: ReadCollection.(List<ReadEntry>) -> Any?) {
    fun toReadData(readCollection: ReadCollection, keys: List<ReadEntry>) = ReadEntry(readCollection.func(keys))
    fun readKey(readCollection: ReadCollection) = ReadEntry(readCollection.func(emptyList()))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ReadEntryTask

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "ReadEntryTask('$name')"
    }


    companion object {
        fun readInt(name: String) = ReadEntryTask(name) { nativeReads.readInt() }
    }
}

//inline fun String.readInt() = ReadEntryTask(this) { nativeReads.readInt() }
//inline fun String.readString() = ReadEntryTask(this) { nativeReads.readString() }
//inline fun String.readDouble() = ReadEntryTask(this) { nativeReads.readDouble() }
//inline fun String.readFloat() = ReadEntryTask(this) { nativeReads.readFloat() }
//inline fun String.readChar() = ReadEntryTask(this) { nativeReads.readChar() }
//inline fun String.readShort() = ReadEntryTask(this) { nativeReads.readShort() }
//inline fun String.readLong() = ReadEntryTask(this) { nativeReads.readLong() }

inline class ReadMethod(val list: List<ReadEntry>) {
    operator fun <T> get(i: Int): T {
        return list[i].any as T
    }
}

data class ObjectRelationalReaderData<T : Any>(
    val clazz: KClass<T>,
    val keys: List<ReadEntryTask>,
    val others: List<ReadEntryTask>,
    val builder: ReadMethod.() -> T
) : ObjectRelationalReader<T> {

    /**
     * reads keys first, and then other values. Builds the object at the end
     */
    override fun read(readCollection: ReadCollection): T {
        val keysRead = readKey(readCollection)
        val othersRead = others.map { it.toReadData(readCollection, keysRead) }
        println("keysRead:")
        keysRead.forEach {
            println("it: $it")
        }
        println("others:")
        othersRead.forEach {
            println("it: $it")
        }

        return ReadMethod(keysRead + othersRead).builder()
//        return ComplexList(read1, list)
    }

    /**
     * reads only the keys. To read all values, use #read
     */
    override fun readKey(readCollection: ReadCollection): List<ReadEntry> {
        return keys.map { it.readKey(readCollection) }
//        return listOf(ReadEntry(readData.readInt()))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ObjectRelationalReaderData<*>

        if (clazz != other.clazz) return false
        if (keys != other.keys) return false
        if (others != other.others) return false

        return true
    }

    override fun hashCode(): Int {
        var result = clazz.hashCode()
        result = 31 * result + keys.hashCode()
        result = 31 * result + others.hashCode()
        return result
    }

    override fun toString(): String {
        return "ObjectRelationalReaderData(${clazz.simpleName}, $keys, noKeys=$others)"
    }


}
