package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.util.BallisticFunctions
import net.spaceeye.someperipherals.util.tableToDoubleArray
import net.spaceeye.someperipherals.util.tableToTableArray

class BallisticAccelerator_Peripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    @LuaFunction
    @Throws(LuaException::class)
    fun timeInAir(args: IArguments): MutableList<Any> {
        val y_projectile_pos:Double      = args.getDouble(0)
        val y_target_pos:Double          = args.getDouble(1)
        val projectile_y_velocity:Double = args.getDouble(2)
        val gravity:Double               = args.optDouble(3).orElse(0.05)
        val drag:Double                  = args.optDouble(4).orElse(0.99)
        val max_steps                    = args.optDouble(5).orElse(1000000.0)

        val res = BallisticFunctions.timeInAir(y_projectile_pos ,y_target_pos, projectile_y_velocity, gravity, drag, max_steps.toInt())
        return mutableListOf(res.first.toDouble(), res.second.toDouble())
    }

    @LuaFunction
    @Throws(LuaException::class)
    fun tryPitch(args: IArguments): MutableList<Any> {
        val pitch_to_try  = args.getDouble(0)
        val initial_speed = args.getDouble(1)
        val length        = args.getInt   (2)
        val distance      = args.getDouble(3)
        val cannon        = tableToDoubleArray(args.getTable (4), "Can't convert cannon item at ")
        val target        = tableToDoubleArray(args.getTable (5), "Can't convert target item at ")
        val gravity       = args.optDouble(6).orElse(0.05)
        val drag          = args.optDouble(7).orElse(0.99)
        val max_steps     = args.optInt   (8).orElse(1000000)

        val res = BallisticFunctions.tryPitch(pitch_to_try, initial_speed, length, distance, cannon, target, gravity, drag, max_steps)
        return mutableListOf(res.first, res.second)
    }

    @LuaFunction
    @Throws(LuaException::class)
    fun calculatePitch(args: IArguments): MutableList<Any> {
        val cannon = tableToDoubleArray(args.getTable (0), "Can't convert cannon item at ")
        val target = tableToDoubleArray(args.getTable (1), "Can't convert target item at ")
        val initial_speed     = args.getDouble(2)
        val length            = args.getInt(3)
        val amin              = args.optInt(4)     .orElse(-30)
        val amax              = args.optInt(5)     .orElse(60)
        val gravity           = args.optDouble(6)  .orElse(0.05)
        val drag              = args.optDouble(7)  .orElse(0.99)
        val max_delta_t_error = args.optDouble(8)  .orElse(1.0)
        val max_steps         = args.optInt(9)     .orElse(1000000)
        val num_iterations    = args.optInt(10)    .orElse(5)
        val num_elements      = args.optInt(11)    .orElse(20)
        val check_impossible  = args.optBoolean(12).orElse(true)

        val res = BallisticFunctions.calculatePitch(cannon, target, initial_speed, length, amin, amax, gravity, drag, max_delta_t_error, max_steps, num_iterations, num_elements, check_impossible)
        return mutableListOf(res.first, res.second)
    }

    @LuaFunction
    @Throws(LuaException::class)
    fun batchCalculatePitches(args: IArguments): MutableList<MutableList<Array<Double>>> {
        val cannon  = tableToDoubleArray(args.getTable (0), "Can't convert cannon item at ")
        val targets = tableToTableArray(args.getTable (1), "Can't convert targets at ")
        val initial_speed     = args.getDouble(2)
        val length            = args.getInt(3)
        val amin              = args.optInt(4)     .orElse(-30)
        val amax              = args.optInt(5)     .orElse(60)
        val gravity           = args.optDouble(6)  .orElse(0.05)
        val drag              = args.optDouble(7)  .orElse(0.99)
        val max_delta_t_error = args.optDouble(8)  .orElse(1.0)
        val max_steps         = args.optInt(9)     .orElse(1000000)
        val num_iterations    = args.optInt(10)    .orElse(5)
        val num_elements      = args.optInt(11)    .orElse(20)
        val check_impossible  = args.optBoolean(12).orElse(true)

        val ret = mutableListOf<MutableList<Array<Double>>>()

        for (target in targets) {
            val ctarget = tableToDoubleArray(target, "Can't convert target")
            val res = BallisticFunctions.calculatePitch(cannon, ctarget, initial_speed, length, amin, amax, gravity, drag, max_delta_t_error, max_steps, num_iterations, num_elements, check_impossible)
            ret.add(mutableListOf(res.first, res.second))
        }

        return ret
    }

    @LuaFunction
    @Throws(LuaException::class)
    fun calculateYaw(Dx: Double, Dz: Double, direction: Double):Double {
        if (direction.toInt() > 3 || direction.toInt() < 0) { throw LuaException("Direction should be between 0 and 3. 0-north, 1-west, 2-south, 3-east") }
        return BallisticFunctions.calculateYaw(Dx, Dz, direction.toInt())
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.BALLISTIC_ACCELERATOR.get())
    override fun getType(): String = "ballisticAccelerator"
}