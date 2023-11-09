package net.spaceeye.someperipherals.utils.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.utils.mix.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip

interface RaycastObjOrError

abstract class RaycastReturn: RaycastObjOrError

data class RaycastBlockReturn(val result: Pair<BlockPos, BlockState>, val distance_to:Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastEntityReturn(val result: Entity, val distance_to:Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastVSShipBlockReturn(val ship: ServerShip, val block: Pair<BlockPos, BlockState>, val distance_to:Double, val hit_position: Vector3d, val hit_position_ship: Vector3d): RaycastReturn()
data class RaycastNoResultReturn(val distance_to: Double): RaycastReturn()
data class RaycastERROR(val error_str: String): RaycastReturn()