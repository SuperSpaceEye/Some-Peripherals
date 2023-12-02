package net.spaceeye.someperipherals.utils.digitizer

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.PlatformUtils
import net.spaceeye.someperipherals.SomePeripheralsMenu
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks

class DigitizerMenu(id: Int, inv: Inventory, entity: BlockEntity, data: ContainerData): AbstractContainerMenu(SomePeripheralsMenu.DIGITIZER_MENU.get(), id) {
    var blockEntity: DigitizerBlockEntity
    var level: Level
    var data: ContainerData

    constructor(id: Int, inv: Inventory, data: FriendlyByteBuf): this(id, inv, inv.player.level.getBlockEntity(data.readBlockPos())!!, SimpleContainerData(9))

    init {
        checkContainerSize(inv, 3)
        checkContainerDataCount(data, 8)

        blockEntity = entity as DigitizerBlockEntity
        level = inv.player.level
        this.data = data

        addPlayerInventory(inv)
        addPlayerHotbar(inv)

        PlatformUtils.setDigitizerStuff(blockEntity, ::addSlot)

        addDataSlots(data)
    }

    // 0 - 8 player inventory top row
    // 9 - 17 player inventory middle row
    // 18 - 26 player inventory bottom row
    // 27 - 35 player inventory hot bar
    // 36 digitizer slot
    private val playerInvIndex = 0
    private val playerInvLength = 27

    private val playerHotBarIndex = 27
    private val playerHotBarLength = 9

    private val digitizerIndex = 36
    private val digitizerLength = 1

    override fun quickMoveStack(playerIn: Player, index: Int): ItemStack {
        val source = slots[index]
        val sourceStack = source.item
        val sourceStackCopy = source.item.copy()
        if (!source.hasItem()) { return ItemStack.EMPTY }
        if (index == digitizerIndex) {
            if (!moveItemStackTo(sourceStack, playerHotBarIndex, playerHotBarIndex + playerHotBarLength, true)) { // try the hotbar; last to first
                if (!moveItemStackTo(sourceStack, playerInvIndex, playerInvIndex + playerInvLength, true)) { // try the inv; last to first
                    return ItemStack.EMPTY // neither hotbar nor inv was empty
                }
            }
        } else {
            if (!moveItemStackTo(sourceStack, digitizerIndex, digitizerIndex + digitizerLength, true)) {
                // try to move the item into the digitizer; order doesn't matter since there's only one slot in the digitizer
                return ItemStack.EMPTY
            }
        }

        if (sourceStack.count == 0) {
            source.set(ItemStack.EMPTY) // we moved the entire item stack; set the original one to be empty
        } else {
            source.setChanged()
        }
        source.onTake(playerIn, sourceStack)
        return sourceStackCopy
    }

    override fun stillValid(player: Player): Boolean {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.blockPos), player, SomePeripheralsCommonBlocks.DIGITIZER.get())
    }

    private fun addPlayerInventory(playerInventory: Inventory) {
        for (i in 0..2) {
            for (l in 0..8) {
                addSlot(Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 86 + i * 18 - 2))
            }
        }
    }

    private fun addPlayerHotbar(playerInventory: Inventory) {
        for (i in 0..8) {
            addSlot(Slot(playerInventory, i, 8 + i * 18, 142))
        }
    }
}