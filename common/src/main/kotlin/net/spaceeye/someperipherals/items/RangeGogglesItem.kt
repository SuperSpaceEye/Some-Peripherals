package net.spaceeye.someperipherals.items

import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.context.UseOnContext
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsItems
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity

class RangeGogglesItem:
    ArmorItem(ArmorMaterials.LEATHER, EquipmentSlot.HEAD, Properties().tab(SomePeripheralsItems.TAB).stacksTo(1)) {

    private val CONTROLLER_POS = "controller_pos"
    private val CONTROLLER_LEVEL = "controller_level"

    override fun getDescription(): Component {
        return Component.translatable("item.some_peripherals.tootlip.range_goggles")
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val bpos = context.clickedPos
        val level = context.level
        if (!level.getBlockState(bpos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())) { return super.useOn(context) }
        val entity = level.getBlockEntity(bpos)
        if (entity !is GoggleLinkPortBlockEntity) {return super.useOn(context)}
        if (level.isClientSide) {
            context.player!!.displayClientMessage(Component.translatable("text.some_peripherals.linked_rage_goggles"), true)
            return InteractionResult.SUCCESS
        }
        val controller: GoggleLinkPortBlockEntity = entity
        val item = context.itemInHand
        if (!item.hasTag()) { item.tag = CompoundTag() }
        val nbt = item.tag
        val pos = controller.blockPos
        nbt!!.putIntArray(CONTROLLER_POS, intArrayOf(pos.x, pos.y, pos.z))
        nbt.putString(CONTROLLER_LEVEL, controller.getLevel()?.dimension().toString());
        item.setTag(nbt);

        context.player!!.displayClientMessage(Component.translatable("text.some_peripherals.linked_rage_goggles"), true)
        return InteractionResult.SUCCESS
    }
}