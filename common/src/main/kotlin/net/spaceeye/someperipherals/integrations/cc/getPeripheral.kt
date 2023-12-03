package net.spaceeye.someperipherals.integrations.cc

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.integrations.cc.peripherals.*

private inline fun c(arg1:BlockState, arg2: Block) = arg1.`is`(arg2)

fun getPeripheralCommon(level: Level, blockPos: BlockPos, direction: Direction): IPeripheral? {
    val s = level.getBlockState(blockPos)
    val be = level.getBlockEntity(blockPos)

    return when {
        c(s, SomePeripheralsCommonBlocks.BALLISTIC_ACCELERATOR.get()) -> BallisticAcceleratorPeripheral(level, blockPos)
        c(s, SomePeripheralsCommonBlocks.RAYCASTER.get()) -> RaycasterPeripheral(level, blockPos, be!!)
        c(s, SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get()) -> GoggleLinkPortPeripheral(level, blockPos, be!!)
        c(s, SomePeripheralsCommonBlocks.RADAR.get()) -> RadarPeripheral(level, blockPos)
        c(s, SomePeripheralsCommonBlocks.DIGITIZER.get()) -> DigitizerPeripheral(level, blockPos, be!!)
        else -> null
    }
}