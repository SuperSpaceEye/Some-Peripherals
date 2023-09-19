package net.spaceeye.someperipherals.fabric

import dev.architectury.platform.Platform
import net.fabricmc.api.ModInitializer
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviders

class SomePeripheralsFabric: ModInitializer {
    override fun onInitialize() {
        SomePeripherals.init()

        if (Platform.isModLoaded("computercraft")) { SomePeripheralsPeripheralProviders.registerPeripheralProviders() }
    }
}