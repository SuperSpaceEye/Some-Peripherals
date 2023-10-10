package net.spaceeye.someperipherals.forge.integrations.cc

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.common.util.LazyOptional
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.integrations.cc.peripherals.BallisticAcceleratorPeripheral
import net.spaceeye.someperipherals.integrations.cc.peripherals.GoggleLinkPortPeripheral
import net.spaceeye.someperipherals.integrations.cc.peripherals.RaycasterPeripheral

class SomePeripheralsPeripheralProviderForge: IPeripheralProvider {
    override fun getPeripheral(level: Level, blockPos: BlockPos, direction: Direction): LazyOptional<IPeripheral> {
        val state = level.getBlockState(blockPos)
        val be = level.getBlockEntity(blockPos)

        if        (state.`is`(SomePeripheralsCommonBlocks.BALLISTIC_ACCELERATOR.get())) {
            return LazyOptional.of{ BallisticAcceleratorPeripheral(level, blockPos) }
        } else if (state.`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())) {
            return LazyOptional.of{RaycasterPeripheral(level, blockPos)}
        } else if (state.`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())) {
            return LazyOptional.of{GoggleLinkPortPeripheral(level, blockPos)}
        }

        return LazyOptional.empty()
    }
}