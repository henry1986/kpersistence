package org.daiv.persister

import kotlin.reflect.KClass

internal actual fun <T : Any> KClass<T>.createObject(vararg args: Any): T {
    return this.constructors.first().call(*args)
}
