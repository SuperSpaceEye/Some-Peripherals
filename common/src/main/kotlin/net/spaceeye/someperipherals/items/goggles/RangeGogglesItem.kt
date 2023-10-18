package net.spaceeye.someperipherals.items.goggles

import kotlinx.coroutines.*
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.*
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.suspendCastRayEntity

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
            is LinkBatchRaycastRequest -> raycastBatchRequest(entity, r)
        }
    }

    private fun raycastRequest(entity: Entity, r: LinkRaycastRequest) = runBlocking {
        controller.link_connections.port_requests.remove(uuid.toString())
        val response = suspendCastRayEntity(entity as LivingEntity, r.distance, r.euler_mode, r.do_cache, r.var1, r.var2, r.var3)
        controller.link_connections.link_response[uuid.toString()] = LinkRaycastResponse(response)
    }

    private fun raycastBatchRequest(entity: Entity, r: LinkBatchRaycastRequest) = runBlocking {
        var resp = controller.link_connections.link_response[uuid.toString()]
        if (resp != null && resp is LinkBatchRaycastResponse) {
            if (resp.is_done) {
                resp = LinkBatchRaycastResponse(mutableListOf())
                controller.link_connections.link_response[uuid.toString()] = resp
            }
        } else {
            resp = LinkBatchRaycastResponse(mutableListOf())
            controller.link_connections.link_response[uuid.toString()] = resp
        }

        val rsp = resp as LinkBatchRaycastResponse
        val req = controller.link_connections.port_requests[uuid.toString()] as LinkBatchRaycastRequest? ?: return@runBlocking

        if (req.do_terminate) {
            rsp.is_done = true
            controller.link_connections.port_requests.remove(uuid.toString())
            return@runBlocking
        }

        val start_index = rsp.results.size

        withTimeoutOrNull(SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms) {
            for (i in start_index until req.data.size) {
                val item = req.data[i]
                rsp.results.add(suspendCastRayEntity(entity as LivingEntity, req.distance, req.euler_mode, req.do_cache, item[0], item[1], item[2],
                    SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms/2))
            }
        }

        if (rsp.results.size >= req.data.size) {
            rsp.is_done = true
            controller.link_connections.port_requests.remove(uuid.toString())
        }
    }
}