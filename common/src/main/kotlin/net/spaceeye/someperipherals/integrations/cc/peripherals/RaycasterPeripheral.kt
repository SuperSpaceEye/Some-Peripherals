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
    }

    @LuaFunction
    fun raycast(args: IArguments): MutableMap<Any, Any> {
        if(!SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.is_enabled) {return mutableMapOf()}
        val distance    = args.getDouble(0)
        val euler_mode  = args.optBoolean(1).orElse(false)
        val do_cache    = args.optBoolean(2).orElse(false)
        val var1        = args.optDouble(3).orElse(0.0) // Pitch or Y
        val var2        = args.optDouble(4).orElse(0.0) // Yaw or X
        val var3        = args.optDouble(5).orElse(1.0) // planar distance or nil

        val response = runBlocking { castRayBlock(level, be, pos, distance, euler_mode, do_cache, var1, var2, var3, null) }
        if (response is RaycastCtx) {throw LuaException("Raycast returned context")}
        return makeRaycastResponse( response as RaycastReturn )
    }

    //Either returns a result immediately (got result under max time) or returns a map of functions "continue", "getCurI"
    // when continue is called, it will continue iterating until it achieves the result
    // when getCurI is called, it will return current i of points_iter
    // when terminate is called, it will terminate raycast
    @LuaFunction
    fun yieldableRaycast(args: IArguments): Any {
        if(!SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.is_enabled) { return mutableMapOf<Any, Any>() }

        val distance    = args.getDouble(0)
        val euler_mode  = args.optBoolean(1).orElse(false)
        val do_cache    = args.optBoolean(2).orElse(false)
        val var1        = args.optDouble(3).orElse(0.0) // Pitch or Y
        val var2        = args.optDouble(4).orElse(0.0) // Yaw or X
        val var3        = args.optDouble(5).orElse(1.0) // planar distance or nil

        var ctx: RaycastCtx? = null
        var terminate = false
        var pull: MethodResult? = null

        val callback = CallbackToLuaWrapper {
            if (terminate) {return@CallbackToLuaWrapper makeRaycastResponse(RaycastERROR("Was terminated"))}

            val res = if (ctx == null) { runBlocking { withTimeoutOrNull(SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms) {
                castRayBlock(level, be, pos, distance, euler_mode, do_cache, var1, var2, var3, null)
            }}} else { runBlocking{ withTimeoutOrNull(SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms) {
                RaycastFunctions.raycast(level, ctx!!.points_iter, ctx!!.ignore_entity, ctx!!.cache, ctx, ctx!!.pos, ctx!!.unit_d)
            }}}

            if (res == null) {return@CallbackToLuaWrapper makeRaycastResponse(RaycastERROR("how"))}

            if (res is RaycastReturn) { return@CallbackToLuaWrapper makeRaycastResponse(res)} else {
                ctx = res as RaycastCtx
                computer.queueEvent("long_raycast")
                return@CallbackToLuaWrapper pull!!
            }
        }

        pull = MethodResult.pullEvent("long_raycast", callback)

        return mutableMapOf(
            Pair("begin", FunToLuaWrapper{ computer.queueEvent("long_raycast"); return@FunToLuaWrapper pull}),
            Pair("getCurI", FunToLuaWrapper { return@FunToLuaWrapper ctx?.points_iter?.cur_i ?: 0 }),
            Pair("terminate", FunToLuaWrapper{ terminate = true; return@FunToLuaWrapper Unit })
        )
    }

//    @LuaFunction
//    fun testCallback(): MethodResult {
//        var callback: CallbackToLuaWrapper? = null
//        var i = 0;
//
//        var pull: MethodResult? = null
//
//        callback = CallbackToLuaWrapper {
//            if (i >= 10) {return@CallbackToLuaWrapper MethodResult.of(10)}
//            i++
//            computer.queueEvent("test_event")
//            return@CallbackToLuaWrapper pull!!
//        }
//
//        pull = MethodResult.pullEvent("test_event", callback)
//        computer.queueEvent("test_event")
//        return pull
//    }

    @LuaFunction
    fun addStickers(state: Boolean) {
        //dont question it
        level.setBlockAndUpdate(be.blockPos, be.blockState.setValue(BlockStateProperties.POWERED, state))
    }

    @LuaFunction
    fun getConfigInfo(): Any {
        return makeConfigInfo()
    }

    lateinit var computer: IComputerAccess

    override fun attach(computer: IComputerAccess) {
        super.attach(computer)
        this.computer = computer
    }
    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}