package net.spaceeye.someperipherals.items.goggles

import kotlinx.coroutines.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.utils.linkPort.*
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.suspendCastRayEntity

class RangeGogglesItem: StatusGogglesItem() {
    override val base_name: String = "item.some_peripherals.tootlip.range_goggles"
    override val linked_name: String = "text.some_peripherals.linked_range_goggles"
    override fun makeConnectionPing(controller: GoggleLinkPort): LinkPing {
        return Server_RangeGogglesPing(controller.link_connections.tick)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)
        if (!tick_successful) {return}
        val r = controller.link_connections.getRequests(uuid.toString()).raycast_request ?: return
        when(r) {
            is LinkRaycastRequest -> raycastRequest(entity, r)
            is LinkBatchRaycastRequest -> raycastBatchRequest(entity, r)
        }
    }

    private fun raycastRequest(entity: Entity, r: LinkRaycastRequest) = runBlocking {
        controller.link_connections.getRequests(uuid.toString()).raycast_request = null
        val rsp = suspendCastRayEntity(entity as LivingEntity, r.distance, r.euler_mode, r.do_cache, r.var1, r.var2, r.var3)
        controller.link_connections.makeResponse(uuid.toString(), LinkRaycastResponse(rsp))
    }

    private fun raycastBatchRequest(entity: Entity, req: LinkBatchRaycastRequest) = runBlocking {
        var rsp = controller.link_connections.getResponses(uuid.toString()).raycast_response

        if (rsp == null || rsp !is LinkBatchRaycastResponse || rsp.is_done) {
            rsp = LinkBatchRaycastResponse(mutableListOf())
            controller.link_connections.getResponses(uuid.toString()).raycast_response = rsp
        }

        if (req.do_terminate) {
            rsp.is_done = true
            controller.link_connections.getRequests(uuid.toString()).raycast_request = null
            return@runBlocking
        }

        val start_index = rsp.results.size

        withTimeoutOrNull(SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms) {
            for (i in start_index until req.data.size) {
                val item = req.data[i]
                rsp.results.add(suspendCastRayEntity(entity as LivingEntity, req.distance, req.euler_mode, req.do_cache, item[0], item[1], item[2],
                    SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms))
            }
        }

        if (rsp.results.size >= req.data.size) {
            rsp.is_done = true
            controller.link_connections.getRequests(uuid.toString()).raycast_request = null
        }
    }
}