package net.spaceeye.someperipherals.fabric;

import net.spaceeye.someperipherals.SomePeripherals;
import net.fabricmc.api.ModInitializer;

public class SomePeripheralsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SomePeripherals.init();
    }
}