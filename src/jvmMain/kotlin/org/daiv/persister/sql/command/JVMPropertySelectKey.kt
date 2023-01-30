package org.daiv.persister.sql.command

fun PropertySelectKey.checkKey(): Boolean {
    return keys.last().returnType.classifier == value?.let { it::class }
}