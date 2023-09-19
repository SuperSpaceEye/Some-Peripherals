package net.spaceeye.someperipherals.forge

import dev.architectury.platform.Platform
import dev.architectury.platform.forge.EventBuses
import net.spaceeye.someperipherals.SomePeripherals
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.someperipherals.forge.integrations.cc.SomePeripheralsPeripheralProviders

@Mod(SomePeripherals.MOD_ID)
class SomePeripheralsForge {
    init {
        EventBuses.registerModEventBus(SomePeripherals.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        SomePeripherals.init()
        if (Platform.isModLoaded("computercraft")) { SomePeripheralsPeripheralProviders.registerPeripheralProviders() }
    }
}