package org.daiv.persister

import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.table.CreateTableClassCache
import org.daiv.persister.table.createTables


fun main() {
    val module = SerializersModule { }
    val s = TestX.serializer()
//    val ret = s.descriptor.buildCreateTable()
    val ret =s.descriptor.createTables(CreateTableClassCache())
    ret.forEach {
        println("ret: $it")
    }
//    val decoder = PDecoder(module)
//    val encoder = PEncoder(module)
////    val json = Json {  }
////    val string = json.encodeToString(TestP.serializer(), TestP(5, "Hello"))
////    println("string: $string")
//    val testP = TestX(9, TestP(5, "Hello"))
//    val x = s.serialize(encoder, testP)
////    val x = s.deserialize(decoder)
//    println("name: $x")
//    s.descriptor
//    s.descriptor.elementDescriptors.forEach {
//        println("it: ${it.serialName}")
//    }
}