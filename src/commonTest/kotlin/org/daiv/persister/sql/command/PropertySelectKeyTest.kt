package org.daiv.persister.sql.command

import org.daiv.persister.ObjectTypeHandlerTest.MyObject
import org.daiv.persister.TestComplexObjectType.ComplexObject
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertySelectKeyTest {

    @Test
    fun testForKey(){
        val l = 5.forKey(ComplexObject::m, MyObject::i)
        val l2 = MyObject(5, "Hello", 9L).forKey(ComplexObject::m)

        assertEquals(SelectKey(listOf("m", "i"), 5), l.toSelectKey())
        assertEquals(SelectKey(listOf("m"), MyObject(5, "Hello", 9L)),l2.toSelectKey())
    }
}
