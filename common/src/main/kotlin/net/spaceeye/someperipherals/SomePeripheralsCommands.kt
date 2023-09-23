package net.spaceeye.someperipherals

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack

object SomePeripheralsCommands {
    private fun lt(name: String) =
        LiteralArgumentBuilder.literal<CommandSourceStack>(name)

    private fun <T> arg(name: String, type: ArgumentType<T>) =
        RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

    private fun optionDebugLogging(it: CommandContext<CommandSourceStack>):Int {
        val state = BoolArgumentType.getBool(it, "debug-logging")
        SomePeripherals.slogger.is_enabled = state
        return 0
    }

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            //lt("some_peripherals")
            //                .then(lt("debug-logging").then(arg("enable", BoolArgumentType.bool()).executes{ optionDebugLogging(it) })
            //            )
            lt("some_peripherals").then(arg("debug-logging", BoolArgumentType.bool()).executes{
                val state = BoolArgumentType.getBool(it, "debug-logging")
                SomePeripherals.slogger.is_enabled = state
                0
            })
        )
    }
}