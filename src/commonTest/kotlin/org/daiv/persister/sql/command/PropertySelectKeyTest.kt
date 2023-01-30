package org.daiv.persister.sql.command

import org.daiv.persister.ObjectTypeHandlerTest
import org.daiv.persister.TestComplexObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class PropertySelectKeyTest {

    @Test
    fun testForKey(){
        val l = 5.forKey(TestComplexObjectType.ComplexObject::m, ObjectTypeHandlerTest.MyObject::i)
        val l2 = ObjectTypeHandlerTest.MyObject(5, "Hello", 9L).forKey(TestComplexObjectType.ComplexObject::m)

        assertEquals(SelectKey(listOf("m", "i"), 5),l.toSelectKey())
        assertEquals(SelectKey(listOf("m"), ObjectTypeHandlerTest.MyObject(5, "Hello", 9L)),l2.toSelectKey())
    }
}