package net.spaceeye.someperipherals.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState

class GoggleLinkPortBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(CommonBlockEntities.GOOGLE_LINK_PORT.get(), pos, state)