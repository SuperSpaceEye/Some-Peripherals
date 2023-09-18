package net.spaceeye.someperipherals

import net.minecraft.resources.ResourceLocation
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object SomePeripherals {
    const val MOD_ID = "some_peripherals"
    val logger: Logger = LogManager.getLogger(MOD_ID)

    @JvmStatic
    fun init() {
        SomePeripheralsBlocks.register()
        SomePeripheralsBlockEntities.register()
        SomePeripheralsItems.register()
    }

    @JvmStatic
    fun initClient() {

    }
    val String.resource: ResourceLocation get() = ResourceLocation(MOD_ID, this)
}