package net.spaceeye.someperipherals.fabric.integrations.cc;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static net.spaceeye.someperipherals.integrations.cc.GetPeripheralKt.getPeripheralCommon;

public class SomePeripheralsPeripheralProviderFabric implements IPeripheralProvider {
    @Nullable
    @Override
    public IPeripheral getPeripheral(@NotNull Level level, @NotNull BlockPos bpos, @NotNull Direction direction) {
        return getPeripheralCommon(level, bpos, direction);
    }
}
