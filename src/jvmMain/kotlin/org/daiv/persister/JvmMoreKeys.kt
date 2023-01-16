package org.daiv.persister


import kotlin.reflect.KClass

internal fun <T : Any> KClass<T>.createObject(vararg args: Any):T{
    return this.constructors.first().call(args)
}

internal fun <T : Any> T?.default(clazz: KClass<T>, vararg args: Any): T {
    return this ?: clazz.createObject(*args)
}

fun MoreKeys?.default(i: Int = 1) = default(MoreKeys::class, i, false)
