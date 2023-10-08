package net.spaceeye.someperipherals.fabric

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.spaceeye.someperipherals.config.ConfigBuilder
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviderFabric

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider {
        return SomePeripheralsPeripheralProviderFabric()
    }

    @JvmStatic
    fun getConfig(): ConfigBuilder {
        return FabricConfigBuilder()
    }
}