package net.spaceeye.someperipherals

import dan200.computercraft.api.ComputerCraftAPI
import dev.architectury.platform.Platform
import dev.architectury.registry.menu.MenuRegistry
import net.spaceeye.someperipherals.config.ConfigDelegateRegister
import net.spaceeye.someperipherals.utils.digitizer.DigitizerScreen
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun LOG(s: String) = SomePeripherals.logger.warn(s)

object SomePeripherals {
    const val MOD_ID = "some_peripherals"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    var has_vs: Boolean = false
    var has_arc = false

    @JvmStatic
    fun init() {
        if (Platform.isModLoaded("valkyrienskies")) { has_vs = true}
        if (Platform.isModLoaded("acceleratedraycasting")) { has_arc = true}

        ConfigDelegateRegister.initConfig()

        SomePeripheralsBlocks.register()
        SomePeripheralsBlockEntities.register()
        SomePeripheralsItems.register()
        SomePeripheralsMenu.register()

        if (Platform.isModLoaded("computercraft")) { ComputerCraftAPI.registerPeripheralProvider(PlatformUtils.getPeripheralProvider()) }
    }

    @JvmStatic
    fun initClient() {
        MenuRegistry.registerScreenFactory(SomePeripheralsMenu.DIGITIZER_MENU.get()) {it1, it2, it3 -> DigitizerScreen(it1, it2, it3) }
    }
}