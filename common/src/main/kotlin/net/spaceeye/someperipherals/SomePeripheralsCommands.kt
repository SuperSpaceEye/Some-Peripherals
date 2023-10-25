package net.spaceeye.someperipherals

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.minecraft.client.Minecraft
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.spaceeye.someperipherals.util.getNow_ms

@Volatile
private lateinit var data: BlockState

object SomePeripheralsCommands {
    private fun lt(name: String) = LiteralArgumentBuilder.literal<CommandSourceStack>(name)
    private fun <T> arg(name: String, type: ArgumentType<T>) = RequiredArgumentBuilder.argument<CommandSourceStack, T>(name, type)

    private fun optionDebugLogging(it: CommandContext<CommandSourceStack>):Int {
        val state = BoolArgumentType.getBool(it, "enable")
        SomePeripherals.slogger.is_enabled = state
        return 0
    }

    private fun testLevelGetBlockStateChunked(it: CommandContext<CommandSourceStack>): Int {
        val rounds = IntegerArgumentType.getInteger(it, "rounds")

        val source_level = it.source.level as ServerLevel
        val chunk = source_level.getChunkAt(BlockPos(it.source.position))

        val start_x = chunk.pos.minBlockX
        val start_z = chunk.pos.minBlockZ
        val start_y = -64

        val stop_x = chunk.pos.maxBlockX
        val stop_z = chunk.pos.maxBlockZ
        val stop_y = 319

        var str: String

        var total_time = 0L

        for (round in 0 until rounds) {
            val start = getNow_ms()

            for (x in start_x..stop_x) {
            for (z in start_z..stop_z) {
            for (y in start_y..stop_y) {
                data = chunk.getBlockState(BlockPos(x, y, z))
            }}}

            val stop = getNow_ms()

            total_time += stop - start
        }

        str = "IN TOTAL: ${total_time.toDouble()/rounds} ms"
        Minecraft.getInstance().player!!.chat(str)

        return 0
    }

    private fun testLevelGetBlockStateNaive(it: CommandContext<CommandSourceStack>): Int {
        val rounds = IntegerArgumentType.getInteger(it, "rounds")

        val source_level = it.source.level as ServerLevel
        val chunk = source_level.getChunkAt(BlockPos(it.source.position))

        val start_x = chunk.pos.minBlockX
        val start_z = chunk.pos.minBlockZ
        val start_y = -64

        val stop_x = chunk.pos.maxBlockX
        val stop_z = chunk.pos.maxBlockZ
        val stop_y = 319

        var str: String

        var total_time = 0L

        for (round in 0 until rounds) {
            val start = getNow_ms()

            for (x in start_x..stop_x) {
            for (z in start_z..stop_z) {
            for (y in start_y..stop_y) {
                data = source_level.getBlockState(BlockPos(x, y, z))
            }}}

            val stop = getNow_ms()

            total_time += stop - start
        }

        str = "IN TOTAL: ${total_time.toDouble()/rounds} ms"
        Minecraft.getInstance().player!!.chat(str)

        return 0
    }

    private fun getNewOrPreviousChunk(level: ServerLevel, chunk: LevelChunk, pos: BlockPos): LevelChunk {
        val cpos = chunk.pos
        if (pos.x > cpos.maxBlockX || pos.x < cpos.minBlockX || pos.z > cpos.maxBlockZ || pos.z < cpos.minBlockZ) {
            return level.getChunkAt(pos)
        }
        return chunk
    }

    private fun testLevelGetBlockStateChunkedLine(it: CommandContext<CommandSourceStack>): Int {
        val rounds = IntegerArgumentType.getInteger(it, "rounds")
        val range = IntegerArgumentType.getInteger(it, "range")

        val level = it.source.level as ServerLevel
        var chunk = level.getChunkAt(BlockPos(it.source.position))

        val start_x = chunk.pos.minBlockX

        val stop_x = chunk.pos.minBlockX + range

        val z = 0
        val y = 0

        var str: String

        var total_time = 0L

        for (round in 0 until rounds) {
            val start = getNow_ms()

            for (x in start_x..stop_x) {
                chunk = getNewOrPreviousChunk(level, chunk, BlockPos(x, y, z))
                data = chunk.getBlockState(BlockPos(x, y, z))
            }

            val stop = getNow_ms()

            total_time += stop - start
        }

        str = "IN TOTAL: ${total_time.toDouble()/rounds} ms"
        Minecraft.getInstance().player!!.chat(str)

        return 0
    }

    private fun testLevelGetBlockStateNaiveLine(it: CommandContext<CommandSourceStack>): Int {
        val rounds = IntegerArgumentType.getInteger(it, "rounds")
        val range = IntegerArgumentType.getInteger(it, "range")

        val level = it.source.level as ServerLevel
        val chunk = level.getChunkAt(BlockPos(it.source.position))

        val start_x = chunk.pos.minBlockX

        val stop_x = chunk.pos.minBlockX + range

        val z = 0
        val y = 0

        var str: String

        var total_time = 0L

        for (round in 0 until rounds) {
            val start = getNow_ms()

            for (x in start_x..stop_x) {
                data = level.getBlockState(BlockPos(x, y, z))
            }

            val stop = getNow_ms()

            total_time += stop - start
        }

        str = "IN TOTAL: ${total_time.toDouble()/rounds} ms"
        Minecraft.getInstance().player!!.chat(str)

        return 0
    }

//    private fun optionSetDebugOffset(it: CommandContext<CommandSourceStack>): Int {
//        val offset: Float = FloatArgumentType.getFloat(it, "offset")
//
//        SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.debug_offset = offset.toDouble()
//        return 0
//    }

    fun registerServerCommands(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            lt("some_peripherals").then(
                lt("debug-logging")
                    .then(arg("enable", BoolArgumentType.bool()).executes{ optionDebugLogging(it) })
                )
//                .then(
//                lt("debug-offset")
//                    .then(arg("offset", FloatArgumentType.floatArg()).executes{ optionSetDebugOffset(it) })
//                )
                .then(
                    lt("test_chunked_level").then(arg("rounds", IntegerArgumentType.integer())
                        .executes { testLevelGetBlockStateChunked(it) })
                )
                .then(
                    lt("test_naive_level").then(arg("rounds", IntegerArgumentType.integer())
                        .executes{ testLevelGetBlockStateNaive(it)})
                )

                .then(
                    lt("test_chunked_level_line").then(arg("rounds", IntegerArgumentType.integer())
                        .then(arg("range", IntegerArgumentType.integer())
                        .executes { testLevelGetBlockStateChunkedLine(it) }))
                )
                .then(
                    lt("test_naive_level_line").then(arg("rounds", IntegerArgumentType.integer())
                        .then(arg("range", IntegerArgumentType.integer())
                            .executes { testLevelGetBlockStateNaiveLine(it) }))
                )
        )
    }
}