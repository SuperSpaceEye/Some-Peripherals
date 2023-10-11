package net.spaceeye.someperipherals.items.goggles

import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.LinkRaycastRequest
import net.spaceeye.someperipherals.LinkPortUtils.LinkUpdate
import net.spaceeye.someperipherals.LinkPortUtils.Server_RangeGogglesPhysUpdate
import net.spaceeye.someperipherals.LinkPortUtils.entityToMap
import net.spaceeye.someperipherals.raycasting.RaycastFunctions

class RangeGogglesItem: StatusGogglesItem() {
    override val base_name: String = "item.some_peripherals.tootlip.range_goggles"
    override val linked_name: String = "text.some_peripherals.linked_range_goggles"
    override fun makeConnectionUpdate(entity: Entity): LinkUpdate {
        return Server_RangeGogglesPhysUpdate(entityToMap(entity))
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)
        if (!tick_successful) {return}
        val request = controller.link_connections.port_requests[uuid.toString()] ?: return
        if (request !is LinkRaycastRequest) {return} //raycast goggles can only raycast
        controller.link_connections.port_requests.remove(uuid.toString())

        RaycastFunctions

    }
}