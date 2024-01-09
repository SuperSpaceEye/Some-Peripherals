package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.stuff.configToMap.makeRadarConfigInfo
import net.spaceeye.someperipherals.stuff.radar.scanForEntitiesInRadius
import net.spaceeye.someperipherals.stuff.radar.scanForPlayersInRadius
import net.spaceeye.someperipherals.stuff.radar.scanForShipsInRadius
import net.spaceeye.someperipherals.stuff.radar.scanInRadius
import kotlin.jvm.Throws

class RadarPeripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    fun scan(radius: Double) = scanInRadius(radius, level, pos)

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    fun scanForEntities(radius: Double) = scanForEntitiesInRadius(radius, level, pos)

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    fun scanForShips(radius: Double) = scanForShipsInRadius(radius, level, pos)

    @LuaFunction(mainThread = true)
    @Throws(LuaException::class)
    fun scanForPlayers(radius: Double) = scanForPlayersInRadius(radius, level, pos)

    @LuaFunction
    fun getConfigInfo() = makeRadarConfigInfo()

    override fun equals(p0: IPeripheral?) = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RADAR.get())
    override fun getType() = "sp_radar"
}