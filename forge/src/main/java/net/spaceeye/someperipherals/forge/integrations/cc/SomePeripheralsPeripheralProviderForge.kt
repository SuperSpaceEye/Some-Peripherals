package net.spaceeye.someperipherals.forge.integrations.cc

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.common.util.LazyOptional
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.integrations.cc.peripherals.BallisticAccelerator_Peripheral
import net.spaceeye.someperipherals.integrations.cc.peripherals.Raycaster_Peripheral

class SomePeripheralsPeripheralProviderForge: IPeripheralProvider {
    override fun getPeripheral(level: Level, blockPos: BlockPos, direction: Direction): LazyOptional<IPeripheral> {
        val state = level.getBlockState(blockPos)
        val be = level.getBlockEntity(blockPos)

        if (state.`is`(SomePeripheralsCommonBlocks.BALLISTIC_ACCELERATOR.get())) {
            return LazyOptional.of{ BallisticAccelerator_Peripheral(level, blockPos) }
        } else if (state.`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())) {
            return LazyOptional.of{Raycaster_Peripheral(level, blockPos)}
        }

        return LazyOptional.empty()
    }
}