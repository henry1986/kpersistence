package org.daiv.persister

import kotlin.reflect.KClass


//
//internal expect fun <T : Any> KClass<T>.createObject(vararg args: Any):T
//
//internal fun <T : Any> T?.default(clazz: KClass<T>, vararg args: Any): T {
//    return this ?: clazz.createObject(*args)
//}
//
//internal fun MoreKeys?.default(i: Int = 1) = default(MoreKeys::class, i, false)


annotation class MoreKeys(val amount: Int = 1, val auto: Boolean = false)

fun MoreKeys.toMoreKeysData() = MoreKeysData(amount, auto)


data class MoreKeysData(val amount: Int = 1, val auto: Boolean = false)