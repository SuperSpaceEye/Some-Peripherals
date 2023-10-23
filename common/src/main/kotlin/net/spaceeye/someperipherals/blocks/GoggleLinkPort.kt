package net.spaceeye.someperipherals.blocks

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.LinkPortUtils.LinkConnectionsManager
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity

class GoggleLinkPort(properties: Properties): BaseEntityBlock(properties) {
    val link_connections = LinkConnectionsManager()

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? {
        return GoggleLinkPortBlockEntity(pos, state)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        super.onRemove(state, level, pos, newState, isMoving)
        link_connections.clear()
    }
}