package org.daiv.persister.objectrelational

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

class ReflectionObjectRelationalMapperTest {

    private data class SimpleObject(val x: Int, val y: String)

    @Test
    fun testReflection() {
        val p = SimpleObject::class.declaredMemberProperties
        val s = SimpleObject(5, "World")
        val list = listOf(1, 5, 9)
        runBlocking {
            list.asFlow().map {
                GlobalScope.launch {
                    println("before delay $it")
                    delay(100)
                    println("after delay $it")
                    delay(100)
                    println("after delay $it")
                }
            }.toList().forEach { it.join() }
        }
    }
}