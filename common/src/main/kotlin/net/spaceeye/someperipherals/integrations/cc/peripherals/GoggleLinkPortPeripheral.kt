package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.LinkUpdate
import net.spaceeye.someperipherals.LinkPortUtils.Server_EntityPhysUpdate
import net.spaceeye.someperipherals.LinkPortUtils.Server_RangeGogglesPhysUpdate
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.blocks.GoggleLinkPort

class GoggleLinkPortPeripheral(private val level: Level, private val pos: BlockPos):IPeripheral {
    private var be = level.getBlockEntity(pos) as GoggleLinkPortBlockEntity

    private fun goggleType(connection: LinkUpdate): String {
        return when(connection) {
            is Server_RangeGogglesPhysUpdate -> "range_goggles"
            is Server_EntityPhysUpdate -> "status_goggles"
            else -> throw AssertionError("Unknown goggle type")
        }
    }

    @LuaFunction
    fun getConnected(): Any {
        val ret = mutableMapOf<String, Any>()
        val port = (be.blockState.block as GoggleLinkPort)

        for ((k, v) in port.link_connections.constant_updates) {
            val item = (v as Server_EntityPhysUpdate).data
            item["timestamp"] = v.timestamp
            item["goggle_type"] = goggleType(v)

            ret[k] = item
        }

        return ret
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())
    override fun getType(): String = "goggle_link_port"
}