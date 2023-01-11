package org.daiv.persister

import kotlin.reflect.KClass


annotation class MoreKeys(val amount:Int = 1, val auto:Boolean = false)

fun MoreKeys.toMoreKeysData() = MoreKeysData(amount, auto)


data class MoreKeysData(val amount: Int = 1, val auto: Boolean = false)