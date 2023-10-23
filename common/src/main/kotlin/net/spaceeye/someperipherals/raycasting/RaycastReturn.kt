package net.spaceeye.someperipherals.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.util.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip

interface RaycastReturnOrCtx

abstract class RaycastReturn: RaycastReturnOrCtx

data class RaycastBlockReturn(val result: Pair<BlockPos, BlockState>, val distance_to:Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastEntityReturn(val result: Entity, val distance_to:Double, val hit_position: Vector3d): RaycastReturn()
data class RaycastVSShipBlockReturn(val ship: ServerShip, val block: Pair<BlockPos, BlockState>, val distance_to:Double, val hit_position: Vector3d, val hit_position_ship: Vector3d): RaycastReturn()
data class RaycastNoResultReturn(val distance_to: Double): RaycastReturn()
data class RaycastERROR(val error_str: String): RaycastReturn()


data class RaycastCtx(
    val points_iter: RayIter,
    val ignore_entity: Entity?,
    val cache: PosCache,
    val pos: Vector3d,
    val unit_d: Vector3d,

    val intersected_entity: Pair<Entity, Double>?,
    val entity_step_counter: Int,

    val future_ship_intersections: MutableList<Pair<ServerShip, Double>>? = null,
    val shipyard_rays: MutableList<Ray>? = null,
    val ship_hit_res: MutableList<Pair<RaycastReturn, Double>>? = null,

    val world_res: Pair<Pair<BlockPos, BlockState>, Double>? = null,
) : RaycastReturnOrCtx