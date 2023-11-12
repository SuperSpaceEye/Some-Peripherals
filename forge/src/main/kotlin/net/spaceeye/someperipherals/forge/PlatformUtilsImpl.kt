package net.spaceeye.someperipherals.forge

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.spaceeye.someperipherals.config.AbstractConfigBuilder
import net.spaceeye.someperipherals.forge.integrations.cc.SomePeripheralsPeripheralProviderForge

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = SomePeripheralsPeripheralProviderForge()

    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = ForgeConfigBuilder()
}