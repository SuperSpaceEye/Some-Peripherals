package net.spaceeye.someperipherals.fabric

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsCommands

class SomePeripheralsFabric: ModInitializer {
    override fun onInitialize() {
        SomePeripherals.init()

        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment -> SomePeripheralsCommands.registerServerCommands(dispatcher)}
    }
}