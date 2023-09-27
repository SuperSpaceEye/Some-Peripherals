package net.spaceeye.someperipherals.fabric

import dev.architectury.platform.Platform
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsCommands
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviders

class SomePeripheralsFabric: ModInitializer {
    override fun onInitialize() {
        SomePeripherals.init()
        if (Platform.isModLoaded("computercraft")) { SomePeripheralsPeripheralProviders.registerPeripheralProviders() }

        CommandRegistrationCallback.EVENT.register { dispatcher, _ -> SomePeripheralsCommands.registerServerCommands(dispatcher)}
    }
}