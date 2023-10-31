package net.spaceeye.someperipherals.integrations.cc

import dan200.computercraft.api.lua.LuaException

@Throws(LuaException::class)
fun fromTableGetDouble(table: MutableMap<*, *>, key:Any, error_msg:String): Double =
    when(val t = table[key]) { is Number -> t.toDouble() else -> throw LuaException("$error_msg $key")
}

@Throws(LuaException::class)
fun fromTableGetTable(table: MutableMap<*, *>, key:Any, error_msg:String): MutableMap<*, *> =
    when(val t = table[key]) { is MutableMap<*, *> -> t else -> throw LuaException("$error_msg $key")
}

@Throws(LuaException::class)
fun tableToDoubleArray(table: MutableMap<*, *>, error_msg: String = "Can't convert item "): Array<Double> {
    val ret = ArrayList<Double>()
    for (i in 1..table.size) {
        ret.add(fromTableGetDouble(table, i.toDouble(), error_msg))
    }
    return ret.toTypedArray()
}

@Throws(LuaException::class)
fun tableToTableArray(table: MutableMap<*, *>, error_msg: String = "Can't convert item "): Array<MutableMap<*, *>> {
    val ret = ArrayList<MutableMap<*, *>>()
    for (i in 1..table.size) {
        ret.add(fromTableGetTable(table, i.toDouble(), error_msg))
    }
    return ret.toTypedArray()
}