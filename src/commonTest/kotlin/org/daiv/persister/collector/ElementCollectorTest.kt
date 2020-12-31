package org.daiv.persister.collector

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.PEncoder
import org.daiv.persister.table.ElementCache
import kotlin.test.Test

class ElementCollectorTest {
    @Serializable
    data class Simple(val x: Int, val y: String)

    @Serializable
    data class H1(val xH1: Int, val simple: Simple)

    @Serializable
    data class H1Key(val simpleKey: Simple, val simple: Simple)

    @Serializable
    data class H2Key(val h1Key: H1Key, val simple: Simple)

    @Serializable
    data class H2(val xH2: Int, val h1: H1)

    @Serializable
    data class H3(val xH3: Int, val h2: H2)

    @Serializable
    data class H3List(val xH3List: Int, val h2s: List<H2>)


    @Test
    fun test() {
        val s = H3List.serializer()
        val element = H3List(
            9,
            listOf(
                H2(10, H1(9, Simple(6, "Hellox"))),
                H2(11, H1(6, Simple(5, "Hello"))),
                H2(12, H1(6, Simple(5, "Hello"))),
                H2(13, H1(7, Simple(5, "Hello")))
            )
        )

        val cache = ElementCache()
        val collector = ElementCollector(cache, s.getKeys(element), false)
        val module = SerializersModule { }

        val encoder = PEncoder(module) { collector }
        s.serialize(encoder, element)
        cache.all().forEach {
            println("it: $${it.serializer.descriptor.serialName}")
            it.all().forEach {
                println("it: $it")
            }
        }
//        collector.encodeSubInstance(module, Simple.serializer().descriptor, 0, )
    }
}
