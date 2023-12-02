package net.spaceeye.someperipherals.utils.digitizer

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.SomePeripheralsMenu
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks

class DigitizerMenu(var id: Int, var inventory: Inventory): AbstractContainerMenu(SomePeripheralsMenu.DIGITIZER_MENU.get(), id) {
    lateinit var blockEntity: DigitizerBlockEntity
    lateinit var level: Level
    lateinit var data: ContainerData

    constructor(id: Int, inventory: Inventory, entity: BlockEntity, data: ContainerData): this(id, inventory) {
        blockEntity = entity as DigitizerBlockEntity
        level = inventory.player.level
        this.data = data


    }

    override fun stillValid(player: Player): Boolean {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.blockPos), player, SomePeripheralsCommonBlocks.DIGITIZER.get())
    }
}