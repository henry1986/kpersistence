package org.daiv.persister.table

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import org.daiv.persister.*
import kotlin.test.Test

class ReadDataTest {

    @Serializable
    data class SimpleClass(val x:Int, val y:Int)

    @Serializable
    data class ComplexClass(val id:Int, val s:SimpleClass)

    class TestReadData(val list:List<Any>, val nextIndexGetter: NextIndexGetter = AllNextIndexGetter()): ReadData, NextIndexGetter by nextIndexGetter{
        override fun next() {

        }

        var counter = 0

        operator fun<T:Any> get(index:Int):T{
            return list[index] as T
        }

        override fun readLong(index: Int): Long {
            return get(index)
        }

        override fun readInt(index: Int): Int {
            return get(index)
        }

        override fun readShort(index: Int): Short {
            return get(index)
        }

        override fun readString(index: Int): String {
            return get(index)
        }

        override fun readBoolean(index: Int): Boolean {
            return get(index)
        }

        override fun readByte(index: Int): Byte {
            return get(index)
        }

        override fun readChar(index: Int): Char {
            return get(index)
        }

        override fun readFloat(index: Int): Float {
            return get(index)
        }

        override fun readDouble(index: Int): Double {
            return get(index)
        }
    }

    @Test
    fun test() = runTest{
        val s = SimpleClass.serializer()
        val readData = TestReadData(listOf(5, 9))
        val x = s.deserialize(PDecoder(SerializersModule {  }, readData))
        println("x: $x")
    }

    @Test
    fun testComplex() = runTest{
        val s = ComplexClass.serializer()
        val readData = TestReadData(listOf(5, 5))
        val r2 = TestReadData(listOf(5, 9))
        val r = s.deserialize(PDecoder(SerializersModule {  }, readData))
        println("r: $r")
    }

    class LazyTest(val i:Int, val lazy2: Lazy2){

        init {
            println("lazy2: $lazy2")
        }

        override fun toString(): String {
            return "hello $i"
        }
    }
    class Lazy2(val i:Int){
        val lazyTest by lazy { LazyTest(i, this) }

        override fun toString(): String {
            return "wolrd : $lazyTest"
        }
    }

    @Test
    fun testLazy(){
        println("start")
        Lazy2(6).lazyTest
        println("end")
    }
}
