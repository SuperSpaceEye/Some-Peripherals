package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.IArguments
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.stuff.utils.Vector3d
import net.spaceeye.someperipherals.stuff.utils.posShipToWorld
import net.spaceeye.someperipherals.stuff.utils.posWorldToShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import kotlin.math.sqrt

class WorldScannerPeripheral(private val level: Level, private val pos: BlockPos): IPeripheral {
    private fun makeResult(state: BlockState): Any {
        return mutableMapOf(
            Pair("block_type", state.block.descriptionId.toString())
        )
    }

    private fun getBlockAtVS(args: IArguments): Any {
        val x = args.getDouble(0)
        val y = args.getDouble(1)
        val z = args.getDouble(2)
        val inShipyard = args.optBoolean(3).orElse(false)

        val noChunkloading = SomePeripheralsConfig.SERVER.WORLD_SCANNER_SETTINGS.no_chunkloading
        val range = SomePeripheralsConfig.SERVER.WORLD_SCANNER_SETTINGS.max_allowed_range
        if (range >= 0 && sqrt(x*x+y*y+z*z) > range) { return makeErrorReturn("Block is outside of range") }

        val ship = level.getShipManagingPos(pos)

        //to world coordinates with offset
        val pos = if (ship == null || inShipyard) {
            Vector3d(pos).sadd(x, y, z)
        } else {
            posShipToWorld(ship, Vector3d(pos).sadd(x, y, z))
        }

        val shipPositions = level.transformToNearbyShipsAndWorld(pos.x, pos.y, pos.z, 1.0)

        if (shipPositions.isEmpty()) {
            if (noChunkloading && !level.isLoaded(pos.toBlockPos())) { return makeErrorReturn("Chunk is not loaded") }
            return makeResult(level.getBlockState(pos.toBlockPos()))
        }

        for (spos in shipPositions) {
            val worldShip = level.getShipManagingPos(spos.x, spos.y, spos.z) ?: continue

            val posOnShip = posWorldToShip(worldShip, pos)

            if (noChunkloading && !level.isLoaded(posOnShip.toBlockPos())) { return makeErrorReturn("Chunk is not loaded") }
            val res = level.getBlockState(posOnShip.toBlockPos())
            if (res.isAir) { continue }

            return makeResult(res)
        }
        if (noChunkloading && !level.isLoaded(pos.toBlockPos())) { return makeErrorReturn("Chunk is not loaded") }
        return makeResult(level.getBlockState((Vector3d(pos)).toBlockPos()))
    }

    private fun getBlockAtNormal(args: IArguments): Any {
        val x = args.getDouble(0)
        val y = args.getDouble(1)
        val z = args.getDouble(2)

        val range = SomePeripheralsConfig.SERVER.WORLD_SCANNER_SETTINGS.max_allowed_range
        if (range >= 0 && sqrt(x*x+y*y+z*z) > range) { return makeErrorReturn("Block is outside of range") }

        return makeResult(level.getBlockState(BlockPos(x + pos.x, y + pos.y, z + pos.z)))
    }

    @LuaFunction
    fun getBlockAt(args: IArguments): Any {
        return when(SomePeripherals.has_vs) {
            true -> getBlockAtVS(args)
            false -> getBlockAtNormal(args)
        }
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.WORLD_SCANNER.get())
    override fun getType(): String = "world_scanner"
}