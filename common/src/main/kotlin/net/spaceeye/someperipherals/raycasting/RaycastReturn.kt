package net.spaceeye.someperipherals.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip

abstract class RaycastReturn

class RaycastBlockReturn(val result: Pair<BlockPos, BlockState>, val distance_to:Double): RaycastReturn()
class RaycastEntityReturn(val result: Entity, val distance_to:Double): RaycastReturn()
class RaycastVSShipBlockReturn(val ship: ServerShip, val block: Pair<BlockPos, BlockState>, val distance_to:Double): RaycastReturn()
class RaycastERROR(val error_str: String): RaycastReturn()