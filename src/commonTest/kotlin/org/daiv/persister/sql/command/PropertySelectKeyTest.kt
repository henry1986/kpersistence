package org.daiv.persister.sql.command

import org.daiv.persister.ObjectTypeHandlerTest
import org.daiv.persister.ObjectTypeHandlerTest.MyObject
import org.daiv.persister.TestComplexObjectType
import org.daiv.persister.TestComplexObjectType.ComplexObject
import org.daiv.persister.complexObjectTypeHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertySelectKeyTest {

    @Test
    fun testForKey(){
        val l = 5.forKey(ComplexObject::m, MyObject::i)
        val l2 = MyObject(5, "Hello", 9L).forKey(ComplexObject::m)

        assertEquals(SelectKey(listOf("m", "i"), 5),l.toSelectKey())
        assertEquals(SelectKey(listOf("m"), MyObject(5, "Hello", 9L)),l2.toSelectKey())

    }

    @Test
    fun testWithoutConjunction(){
        val c = ComplexObject(MyObject(5, "Hello", 9L), 15, "World")
        val typeHandler = complexObjectTypeHandler
        val l: SelectKey = 5.forKey(ComplexObject::m, MyObject::i).toSelectKey()
        val expect = "select * from ComplexObject where m_i = 5;"
    }
    @Test
    fun testWithConjunction(){
        val c = ComplexObject(MyObject(5, "Hello", 9L), 15, "World")
        val typeHandler = complexObjectTypeHandler
        val l2: SelectKey = MyObject(5, "Hello", 9L).forKey(ComplexObject::m).toSelectKey()
        val expect = "select * from ComplexObject where m_i = 5 AND m_s=\"Hello\";"
        
    }
}