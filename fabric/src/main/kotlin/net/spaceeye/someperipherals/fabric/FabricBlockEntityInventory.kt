package net.spaceeye.someperipherals.fabric

import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.spaceeye.someperipherals.stuff.utils.CommonBlockEntityInventory

class ExtendedItemStackHandler(size: Int): ItemStackHandler(size) {
    protected fun validateSlotIndex(slot: Int) {
        if (slot < 0 || slot >= stacks.size) throw RuntimeException("Slot " + slot + " not in valid range - [0," + stacks.size + ")")
    }
    fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        if (amount == 0) return ItemStack.EMPTY
        validateSlotIndex(slot)
        val existing = stacks[slot]
        if (existing.isEmpty) return ItemStack.EMPTY
        val toExtract = Math.min(amount, existing.maxStackSize)
        return if (existing.count <= toExtract) {
            if (!simulate) {
                stacks[slot] = ItemStack.EMPTY
                onContentsChanged(slot)
                existing
            } else {
                existing.copy()
            }
        } else {
            if (!simulate) {
                stacks[slot] = ItemHandlerHelper.copyStackWithSize(
                    existing,
                    existing.count - toExtract
                )
                onContentsChanged(slot)
            }
            ItemHandlerHelper.copyStackWithSize(existing, toExtract)
        }
    }

    fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty) return ItemStack.EMPTY
        validateSlotIndex(slot)
        val existing = stacks[slot]
        var limit = Math.min(getSlotLimit(slot), stack.maxStackSize);
        if (!existing.isEmpty) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, existing)) return stack
            limit -= existing.count
        }
        if (limit <= 0) return stack
        val reachedLimit = stack.count > limit
        if (!simulate) {
            if (existing.isEmpty) {
                stacks[slot] =
                    if (reachedLimit) ItemHandlerHelper.copyStackWithSize(
                        stack,
                        limit
                    ) else stack
            } else {
                existing.grow(if (reachedLimit) limit else stack.count)
            }
            onContentsChanged(slot)
        }
        return if (reachedLimit) ItemHandlerHelper.copyStackWithSize(
            stack,
            stack.count - limit
        ) else ItemStack.EMPTY
    }
}

class FabricBlockEntityInventory(size: Int): CommonBlockEntityInventory(size) {
    val inventory = ExtendedItemStackHandler(size)

    override fun serializeNBT(): CompoundTag = inventory.serializeNBT()
    override fun deserializeNBT(nbt: CompoundTag) = inventory.deserializeNBT(nbt)
    override fun getStackInSlot(slot: Int): ItemStack = inventory.getStackInSlot(slot)
    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack = inventory.extractItem(slot, amount, simulate)
    override fun setStackInSlot(slot: Int, stack: ItemStack) = inventory.setStackInSlot(slot, stack)
    override fun insertItemInSlot(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack = inventory.insertItem(slot, stack, simulate)
    override fun getSlotLimit(slot: Int): Int = inventory.getSlotLimit(slot)
}