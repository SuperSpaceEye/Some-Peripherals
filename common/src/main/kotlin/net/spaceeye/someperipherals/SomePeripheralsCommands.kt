package net.spaceeye.someperipherals

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import net.minecraft.commands.CommandSource
import net.minecraft.commands.CommandSourceStack

object SomePeripheralsCommands {
    private fun literal(name: String) =
        LiteralArgumentBuilder.literal<CommandSourceStack>(name)

    private fun <T> argument(name: String, type: ArgumentType<T>) =
        RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)
    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal("some_peripherals").then(argument("debug-logging", BoolArgumentType.bool()).executes{
                val state = BoolArgumentType.getBool(it, "debug-logging")
                SomePeripherals.slogger.is_enabled = state
                0
            })
        )
    }
}