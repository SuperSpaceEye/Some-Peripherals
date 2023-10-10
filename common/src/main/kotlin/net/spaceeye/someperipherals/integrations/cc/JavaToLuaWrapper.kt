package net.spaceeye.someperipherals.integrations.cc

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.ILuaFunction
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.MethodResult
import kotlin.jvm.Throws

class JavaToLuaWrapper(var construction_args: IArguments, var function: (IArguments, IArguments) -> Any): ILuaFunction {
    @Throws(LuaException::class)
    override fun call(call_arguments: IArguments): MethodResult {
        return MethodResult.of(function(construction_args, call_arguments))
    }
}