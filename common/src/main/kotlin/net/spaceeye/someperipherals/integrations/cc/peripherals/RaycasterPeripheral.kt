package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.*
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity
import net.spaceeye.someperipherals.raycasting.*
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.castRay

class RaycasterPeripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private var be = level.getBlockEntity(pos) as RaycasterBlockEntity

    private fun makeResponseBlock(
        res: RaycastBlockReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.RaycasterSettings
    ) {
        val pos = res.result.first
        val bs  = res.result.second
        val hpos= res.hit_position

        ret["is_block"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
        if (rcc.return_hit_pos)  {ret["hit_pos"] = hpos.toArray()}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}
        if (rcc.return_block_type) {ret["block_type"] = bs.block.descriptionId.toString()}
    }

    private fun makeResponseEntity(
        res: RaycastEntityReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.RaycasterSettings
    ) {
        val entity = res.result
        val hpos = res.hit_position

        ret["is_entity"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(entity.x, entity.y, entity.z)}
        if (rcc.return_hit_pos)  {ret["hit_pos"] = hpos.toArray()}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}

        if (rcc.return_entity_type) {ret["descriptionId"] = entity.type.descriptionId}
    }

    private fun makeResponseVSBlock(
        res: RaycastVSShipBlockReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.RaycasterSettings
    ) {
        val pos = res.block.first
        val bs  = res.block.second
        val hpos= res.hit_position

        ret["is_block"] = true
        if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
        if (rcc.return_hit_pos)  {ret["hit_pos"] = hpos.toArray()}
        if (rcc.return_distance) {ret["distance"] = res.distance_to}
        if (rcc.return_block_type) {ret["block_type"] = bs.block.descriptionId.toString()}

        if (rcc.return_ship_id)  {ret["ship_id"] = res.ship.id.toDouble()}
        if (rcc.return_shipyard_hit_pos) {ret["hit_pos_ship"] = res.hit_position_ship.toArray()}
    }

    private fun makeResponseNoResult(
        res: RaycastNoResultReturn,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.RaycasterSettings
    ) {
        ret["is_block"] = true
        ret["distance"] = res.distance_to
        ret["block_type"] = "block.minecraft.air"
    }

    private fun makeRaycastResponse(res: RaycastReturn): MutableMap<Any, Any> {
        val ret = mutableMapOf<Any, Any>()
        val rcc = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS

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
        if(!SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.is_enabled) {return mutableMapOf()}
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

    @LuaFunction
    fun getConfigInfo(): Any {
        val rc = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS
        return mutableMapOf(
            Pair("is_enabled", rc.is_enabled),
            Pair("vector_rotation_enabled", rc.vector_rotation_enabled),
            Pair("max_raycast_distance", rc.max_raycast_distance),
            Pair("max_yaw_angle", rc.max_yaw_angle),
            Pair("max_pitch_angle", rc.max_pitch_angle),
            Pair("entity_check_radius", rc.entity_check_radius),
            Pair("check_for_intersection_with_entities", rc.check_for_intersection_with_entities),
            Pair("return_abs_pos", rc.return_abs_pos),
            Pair("return_hit_pos", rc.return_hit_pos),
            Pair("return_distance", rc.return_distance),
            Pair("return_block_type", rc.return_block_type),
            Pair("return_ship_id", rc.return_ship_id),
            Pair("return_shipyard_hit_pos", rc.return_shipyard_hit_pos),
            Pair("return_entity_type", rc.return_entity_type),
            Pair("do_position_caching", rc.do_position_caching),
            Pair("max_cached_positions", rc.max_cached_positions),
            Pair("save_cache_for_N_ticks", rc.save_cache_for_N_ticks),
        )
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"

//    @LuaFunction
//    @Throws(LuaException::class)
//    fun test(args: IArguments): Any {
//        return mutableListOf(
//            testLuaFn(3.14),
//            testLuaFn(5.46),
//            testLuaFn(42.0)
//        )
//    }
//
//    class testLuaFn(var test: Double) : ILuaFunction {
//        @Throws(LuaException::class)
//        override fun call(p0: IArguments): MethodResult {
//            SomePeripherals.logger.warn("test function return was called")
//            return MethodResult.of(test)
//        }
//    }
}