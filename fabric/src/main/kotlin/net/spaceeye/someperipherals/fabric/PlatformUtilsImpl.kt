package net.spaceeye.someperipherals.fabric

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.spaceeye.someperipherals.config.AbstractConfigBuilder
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviderFabric

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = SomePeripheralsPeripheralProviderFabric()

    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = FabricConfigBuilder()
}