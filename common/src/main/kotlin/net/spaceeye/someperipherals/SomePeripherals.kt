package net.spaceeye.someperipherals

import dan200.computercraft.api.ComputerCraftAPI
import dev.architectury.platform.Platform
import net.spaceeye.someperipherals.config.ConfigDelegateRegister
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

fun LOG(s: String) = SomePeripherals.logger.warn(s)

object SomePeripherals {
    const val MOD_ID = "some_peripherals"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!

    var has_vs: Boolean = false

    @JvmStatic
    fun init() {
        if (Platform.isModLoaded("valkyrienskies")) { has_vs = true}

        ConfigDelegateRegister.initConfig()

        SomePeripheralsBlocks.register()
        SomePeripheralsBlockEntities.register()
        SomePeripheralsItems.register()
        SomePeripheralsMenu.register()

        if (Platform.isModLoaded("computercraft")) { ComputerCraftAPI.registerPeripheralProvider(PlatformUtils.getPeripheralProvider()) }
    }
}