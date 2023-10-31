package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks

class RadarPeripheral(private val level: Level, private val pos: BlockPos, private var be: BlockEntity): IPeripheral {


    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RADAR.get())
    override fun getType(): String = "sp_radar"
}