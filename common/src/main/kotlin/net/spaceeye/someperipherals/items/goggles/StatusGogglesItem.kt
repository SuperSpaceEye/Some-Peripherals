package net.spaceeye.someperipherals.items.goggles

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.ArmorMaterials
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.LinkUpdate
import net.spaceeye.someperipherals.LinkPortUtils.Server_EntityPhysUpdate
import net.spaceeye.someperipherals.LinkPortUtils.entityToMap
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsItems
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.util.Constants
import java.util.UUID

open class StatusGogglesItem:
    ArmorItem(ArmorMaterials.LEATHER, EquipmentSlot.HEAD, Properties().tab(SomePeripheralsItems.TAB).stacksTo(1)) {

    private val CONTROLLER_POS = "controller_pos"
    private val CONTROLLER_LEVEL = "controller_level"
    private val _UUID = "uuid"

    protected open val base_name = "item.some_peripherals.tootlip.status_goggles"
    protected open val linked_name = "text.some_peripherals.linked_status_goggles"

    protected var tick_successful = false
    protected lateinit var pos: BlockPos
    protected lateinit var uuid: UUID
    protected lateinit var controller: GoggleLinkPort

    override fun getDescription(): Component {
        return TranslatableComponent(base_name)
    }

    protected open fun makeConnectionUpdate(entity: Entity): LinkUpdate {
        return Server_EntityPhysUpdate(entityToMap(entity), entity)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        tick_successful = false
        if (level.isClientSide) {return}
        if (slotId != Constants.HELMET_ARMOR_SLOT_ID) {return}
        if (!stack.hasTag()
            || !stack.tag!!.contains(CONTROLLER_POS)
            || !stack.tag!!.contains(CONTROLLER_LEVEL)
            || !stack.tag!!.contains(_UUID)) {return}
        val dimension = stack.tag!!.getString(CONTROLLER_LEVEL)
        if (dimension != level.dimension().toString()) {return}

        val arr = stack.tag!!.getIntArray(CONTROLLER_POS)
        if (arr.size < 3) {return}
        pos = BlockPos(arr[0], arr[1], arr[2])

        val be = level.getBlockEntity(pos)?: return
        if (be.blockState.block !is GoggleLinkPort) {return}

        //All checks have successfully passed

        uuid = stack.tag!!.getUUID(_UUID)
        controller = be.blockState.block as GoggleLinkPort

        tick_successful = true

        controller.link_connections.constant_updates[uuid.toString()] = makeConnectionUpdate(entity)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val bpos = context.clickedPos
        val level = context.level
        if (!level.getBlockState(bpos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())) { return super.useOn(context) }
        val entity = level.getBlockEntity(bpos)
        if (entity !is GoggleLinkPortBlockEntity) {return super.useOn(context)}
        if (level.isClientSide) {
            context.player!!.displayClientMessage(TranslatableComponent(linked_name), true)
            return InteractionResult.SUCCESS
        }
        val controller: GoggleLinkPortBlockEntity = entity
        val item = context.itemInHand
        if (!item.hasTag()) { item.tag = CompoundTag() }
        val nbt = item.tag
        val pos = controller.blockPos
        nbt!!.putIntArray(CONTROLLER_POS, intArrayOf(pos.x, pos.y, pos.z))
        nbt.putString(CONTROLLER_LEVEL, controller.getLevel()?.dimension().toString());
        nbt.putUUID(_UUID, UUID.randomUUID())
        item.setTag(nbt)

        context.player!!.displayClientMessage(TranslatableComponent(linked_name), true)
        return InteractionResult.SUCCESS
    }
}