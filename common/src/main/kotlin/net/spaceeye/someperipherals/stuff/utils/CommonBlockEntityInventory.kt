package net.spaceeye.someperipherals.stuff.utils

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack

abstract class CommonBlockEntityInventory(size: Int) {
    abstract fun serializeNBT(): CompoundTag
    abstract fun deserializeNBT(nbt: CompoundTag)
    abstract fun getStackInSlot(slot: Int): ItemStack
    abstract fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack
    abstract fun setStackInSlot(slot: Int, stack: ItemStack)
    abstract fun insertItemInSlot(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack
    abstract fun getSlotLimit(slot: Int): Int
}