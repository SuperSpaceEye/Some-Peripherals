package net.spaceeye.someperipherals

import dan200.computercraft.api.peripheral.IPeripheralProvider
import dev.architectury.injectables.annotations.ExpectPlatform

object PlatformUtils {
    @ExpectPlatform
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider {
        throw AssertionError()
    }
}