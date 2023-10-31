package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.*
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import kotlinx.coroutines.*
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity
import net.spaceeye.someperipherals.integrations.cc.CallbackToLuaWrapper
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.raycasting.*
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.castRayBlock
import net.spaceeye.someperipherals.util.Constants
import net.spaceeye.someperipherals.util.makeCCErrorReturn
import net.spaceeye.someperipherals.util.tableToDoubleArray

class RaycasterPeripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private var be = level.getBlockEntity(pos) as RaycasterBlockEntity

    companion object {
        @JvmStatic
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
        @JvmStatic
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
        @JvmStatic
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
        @JvmStatic
        private fun makeResponseNoResult(
            res: RaycastNoResultReturn,
            ret: MutableMap<Any, Any>,
            rcc: SomePeripheralsConfig.Server.RaycasterSettings
        ) {
            ret["is_block"] = true
            ret["distance"] = res.distance_to
            ret["block_type"] = "block.minecraft.air"
        }
        @JvmStatic
        fun makeRaycastResponse(res: RaycastReturn): MutableMap<Any, Any> {
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
        @JvmStatic
        fun makeConfigInfo(): MutableMap<String, Any> {
            val rc = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS
            return mutableMapOf(
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
    }

    @LuaFunction
    fun raycast(computer: IComputerAccess, args: IArguments): MethodResult {
        val distance    = args.getDouble(0)
        // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
        val variables   = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
        val euler_mode  = args.optBoolean(2).orElse(false)
        val im_execute  = args.optBoolean(3).orElse(true) // execute immediately
        val do_cache    = args.optBoolean(4).orElse(false)

        if (variables.size < 2 || variables.size > 3) { return MethodResult.of(makeCCErrorReturn("Variables table should have 2 or 3 items")) }
        val var1 = variables[0]
        val var2 = variables[1]
        val var3 = if (variables.size == 3) {variables[2]} else {1.0}

        var ctx: RaycastCtx? = null
        var terminate = false
        var pull: MethodResult? = null

        val callback = CallbackToLuaWrapper {
            if (terminate) {return@CallbackToLuaWrapper makeCCErrorReturn("Was terminated")}

            val res = if (ctx == null) { runBlocking { withTimeoutOrNull(SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms) {
                castRayBlock(level, be, pos, distance, euler_mode, do_cache, var1, var2, var3, null)
            }}} else { runBlocking{ withTimeoutOrNull(SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms) {
                RaycastFunctions.raycast(level, ctx!!.points_iter, ctx!!.ignore_entity, ctx!!.cache, ctx, ctx!!.pos, ctx!!.unit_d)
            }}}

            if (res == null) {return@CallbackToLuaWrapper makeCCErrorReturn("how")}

            if (res is RaycastReturn) { return@CallbackToLuaWrapper makeRaycastResponse(res)} else {
                ctx = res as RaycastCtx
                computer.queueEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME)
                return@CallbackToLuaWrapper pull!!
            }
        }

        pull = MethodResult.pullEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME, callback)

        if (!im_execute) {
            return MethodResult.of(mutableMapOf(
                Pair("begin",     FunToLuaWrapper { computer.queueEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME); return@FunToLuaWrapper pull }),
                Pair("getCurI",   FunToLuaWrapper { return@FunToLuaWrapper ctx?.points_iter?.cur_i ?: 0 }),
                Pair("terminate", FunToLuaWrapper { terminate = true; return@FunToLuaWrapper Unit })
            ))
        } else {
            computer.queueEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME)
            return pull
        }
    }

    @LuaFunction
    fun addStickers(state: Boolean) {
        //dont question it
        level.setBlockAndUpdate(be.blockPos, be.blockState.setValue(BlockStateProperties.POWERED, state))
    }

    @LuaFunction
    fun getConfigInfo(): Any {
        return makeConfigInfo()
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}