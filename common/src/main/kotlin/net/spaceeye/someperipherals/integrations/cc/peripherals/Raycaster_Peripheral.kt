package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity
import net.spaceeye.someperipherals.raycasting.*
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.castRay

class Raycaster_Peripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private var be = level.getBlockEntity(pos) as RaycasterBlockEntity

    private fun makeResponseBlock(
        res: RaycastBlockReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        val pos = res.result.first
        val bs  = res.result.second

        ret["is_block"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}
        if (rcc.return_block_id) {ret["block_type"] = bs.block.descriptionId.toString()}
    }

    private fun makeResponseEntity(
        res: RaycastEntityReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        val entity: Entity = res.result

        ret["is_entity"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(entity.x, entity.y, entity.z)}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}

        if (rcc.return_entity_type_descriptionId) {ret["descriptionId"] = entity.type.descriptionId}
    }

    private fun makeResponseVSBlock(
        res: RaycastVSShipBlockReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        val pos = res.block.first
        val bs  = res.block.second

        ret["is_block"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}
        if (rcc.return_block_id) {ret["block_type"] = bs.block.descriptionId.toString()}
        if (rcc.return_ship_id)  {ret["ship_id"] = res.ship.id.toDouble()}
    }

    private fun makeResponseNoResult(
        res: RaycastNoResultReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        ret["is_block"] = true
        ret["distance"] = res.distance_to
        ret["block_type"] = "block.minecraft.air"
    }

    private fun makeRaycastResponse(res: RaycastReturn): MutableMap<Any, Any> {
        val ret = mutableMapOf<Any, Any>()
        val rcc = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS

        when (res) {
            is RaycastBlockReturn       -> makeResponseBlock   (res, ret, rcc)
            is RaycastEntityReturn      -> makeResponseEntity  (res, ret, rcc)
            is RaycastVSShipBlockReturn -> makeResponseVSBlock (res, ret, rcc)
            is RaycastNoResultReturn    -> makeResponseNoResult(res, ret, rcc)
            is RaycastERROR -> {ret["error"] = res.error_str}
            else -> {ret["error"] = "Something went very, very wrong, as this should never ever happen"}
            }

        return ret
    }

    @LuaFunction
    fun raycast(args: IArguments): MutableMap<Any, Any> {
        if(!SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.is_enabled) {return mutableMapOf()}
        val distance    = args.getDouble(0)
        val euler_mode  = args.optBoolean(1).orElse(false)
        val var1        = args.optDouble(2).orElse(0.0) // Pitch or Y
        val var2        = args.optDouble(3).orElse(0.0) // Yaw or X
        val var3        = args.optDouble(4).orElse(1.0) // planar distance or nil

        return makeRaycastResponse(castRay(level, be, pos, distance, euler_mode, var1, var2, var3))
    }

    @LuaFunction
    fun addStickers(state: Boolean) {
        //dont question it
        level.setBlockAndUpdate(be.blockPos, be.blockState.setValue(BlockStateProperties.POWERED, state))
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}