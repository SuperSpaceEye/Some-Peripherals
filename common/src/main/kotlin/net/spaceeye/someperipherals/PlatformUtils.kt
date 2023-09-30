package net.spaceeye.someperipherals

import dan200.computercraft.api.peripheral.IPeripheralProvider
import dev.architectury.injectables.annotations.ExpectPlatform

object PlatformUtils {
    @ExpectPlatform
    fun getPeripheralProvider(): IPeripheralProvider {
        throw AssertionError()
    }
}