package net.spaceeye.someperipherals.forge

import dev.architectury.platform.forge.EventBuses
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventBusSubscriber
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsCommands
import net.spaceeye.someperipherals.SomePeripheralsMenu
import net.spaceeye.someperipherals.utils.digitizer.DigitizerScreen

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

    @EventBusSubscriber(modid = SomePeripherals.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
    object ClientModEvents {
        @SubscribeEvent
        @JvmStatic fun onClientSetup(event: FMLClientSetupEvent?) {
            SomePeripherals.initClient()
        }
    }
}