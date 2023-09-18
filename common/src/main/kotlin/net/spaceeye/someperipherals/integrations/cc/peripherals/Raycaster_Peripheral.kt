package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity
import net.spaceeye.someperipherals.util.RaycastFunctions.castRay
import java.lang.Math.pow
import kotlin.math.sqrt

class Raycaster_Peripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private var be = level.getBlockEntity(pos) as RaycasterBlockEntity

    fun makeRaycastResponse(res: Pair<BlockPos, BlockState>): MutableMap<Any, Any> {
        val ret = mutableMapOf<Any, Any>()
        val rcc = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS

        val pos = res.first
        val bs = res.second
        val rc_pos = be.blockPos

        if (rcc.return_abs_pos) {ret["abs_pos"] = mutableListOf(pos.x, pos.y, pos.z)}
        val distance: Double = sqrt(
               pow((pos.x-rc_pos.x).toDouble(), 2.0)
                + pow((pos.y-rc_pos.y).toDouble(), 2.0)
                + pow((pos.z-rc_pos.z).toDouble(), 2.0)
        )
        if (rcc.return_distance) {ret["distance"] = distance}
        if (rcc.return_block_id) {ret["block_type"] = bs.block.descriptionId.toString()}

        return ret
    }

    @LuaFunction
    fun simpleRaycast(distance: Double, var1:Double, var2: Double, var3:Double, use_fisheye: Boolean = true): MutableMap<Any, Any> {
        return makeRaycastResponse(castRay(level, be, pos, distance, var1, var2, var3, use_fisheye))
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}