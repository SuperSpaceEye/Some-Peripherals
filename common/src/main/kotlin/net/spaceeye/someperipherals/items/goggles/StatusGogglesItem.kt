package net.spaceeye.someperipherals.items.goggles

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
import net.spaceeye.someperipherals.utils.mix.entityToMapGoggles
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.SomePeripheralsItems
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.utils.linkPort.*
import net.spaceeye.someperipherals.utils.mix.Constants
import java.util.UUID

open class StatusGogglesItem:
    ArmorItem(ArmorMaterials.LEATHER, EquipmentSlot.HEAD, Properties().tab(SomePeripheralsItems.TAB).stacksTo(1)) {

    protected open val base_name = "item.some_peripherals.tootlip.status_goggles"
    protected open val linked_name = "text.some_peripherals.linked_status_goggles"

    protected var tick_successful = false
    protected lateinit var uuid: UUID
    protected lateinit var connection_key: UUID
    protected var connection: LinkConnectionsManager? = null

    override fun getDescription(): Component {
        return TranslatableComponent(base_name)
    }

    protected open fun makeConnectionPing(): LinkPing {
        return Server_StatusGogglesPing(connection!!.tick)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        tick_successful = false
        if (level.isClientSide) {return}
        if (slotId != Constants.HELMET_ARMOR_SLOT_ID) {return}
        if (!stack.hasTag()
            || !stack.tag!!.contains(Constants.LINK_UUID_NAME)
            || !stack.tag!!.contains(Constants.GLASSES_UUID_NAME)) {return}

        uuid           = stack.tag!!.getUUID(Constants.GLASSES_UUID_NAME)
        connection_key = stack.tag!!.getUUID(Constants.LINK_UUID_NAME)
        connection = GlobalLinkConnections.links[connection_key] ?: return

        tick_successful = true

        connection!!.constant_pings[uuid.toString()] = makeConnectionPing()

        tryExecuteStatusRequest(entity)
    }

    private fun tryExecuteStatusRequest(entity: Entity) {
        val link = GlobalLinkConnections.links[connection_key] ?: return

        val r = link.getRequests(uuid.toString())
        if (r.status_request == null) { return }
        r.status_request = null
        link.makeResponse(uuid.toString(), LinkStatusResponse(entityToMapGoggles(entity, SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.ALLOWED_ENTITY_DATA_SETTINGS), entity))
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val bpos = context.clickedPos
        val level = context.level

        if (!level.getBlockState(bpos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())) { return super.useOn(context) }
        val be = level.getBlockEntity(bpos)
        if (be !is GoggleLinkPortBlockEntity) {return super.useOn(context)}
        if (level.isClientSide) {
            context.player!!.displayClientMessage(TranslatableComponent(linked_name), true)
            return InteractionResult.SUCCESS
        }

        val controller: GoggleLinkPortBlockEntity = be
        val item = context.itemInHand
        if (!item.hasTag()) { item.tag = CompoundTag() }
        val nbt = item.tag

        connection_key = UUID.fromString(controller.this_manager_key.toString())

        nbt!!.putUUID(Constants.GLASSES_UUID_NAME, UUID.randomUUID())
        nbt  .putUUID(Constants.LINK_UUID_NAME, connection_key)

        item.setTag(nbt)

        context.player!!.displayClientMessage(TranslatableComponent(linked_name), true)
        return InteractionResult.SUCCESS
    }
}