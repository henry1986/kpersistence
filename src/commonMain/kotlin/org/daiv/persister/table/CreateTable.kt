package org.daiv.persister.table

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import org.daiv.persister.MoreKeys
import org.daiv.persister.MoreKeysData

internal fun MoreKeys?.default(i: Int = 1): MoreKeysData = this?.let { MoreKeysData(amount) } ?: MoreKeysData(i)



class CreateTableClassCache {
    private val map = mutableMapOf<SerialDescriptor, String>()
    operator fun get(serialDescriptor: SerialDescriptor): String? {
        return map[serialDescriptor]
    }

    operator fun set(serialDescriptor: SerialDescriptor, operation: String) {
        map[serialDescriptor] = operation
    }
}

fun SerialDescriptor.getNativeDescriptors(): List<SerialDescriptor> {
    val d = elementDescriptors.flatMapIndexed { i, it ->
        if (it.kind == StructureKind.CLASS) {
            it.getNativeDescriptors()
        } else {
            listOf(it)
        }
    }
    return d
}

fun SerialDescriptor.getNativeDescriptorNames(): List<String> {
    val names = elementNames.toList()
    val d = elementDescriptors.flatMapIndexed { i, it ->
        val name = names[i].prefix(null)
        if (it.kind == StructureKind.CLASS) {
            it.getNativeKeyDescriptorNames(name)
        } else {
            listOf(name)
        }
    }
    return d
}

fun SerialDescriptor.moreKeys() = (annotations.find { it is MoreKeys } as MoreKeys?).default()

fun SerialDescriptor.getNativeKeyDescriptorNames(prefix: String?): List<String> {
    val moreKeys = moreKeys()
    val names = elementNames.take(moreKeys.amount).toList()
    val d = elementDescriptors.take(moreKeys.amount).flatMapIndexed { i, it ->
        val name = names[i].prefix(prefix)
        if (it.kind == StructureKind.CLASS) {
            it.getNativeKeyDescriptorNames(name)
        } else {
            listOf(name)
        }
    }
    return d
}

fun String.prefix(prefix: String?) = prefix?.let { "${it}_$this" } ?: this

fun SerialDescriptor.sqlKind() = when (this.kind) {
    PrimitiveKind.BOOLEAN -> "INT"
    PrimitiveKind.STRING -> "TEXT"
    StructureKind.LIST -> TODO()
    StructureKind.MAP -> TODO()
    StructureKind.OBJECT -> TODO()
    PolymorphicKind.SEALED -> TODO()
    PolymorphicKind.OPEN -> TODO()
    SerialKind.ENUM -> TODO()
    SerialKind.CONTEXTUAL -> TODO()
    else -> this.kind.toString()
}

fun SerialDescriptor.primaryKey(): String {
    return getNativeKeyDescriptorNames(null).joinToString(", ", "PRIMARY KEY(", ")")
}


fun SerialDescriptor.toHead(): List<String> {
    val names = getNativeDescriptorNames()
    val types = getNativeDescriptors()
    return names.mapIndexed { i, name ->
        val type = types[i].sqlKind()
        "$name ${type} NOT NULL"
    }
}

fun SerialDescriptor.tableName() = serialName.split(".").last()

fun SerialDescriptor.buildCreateTable(): String {
    return toHead().joinToString(", ", "CREATE TABLE IF NOT EXISTS ${tableName()} (", ", ${primaryKey()});")
}

fun SerialDescriptor.dependencyTables(cache: CreateTableClassCache): List<String> {
    return elementDescriptors.flatMap {
        if (it.kind == StructureKind.CLASS && cache[it] == null) {
            it.dependencyTables(cache) + it.buildCreateTable()
        } else {
            emptyList()
        }
    }
}

fun SerialDescriptor.createTables(cache: CreateTableClassCache) = dependencyTables(cache) + buildCreateTable()

