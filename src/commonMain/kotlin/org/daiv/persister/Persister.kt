package org.daiv.persister

import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable

data class MoreKeysData(val amount: Int)

@SerialInfo
annotation class MoreKeys(val amount: Int)

@Serializable
@MoreKeys(2)
data class TestP(val x: Int, val y: String)

@Serializable
data class TestX(val z: Int, val t: TestP)

@Serializable
data class TestZ(val d: Int, val l: List<TestP>)

class Persister {

}

