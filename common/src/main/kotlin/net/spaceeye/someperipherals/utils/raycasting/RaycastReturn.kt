package net.spaceeye.someperipherals.utils.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.utils.mix.Vector3d
import org.valkyrienskies.core.api.ships.Ship

interface RaycastObjOrError

abstract class RaycastReturn: RaycastObjOrError

data class RaycastBlockReturn(val origin: Vector3d, val result: Pair<BlockPos, IBlockRes>, val distance_to: Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastEntityReturn(val origin: Vector3d, val result: Entity, val distance_to:Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastVSShipBlockReturn(val origin: Vector3d, val ship: Ship, val block: Pair<BlockPos, IBlockRes>, val distance_to:Double, val hit_position: Vector3d, val hit_position_ship: Vector3d): RaycastReturn()
data class RaycastNoResultReturn(val distance_to: Double): RaycastReturn()
data class RaycastERROR(val error_str: String): RaycastReturn()