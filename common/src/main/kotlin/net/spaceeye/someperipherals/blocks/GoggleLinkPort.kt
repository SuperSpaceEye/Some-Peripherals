package net.spaceeye.someperipherals.blocks

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.stuff.linkPort.GlobalLinkConnections

class GoggleLinkPort(properties: Properties): BaseEntityBlock(properties) {
    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return GoggleLinkPortBlockEntity(pos, state)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)
    }

    private fun doTick(state: BlockState, level: ServerLevel, pos: BlockPos) {
        val key = (level.getBlockEntity(pos) as GoggleLinkPortBlockEntity).this_manager_key
        val link_connections = GlobalLinkConnections.links[key] ?: return
        link_connections.tick++
    }

    override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (level.isClientSide) {null} else {
            BlockEntityTicker<T> {level: Level, pos: BlockPos, state: BlockState, entity: T ->
                doTick(state, level as ServerLevel, pos)
            }
        }
    }
}