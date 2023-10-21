package net.spaceeye.someperipherals.integrations.cc

import dan200.computercraft.api.lua.*
import kotlin.jvm.Throws

class FunToLuaWrapper(var function: (IArguments) -> Any): ILuaFunction {
    @Throws(LuaException::class)
    override fun call(call_arguments: IArguments): MethodResult {
        val ret = function(call_arguments)
        return when (ret) {
            is MethodResult -> ret
            else -> MethodResult.of(ret)
        }
    }
}

class CallbackToLuaWrapper(var callback: (Array<out Any>?) -> Any): ILuaCallback {
    @Throws(LuaException::class)
    override fun resume(p0: Array<out Any>?): MethodResult {
        val ret = callback(p0)
        return when (ret) {
            is MethodResult -> ret
            else -> MethodResult.of(ret)
        }
    }
}