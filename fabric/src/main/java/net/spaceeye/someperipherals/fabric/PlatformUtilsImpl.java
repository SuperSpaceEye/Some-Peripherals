package net.spaceeye.someperipherals.fabric;

import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviderFabric;

public class PlatformUtilsImpl {
    public static IPeripheralProvider getPeripheralProvider() {
        return new SomePeripheralsPeripheralProviderFabric();
    }
}
