package net.spaceeye.someperipherals.fabric

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsCommands

class SomePeripheralsFabric: ModInitializer {
    override fun onInitialize() {
        SomePeripherals.init()

        CommandRegistrationCallback.EVENT.register { dispatcher, _ -> SomePeripheralsCommands.registerServerCommands(dispatcher)}
    }
}

@Environment(EnvType.CLIENT)
class SomePeripheralsFabricClient: ClientModInitializer {
    override fun onInitializeClient() {
        SomePeripherals.initClient()
    }
}