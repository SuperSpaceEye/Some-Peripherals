package net.spaceeye.someperipherals.items.goggles

import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.*
import net.spaceeye.someperipherals.raycasting.RaycastERROR
import net.spaceeye.someperipherals.raycasting.RaycastFunctions
import net.spaceeye.someperipherals.raycasting.RaycastReturn

class RangeGogglesItem: StatusGogglesItem() {
    override val base_name: String = "item.some_peripherals.tootlip.range_goggles"
    override val linked_name: String = "text.some_peripherals.linked_range_goggles"
    override fun makeConnectionUpdate(entity: Entity): LinkUpdate {
        return Server_RangeGogglesPhysUpdate(entityToMap(entity), entity)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)
        if (!tick_successful) {return}
        val r = controller.link_connections.port_requests[uuid.toString()] ?: return
        when(r) {
            is LinkRaycastRequest -> raycastRequest(entity, r)
        }
    }

    private fun raycastRequest(
        entity: Entity,
        r: LinkRaycastRequest
    ) {
        controller.link_connections.port_requests.remove(uuid.toString())
        var response: RaycastReturn
        try {
            response =
                RaycastFunctions.castRayEntity(entity, r.distance, r.euler_mode, r.do_cache, r.var1, r.var2, r.var3)
        } catch (e: Exception) {
            val str = e.toString()
            response = RaycastERROR(str)
        }
        controller.link_connections.link_response[uuid.toString()] = LinkRaycastResponse(response)
    }
}