package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.*
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.integrations.cc.*
import net.spaceeye.someperipherals.stuff.raycasting.RaycastFunctions.blockMakeRaycastObj
import net.spaceeye.someperipherals.stuff.utils.Constants
import net.spaceeye.someperipherals.stuff.configToMap.makeRaycastingConfigInfo
import net.spaceeye.someperipherals.stuff.raycasting.*
import net.spaceeye.someperipherals.stuff.raycasting.RaycastFunctions.timedRaycast
import net.spaceeye.someperipherals.stuff.raycasting.RaycastFunctions.RaycastObj

class RaycasterPeripheral(private val level: Level, private val pos: BlockPos, private var be: BlockEntity): IPeripheral {
    @LuaFunction
    fun raycast(computer: IComputerAccess, args: IArguments): MethodResult {
        val distance    = args.getDouble(0)
        // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
        val variables   = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
        val euler_mode  = args.optBoolean(2).orElse(false)
        val im_execute  = args.optBoolean(3).orElse(true) // execute immediately
        var check_for_blocks_in_world = args.optBoolean(4).orElse(true)
        val only_distance = args.optBoolean(5).orElse(false)

        check_for_blocks_in_world = check_for_blocks_in_world && SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.allow_raycasting_for_entities_only

        if (variables.size < 2 || variables.size > 3) { return MethodResult.of(makeErrorReturn("Variables table should have 2 or 3 items")) }
        val var1 = variables[0]
        val var2 = variables[1]
        val var3 = if (variables.size == 3) {variables[2]} else {1.0}

        val raycast_obj = blockMakeRaycastObj(level, be, pos, distance, euler_mode, var1, var2, var3, check_for_blocks_in_world, only_distance)
        var terminate = false
        var pull: MethodResult? = null

        if (raycast_obj !is RaycastObj) { return MethodResult.of(makeRaycastReturn(raycast_obj as RaycastReturn)) }

        val callback = CallbackToLuaWrapper {
            if (terminate) {return@CallbackToLuaWrapper makeErrorReturn("Was terminated") }

            val res = timedRaycast(raycast_obj, level as ServerLevel, SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms)

            if (res.first != null) { return@CallbackToLuaWrapper makeRaycastReturn(res.first!!) } else {
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