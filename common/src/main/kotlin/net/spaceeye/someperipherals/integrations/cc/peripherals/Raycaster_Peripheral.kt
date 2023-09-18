package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity
import net.spaceeye.someperipherals.util.RaycastFunctions.castRay
import java.lang.Math.pow
import java.lang.RuntimeException
import kotlin.math.sqrt

class Raycaster_Peripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private var be = level.getBlockEntity(pos) as RaycasterBlockEntity

    private fun makeResponseBlock(
        res: Pair<*, *>,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        val pos = res.first as BlockPos
        val bs = res.second as BlockState
        val rc_pos = be.blockPos

        ret["is_block"] = true
        if (rcc.return_abs_pos) {ret["abs_pos"] = mutableListOf(pos.x, pos.y, pos.z)}
        val distance: Double = sqrt(
            pow((pos.x-rc_pos.x).toDouble(), 2.0)
             + pow((pos.y-rc_pos.y).toDouble(), 2.0)
             + pow((pos.z-rc_pos.z).toDouble(), 2.0)
        )
        if (rcc.return_distance) {ret["distance"] = distance}
        if (rcc.return_block_id) {ret["block_type"] = bs.block.descriptionId.toString()}
    }

    private fun makeResponseEntity(
        res: Entity,
        ret: MutableMap<Any, Any>,
        rcc: SomePeripheralsConfig.Server.Common.RaycasterSettings
    ) {
        val entity: Entity = res
        val rc_pos = be.blockPos

        ret["is_entity"] = true
        if (rcc.return_abs_pos) {ret["abs_pos"] = mutableListOf(entity.x, entity.y, entity.z)}
        val distance: Double = sqrt(
            pow((entity.x-rc_pos.x), 2.0)
                    + pow((entity.y-rc_pos.y), 2.0)
                    + pow((entity.z-rc_pos.z), 2.0)
        )
        if (rcc.return_distance) {ret["distance"] = distance}

        if (rcc.return_entity_type_descriptionId) {ret["desctiptionId"] = entity.type.descriptionId}
        if (rcc.return_entity_endodeId) {ret["encodeId"] = entity.encodeId.orEmpty()}
        if (rcc.return_entity_customName) {ret["customName"] = entity.customName.toString()}
    }

    private fun makeRaycastResponse(res: Any): MutableMap<Any, Any> {
        val ret = mutableMapOf<Any, Any>()
        val rcc = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS

        when (res) {
            is Pair<*, *> -> makeResponseBlock(res, ret, rcc)
            is Entity     -> makeResponseEntity(res, ret, rcc)
            else -> throw RuntimeException("i fucked up. ohno.")
        }

        return ret
    }

    @LuaFunction
    fun simpleRaycast(distance: Double, var1:Double, var2: Double, var3:Double, use_fisheye: Boolean = true): MutableMap<Any, Any> {
        return makeRaycastResponse(castRay(level, be, pos, distance, var1, var2, var3, use_fisheye))
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.RAYCASTER.get())
    override fun getType(): String = "raycaster"
}