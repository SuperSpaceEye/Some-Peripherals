package net.spaceeye.someperipherals

import dan200.computercraft.api.peripheral.IPeripheralProvider
import dev.architectury.injectables.annotations.ExpectPlatform
import net.spaceeye.someperipherals.config.AbstractConfigBuilder

object PlatformUtils {
    @ExpectPlatform
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = throw AssertionError()

    @ExpectPlatform
    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = throw AssertionError()
}