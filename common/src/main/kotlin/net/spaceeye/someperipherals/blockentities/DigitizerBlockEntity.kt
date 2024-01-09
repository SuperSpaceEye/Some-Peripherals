package net.spaceeye.someperipherals.blockentities

import dev.architectury.registry.menu.ExtendedMenuProvider
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.PlatformUtils.makeCommonBlockEntityInventory
import net.spaceeye.someperipherals.stuff.digitizer.DigitizerMenu

class DigitizerBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(CommonBlockEntities.DIGITIZER.get(), pos, state), ExtendedMenuProvider {
    val inventory = makeCommonBlockEntityInventory(1)
    private val data = SimpleContainerData(8)

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        tag.put("inventory", inventory.serializeNBT())
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        inventory.deserializeNBT(tag.getCompound("inventory"))
    }

    override fun createMenu(id: Int, inventory: Inventory, player: Player): AbstractContainerMenu
        = DigitizerMenu(id, inventory, this, data)

    override fun getDisplayName(): Component = TextComponent("Digitizer")

    override fun saveExtraData(buf: FriendlyByteBuf?) {
        buf!!.writeBlockPos(blockPos)
    }
}