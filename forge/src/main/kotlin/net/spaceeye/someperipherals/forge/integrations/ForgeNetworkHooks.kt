package net.spaceeye.someperipherals.forge.integrations

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.MenuProvider
import net.minecraftforge.network.NetworkHooks
import net.spaceeye.someperipherals.utils.mix.CommonNetworkHooks

object ForgeNetworkHooks: CommonNetworkHooks() {
    override fun openScreen(player: ServerPlayer, containerSupplier: MenuProvider, pos: BlockPos) =
        NetworkHooks.openGui(player, containerSupplier, pos)
}