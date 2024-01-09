package net.spaceeye.someperipherals.forge

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemStackHandler
import net.spaceeye.someperipherals.stuff.utils.CommonBlockEntityInventory

class ForgeBlockEntityInventory(size: Int): CommonBlockEntityInventory(size) {
    val inventory = ItemStackHandler(size)

    override fun serializeNBT(): CompoundTag = inventory.serializeNBT()
    override fun deserializeNBT(nbt: CompoundTag) = inventory.deserializeNBT(nbt)
    override fun getStackInSlot(slot: Int): ItemStack = inventory.getStackInSlot(slot)
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = inventory.extractItem(slot, amount, simulate)
    override fun setStackInSlot(slot: Int, stack: ItemStack) = inventory.setStackInSlot(slot, stack)
    override fun insertItemInSlot(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = inventory.insertItem(slot, stack, simulate)
    override fun getSlotLimit(slot: Int): Int = inventory.getSlotLimit(slot)
}