package org.daiv.persister

data class ObjectTypeHandler(
    override val typeName: String,
    override val name: String,
    override val isNullable: Boolean,
    val moreKeys: MoreKeysData,
    val nativeTypes: List<NativeTypeHandler<*, *>>
) :TypeNameable, Nameable, Headerable{

    val keys = nativeTypes.take(moreKeys.amount)

    override fun toHeader(): String {
        return keys.joinToString(", ") { it.toHeader() }
    }
}
