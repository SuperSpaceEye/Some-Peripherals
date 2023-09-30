package net.spaceeye.someperipherals.forge

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.spaceeye.someperipherals.forge.integrations.cc.SomePeripheralsPeripheralProviderForge

object PlatformUtilsImpl {
    fun getPeripheralProvider(): IPeripheralProvider {
        return SomePeripheralsPeripheralProviderForge()
    }
}