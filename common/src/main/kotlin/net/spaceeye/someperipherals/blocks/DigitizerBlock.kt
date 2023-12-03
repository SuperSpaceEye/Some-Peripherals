package net.spaceeye.someperipherals.blocks

import dev.architectury.registry.menu.ExtendedMenuProvider
import dev.architectury.registry.menu.MenuRegistry
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity

class DigitizerBlock(properties: Properties): BaseEntityBlock(properties) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return DigitizerBlockEntity(pos, state)
    }

    init {
        registerDefaultState(defaultBlockState()
            .setValue(BlockStateProperties.FACING, Direction.SOUTH)
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.FACING)
            .add(BlockStateProperties.POWERED)
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        val direction = if (ctx.player?.isCrouching == true) { ctx.nearestLookingDirection } else { ctx.nearestLookingDirection.opposite }
        return defaultBlockState().setValue(BlockStateProperties.FACING, direction)
    }

    override fun use(state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult): InteractionResult {
        if (player !is ServerPlayer) {return InteractionResult.SUCCESS}

        val provider = this.getMenuProvider(state, level, pos)
        if (provider != null) {
            MenuRegistry.openExtendedMenu(player, provider as ExtendedMenuProvider)
        }
        return InteractionResult.CONSUME
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)
    }
}