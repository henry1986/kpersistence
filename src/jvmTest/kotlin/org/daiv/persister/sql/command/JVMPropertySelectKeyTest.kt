package org.daiv.persister.sql.command

import org.daiv.persister.ObjectTypeHandlerTest
import org.daiv.persister.TestComplexObjectType
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JVMPropertySelectKeyTest {

    @Test
    fun testCheckKey() {
        val l = 5.forKey(TestComplexObjectType.ComplexObject::m, ObjectTypeHandlerTest.MyObject::i)
        val l2 = ObjectTypeHandlerTest.MyObject(5, "Hello", 9L).forKey(TestComplexObjectType.ComplexObject::m)
        assertTrue(l.checkKey())
        assertTrue(l2.checkKey())
    }
}