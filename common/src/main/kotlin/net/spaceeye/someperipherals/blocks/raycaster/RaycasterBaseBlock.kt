package net.spaceeye.someperipherals.blocks.raycaster

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.spaceeye.someperipherals.blockentities.RaycasterBlockEntity

class RaycasterBaseBlock(properties: Properties): BaseEntityBlock(properties) {
    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.FACING, Direction.SOUTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.FACING)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val direction = if (ctx.player?.isCrouching == true) { ctx.nearestLookingDirection } else { ctx.nearestLookingDirection.opposite }
        return defaultBlockState().setValue(BlockStateProperties.FACING, direction)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return RaycasterBlockEntity(pos, state)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }
}