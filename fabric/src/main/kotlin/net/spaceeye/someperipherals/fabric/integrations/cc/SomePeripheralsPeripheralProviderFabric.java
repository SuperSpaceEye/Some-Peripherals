package net.spaceeye.someperipherals.fabric.integrations.cc;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks;
import net.spaceeye.someperipherals.integrations.cc.peripherals.BallisticAcceleratorPeripheral;
import net.spaceeye.someperipherals.integrations.cc.peripherals.GoggleLinkPortPeripheral;
import net.spaceeye.someperipherals.integrations.cc.peripherals.RaycasterPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO: Kotlin-ify?
public class SomePeripheralsPeripheralProviderFabric implements IPeripheralProvider {
    @Nullable
    @Override
    public IPeripheral getPeripheral(@NotNull Level level, @NotNull BlockPos bpos, @NotNull Direction direction) {
        BlockState state = level.getBlockState(bpos);
        BlockEntity be = level.getBlockEntity(bpos);

        if        (state.is(SomePeripheralsCommonBlocks.BALLISTIC_ACCELERATOR.get())) {
            return new BallisticAcceleratorPeripheral(level, bpos);
        } else if (state.is(SomePeripheralsCommonBlocks.RAYCASTER.get())) {
            return new RaycasterPeripheral(level, bpos);
        } else if (state.is(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())) {
            return new GoggleLinkPortPeripheral(level, bpos);
        }

        return null;
    }
}
