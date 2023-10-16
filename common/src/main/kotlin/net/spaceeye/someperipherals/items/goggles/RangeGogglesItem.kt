package net.spaceeye.someperipherals.items.goggles

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.minecraft.world.entity.Entity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.*
import net.spaceeye.someperipherals.SomePeripheralsConfig
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
            is LinkBatchRaycastRequest -> raycastBatchRequest(entity, r)
        }
    }

    suspend private fun suspendRaycast(entity: Entity, distance: Double, euler_mode: Boolean = true, do_cache:Boolean = false,
                                       var1:Double, var2: Double, var3: Double): RaycastReturn {
        return withTimeout(SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_allowed_raycast_waiting_time_ms) {
            try {
                RaycastFunctions.castRayEntity(entity, distance, euler_mode, do_cache, var1, var2, var3)
            } catch (e: CancellationException) {
                RaycastERROR("raycast took too long")
            } catch (e: Exception) {
                RaycastERROR(e.toString())
            }
        }
    }

    private fun raycastRequest(entity: Entity, r: LinkRaycastRequest) = runBlocking {
        controller.link_connections.port_requests.remove(uuid.toString())
        val response = suspendRaycast(entity, r.distance, r.euler_mode, r.do_cache, r.var1, r.var2, r.var3)
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

        val start_index = rsp.results.size-1

        withTimeout(SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms) {
            for (i in start_index..req.data.size) {
                val item = req.data[i]
                try {
                    rsp.results.add(RaycastFunctions.castRayEntity(entity, req.distance, req.euler_mode, req.do_cache, item[0], item[1], item[2]))
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    rsp.results.add(RaycastERROR(e.toString()))
                }
            }
        }

        if (rsp.results.size >= req.data.size) {
            rsp.is_done = true
            controller.link_connections.port_requests.remove(uuid.toString())
        }
    }
}