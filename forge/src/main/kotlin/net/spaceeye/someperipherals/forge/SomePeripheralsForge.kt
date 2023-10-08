package net.spaceeye.someperipherals.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.spaceeye.someperipherals.SomePeripherals
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.someperipherals.SomePeripheralsCommands

@Mod(SomePeripherals.MOD_ID)
class SomePeripheralsForge {
    init {
        EventBuses.registerModEventBus(SomePeripherals.MOD_ID, FMLJavaModLoadingContext.get().modEventBus)
        SomePeripherals.init()

        MinecraftForge.EVENT_BUS.addListener(::registerCommands)
    }

    private fun registerCommands(event: RegisterCommandsEvent) {
        SomePeripheralsCommands.registerServerCommands(event.dispatcher)
    }
}