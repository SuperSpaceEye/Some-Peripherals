package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.*
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.integrations.cc.CallbackToLuaWrapper
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.blockMakeRaycastObj
import net.spaceeye.someperipherals.utils.mix.Constants
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.integrations.cc.tableToDoubleArray
import net.spaceeye.someperipherals.utils.configToMap.makeRaycastingConfigInfo
import net.spaceeye.someperipherals.utils.raycasting.*
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.timedRaycast
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.RaycastObj

class RaycasterPeripheral(private val level: Level, private val pos: BlockPos, private var be: BlockEntity): IPeripheral {
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
    }

    @LuaFunction
    fun raycast(computer: IComputerAccess, args: IArguments): MethodResult {
        val distance    = args.getDouble(0)
        // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
        val variables   = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
        val euler_mode  = args.optBoolean(2).orElse(false)
        val im_execute  = args.optBoolean(3).orElse(true) // execute immediately
        val do_cache    = args.optBoolean(4).orElse(false)
        var check_for_blocks_in_world = args.optBoolean(5).orElse(true)

        check_for_blocks_in_world = check_for_blocks_in_world && SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.allow_raycasting_for_entities_only

        if (variables.size < 2 || variables.size > 3) { return MethodResult.of(makeErrorReturn("Variables table should have 2 or 3 items")) }
        val var1 = variables[0]
        val var2 = variables[1]
        val var3 = if (variables.size == 3) {variables[2]} else {1.0}

        val raycast_obj = blockMakeRaycastObj(level, be, pos, distance, euler_mode, do_cache, var1, var2, var3, check_for_blocks_in_world)
        var terminate = false
        var pull: MethodResult? = null

        if (raycast_obj !is RaycastObj) { return MethodResult.of(makeRaycastResponse(raycast_obj as RaycastReturn)) }

        val callback = CallbackToLuaWrapper {
            if (terminate) {return@CallbackToLuaWrapper makeErrorReturn("Was terminated") }

            val res = timedRaycast(raycast_obj, level, SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms)

            if (res.first != null) { return@CallbackToLuaWrapper makeRaycastResponse(res.first!!) } else {
                computer.queueEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME)
                return@CallbackToLuaWrapper pull!!
            }
        }

        pull = MethodResult.pullEvent(Constants.RAYCASTER_RAYCAST_EVENT_NAME, callback)

        if (!im_execute) {
            return MethodResult.of(mutableMapOf(
                Pair("begin",     FunToLuaWrapper { return@FunToLuaWrapper callback.resume(null) }),
                Pair("getCurI",   FunToLuaWrapper { return@FunToLuaWrapper raycast_obj.points_iter.cur_i }),
                Pair("terminate", FunToLuaWrapper { terminate = true; return@FunToLuaWrapper Unit })
            ))
        } else {
            return callback.resume(null)
        }
    }

    @LuaFunction
    fun addStickers(state: Boolean) {
        //dont question it
        level.setBlockAndUpdate(be.blockPos, be.blockState.setValue(BlockStateProperties.POWERED, state))
    }

    @LuaFunction
    fun getConfigInfo() = makeRaycastingConfigInfo()

    @LuaFunction
    fun getFacingDirection() = be.blockState.getValue(BlockStateProperties.FACING).getName()

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}