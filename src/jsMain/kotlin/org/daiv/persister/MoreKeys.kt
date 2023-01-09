package org.daiv.persister

import kotlin.reflect.KClass

internal actual fun <T : Any> KClass<T>.createObject(vararg args: Any):T{
    throw RuntimeException("this function cannot be called in js")
}
