package net.spaceeye.someperipherals.forge

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.spaceeye.someperipherals.config.AbstractConfigBuilder
import net.spaceeye.someperipherals.forge.integrations.ForgeNetworkHooks
import net.spaceeye.someperipherals.forge.integrations.cc.SomePeripheralsPeripheralProviderForge
import net.spaceeye.someperipherals.utils.mix.CommonBlockEntityInventory
import net.spaceeye.someperipherals.utils.mix.CommonNetworkHooks

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = SomePeripheralsPeripheralProviderForge()

    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = ForgeConfigBuilder()

    @JvmStatic
    fun makeCommonBlockEntityInventory(size: Int): CommonBlockEntityInventory = ForgeBlockEntityInventory(size)

    @JvmStatic
    fun getCommonNetworkHooks(): CommonNetworkHooks = ForgeNetworkHooks
}