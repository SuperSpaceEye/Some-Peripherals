package net.spaceeye.someperipherals.utils.mix

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider

abstract class CommonNetworkHooks {
    abstract fun openScreen(player: ServerPlayer, containerSupplier: MenuProvider, pos: BlockPos)
}