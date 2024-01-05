package net.spaceeye.someperipherals.utils.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.utils.mix.ChunkIsNotLoadedException

class PosManager {
    var chunk: LevelChunk? = null
    var no_chunkloading: Boolean = SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.no_chunkloading_rays

    private fun getNewOrPreviousChunk(level: Level, chunk: LevelChunk, pos: BlockPos): LevelChunk {
        val cpos = chunk.pos
        if (pos.x > cpos.maxBlockX || pos.x < cpos.minBlockX || pos.z > cpos.maxBlockZ || pos.z < cpos.minBlockZ) { return level.getChunkAt(pos) }
        return chunk
    }

    fun cleanup() {chunk = null}

    fun getBlockState(level: Level, bpos: BlockPos): BlockState {
        //TODO wait for when Distant Horizons has a server side
        if (no_chunkloading && !level.isLoaded(bpos)) {throw ChunkIsNotLoadedException()}

        if (chunk == null) {chunk = level.getChunkAt(bpos)}
        chunk = getNewOrPreviousChunk(level, chunk!!, bpos)

        return chunk!!.getBlockState(bpos)
    }
}