package net.spaceeye.someperipherals.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class RaycasterBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(CommonBlockEntities.RAYCASTER.get(), pos, state) {
}