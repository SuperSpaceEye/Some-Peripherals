package net.spaceeye.someperipherals

import dev.architectury.platform.Platform
import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class LogWrapper(val logger: Logger) {
    var is_enabled = false

    fun warn(msg: String) { if(is_enabled) { logger.warn(msg)} }
}

object SomePeripherals {
    const val MOD_ID = "some_peripherals"
    val logger: Logger = LogManager.getLogger(MOD_ID)!!
    val slogger = LogWrapper(logger)

    var has_vs: Boolean = false

    @JvmStatic
    fun init() {
        if (Platform.isModLoaded("valkyrienskies")) { has_vs = true}
        SomePeripheralsBlocks.register()
        SomePeripheralsBlockEntities.register()
        SomePeripheralsItems.register()
    }

    @JvmStatic
    fun initClient() {

    }
    val String.resource: ResourceLocation get() = ResourceLocation(MOD_ID, this)
}