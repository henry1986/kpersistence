package org.daiv.persister


fun interface PrimaryKeyGetter<HOLDER, KEY> {
    fun getPrimaryKey(listHolder: HOLDER): KEY
}

interface KeyInserter<HOLDER : Any, PRIMARYKEY> {
    val primaryKeyReader: PrimaryKeyGetter<HOLDER, PRIMARYKEY>
    val primaryHandler: ValueInserter<PRIMARYKEY>
    fun insertKey(t: HOLDER): Row = primaryHandler.insertValue(primaryKeyReader.getPrimaryKey(t))
}

interface TypeReader<HOLDER : Any, ELEMENT, ITERABLE : Iterable<ELEMENT>> {
    fun getMap(listHolder: HOLDER): ITERABLE
}

fun interface MapTypeReader<LISTHOLDER : Any, MAPELEMENT, MAPKEY> :
    TypeReader<LISTHOLDER, Map.Entry<MAPKEY, MAPELEMENT>, List<Map.Entry<MAPKEY, MAPELEMENT>>> {
    override fun getMap(listHolder: LISTHOLDER): List<Map.Entry<MAPKEY, MAPELEMENT>>
}

fun interface SetTypeReader<SETHOLDER : Any, SETELEMENT> :
    TypeReader<SETHOLDER, SETELEMENT, Set<SETELEMENT>> {
    override fun getMap(listHolder: SETHOLDER): Set<SETELEMENT>
}

fun interface ListTypeReader<LISTHOLDER : Any, LISTELEMENT> :
    TypeReader<LISTHOLDER, LISTELEMENT, List<LISTELEMENT>> {
    override fun getMap(listHolder: LISTHOLDER): List<LISTELEMENT>
}

interface InsertCollectionFromRow<HOLDER : Any, PRIMARYKEY, ELEMENT, ITERABLE : Iterable<ELEMENT>> :
    KeyInserter<HOLDER, PRIMARYKEY> {
    val typeReader: TypeReader<HOLDER, ELEMENT, ITERABLE>
    fun getRow(primarykey: Row, it: ELEMENT, index: Int): Row

    fun insertValue(listHolder: HOLDER): List<Row> {
        val key = insertKey(listHolder)
        return typeReader.getMap(listHolder).mapIndexed { i, it -> getRow(key, it, i) }
    }
}

data class Row private constructor(val list: List<String>) {
    constructor(vararg r: String) : this(r.asList())

    operator fun plus(row: Row): Row {
        return Row(list + row.list)
    }
}

interface CollectionValueGetIterator {
    fun getValues(databaseRunner: DatabaseRunner): DatabaseRunner

    fun getValue(databaseRunner: DatabaseRunner): DatabaseRunner {
        var pair = getValues(databaseRunner).next()
        while (pair.second) {
            val dbr = getValues(pair.first.nextRow())
            pair = dbr.next()
        }
        return pair.first
    }
}


interface GetValuesFromDBRunner : CollectionValueGetIterator {
    val primaryHandler: ReadFromDB
    val second: ReadFromDB
    val valueHandler: ReadFromDB

    override fun getValues(databaseRunner: DatabaseRunner): DatabaseRunner {
        val n = second.getValue(primaryHandler.getValue(databaseRunner))
        return valueHandler.getValue(n)
    }
}

interface ThreeColumnable<COLKEY, COLELEMENT> : CollectionValueGetIterator {
    val primaryHandler: Columnable
    val second: ToValueable<COLKEY>
    val secondColumnable: Columnable
    val valueHandler: ToValueable<COLELEMENT>
    val valueHandlerColumnable: Columnable

    fun transform(it: DRow, tableCollector: TableCollector): Pair<COLKEY?, COLELEMENT?> {
        val n = it.list.drop(primaryHandler.numberOfColumns)
        val keyKeys = n.take(secondColumnable.numberOfColumns)
        val valueKeys = n.takeLast(valueHandlerColumnable.numberOfColumns)
        return second.toValue(keyKeys, tableCollector) to valueHandler.toValue(valueKeys, tableCollector)
    }
}

data class MapTypeHandler<PRIMARYKEY, MAPHOLDER : Any, MAPVALUE, MAPKEY>(
    override val primaryHandler: ColTypeHandler<PRIMARYKEY, *>,
    val keyHandler: ColTypeHandler<MAPKEY, *>,
    override val valueHandler: ColTypeHandler<MAPVALUE, *>,
    override val primaryKeyReader: PrimaryKeyGetter<MAPHOLDER, PRIMARYKEY>,
    override val typeReader: MapTypeReader<MAPHOLDER, MAPVALUE, MAPKEY>
) : HeaderBuilder<ColTypeHandler<out Any?, *>>,
    ThreeColumnable<MAPKEY, MAPVALUE>,
    KeyInserter<MAPHOLDER, PRIMARYKEY>, GetValuesFromDBRunner,
    InsertCollectionFromRow<MAPHOLDER, PRIMARYKEY, Map.Entry<MAPKEY, MAPVALUE>, List<Map.Entry<MAPKEY, MAPVALUE>>> {

    override val nativeTypes = listOf(primaryHandler, keyHandler, valueHandler)
    override val valueHandlerColumnable: Columnable = valueHandler

    override val second: ColTypeHandler<MAPKEY, *> = keyHandler
    override val secondColumnable: Columnable = keyHandler

    override fun getRow(key: Row, it: Map.Entry<MAPKEY, MAPVALUE>, index: Int) =
        key + keyHandler.insertValue(it.key) + valueHandler.insertValue(it.value)


    fun getMap(rows: List<DRow>, tableCollector: TableCollector): Map<MAPKEY?, MAPVALUE?> {
        return rows.associate { transform(it, tableCollector) }
    }
}

data class ListTypeHandler<PRIMARYKEY, LISTHOLDER : Any, LISTELEMENT>(
    override val primaryHandler: ColTypeHandler<PRIMARYKEY, *>,
    override val valueHandler: ColTypeHandler<LISTELEMENT, *>,
    override val primaryKeyReader: PrimaryKeyGetter<LISTHOLDER, PRIMARYKEY>,
    override val typeReader: ListTypeReader<LISTHOLDER, LISTELEMENT>
) : HeaderBuilder<ColTypeHandler<out Any?, *>>,
    ThreeColumnable<Int, LISTELEMENT>,
    GetValuesFromDBRunner,
    InsertCollectionFromRow<LISTHOLDER, PRIMARYKEY, LISTELEMENT, List<LISTELEMENT>> {
    private val indexColType = ListNativeTypeHandler<Int>(NativeType.INT, "index", false)

    override val second: ColTypeHandler<Int, *> = indexColType
    override val secondColumnable: Columnable = indexColType
    override val valueHandlerColumnable: Columnable = valueHandler

    override val nativeTypes = listOf(primaryHandler, indexColType, valueHandler)
    override fun getRow(key: Row, it: LISTELEMENT, index: Int) =
        key + indexColType.insertValue(index) + valueHandler.insertValue(it)

    fun getList(rows: List<DRow>, tableCollector: TableCollector): List<LISTELEMENT?> {
        return rows.map { transform(it, tableCollector) }.sortedBy { it.first!! }.map { it.second }
    }
}

data class SetTypeHandler<PRIMARYKEY, SETHOLDER : Any, SETELEMENT>(
    override val primaryHandler: ColTypeHandler<PRIMARYKEY, *>,
    val valueHandler: ColTypeHandler<SETELEMENT, *>,
    override val primaryKeyReader: PrimaryKeyGetter<SETHOLDER, PRIMARYKEY>,
    override val typeReader: SetTypeReader<SETHOLDER, SETELEMENT>
) : HeaderBuilder<ColTypeHandler<out Any?, *>>, CollectionValueGetIterator,
    InsertCollectionFromRow<SETHOLDER, PRIMARYKEY, SETELEMENT, Set<SETELEMENT>> {
    override val nativeTypes = listOf(primaryHandler, valueHandler)
    override fun getRow(primarykey: Row, it: SETELEMENT, index: Int) =
        primarykey + valueHandler.insertValue(it)

    override fun getValues(databaseRunner: DatabaseRunner): DatabaseRunner {
        val n = primaryHandler.getValue(databaseRunner)
        return valueHandler.getValue(n)
    }

    fun getSet(rows: List<DRow>, tableCollector: TableCollector): Set<SETELEMENT?> {
        return rows.map {
            valueHandler.toValue(it.list.drop(primaryHandler.numberOfColumns), tableCollector)
        }.toSet()
    }
}
