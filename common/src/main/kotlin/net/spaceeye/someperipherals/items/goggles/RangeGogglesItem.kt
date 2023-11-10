package net.spaceeye.someperipherals.items.goggles

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.utils.linkPort.*
import net.spaceeye.someperipherals.utils.mix.getNowFast_ms
import net.spaceeye.someperipherals.utils.raycasting.RaycastERROR
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.timedRaycast
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.entityMakeRaycastObj

class RangeGogglesItem: StatusGogglesItem() {
    override val base_name = "item.some_peripherals.tootlip.range_goggles"
    override val linked_name = "text.some_peripherals.linked_range_goggles"
    override fun makeConnectionPing() = Server_RangeGogglesPing(connection!!.tick)

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)
        if (!tick_successful) {return}
        val r = connection!!.getRequests(uuid.toString()).raycast_request ?: return
        when(r) {
            is LinkRaycastRequest -> raycastRequest(entity, r)
            is LinkBatchRaycastRequest -> raycastBatchRequest(entity, r)
        }
    }

    private fun raycastRequest(entity: Entity, r: LinkRaycastRequest) {
        connection!!.getRequests(uuid.toString()).raycast_request = null
        val rsp = timedRaycast(entityMakeRaycastObj(entity as LivingEntity, r.distance, r.euler_mode, r.do_cache, r.var1, r.var2, r.var3, r.check_for_blocks_in_world), entity.level, SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_raycast_time_ms)
        connection!!.makeResponse(uuid.toString(), LinkRaycastResponse(rsp.first ?: RaycastERROR("Raycast took too long")))
    }

    private fun raycastBatchRequest(entity: Entity, req: LinkBatchRaycastRequest) {
        var rsp = connection!!.getResponses(uuid.toString()).raycast_response

        if (rsp == null || rsp !is LinkBatchRaycastResponse || rsp.is_done) {
            rsp = LinkBatchRaycastResponse(mutableListOf())
            connection!!.getResponses(uuid.toString()).raycast_response = rsp
        }

        val start_index = rsp.results.size

        val timeout = SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms
        val start = getNowFast_ms()

        for (i in start_index until req.data.size) {
            val item = req.data[i]
            rsp.results.add(timedRaycast(
                entityMakeRaycastObj(entity as LivingEntity, req.distance, req.euler_mode, req.do_cache, item[0], item[1], item[2], req.check_for_blocks_in_world),
                entity.level,
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_batch_raycast_time_ms).first ?: RaycastERROR("Raycast took too long")
            )
            if (getNowFast_ms() - start >= timeout) { break }
        }

        if (rsp.results.size >= req.data.size) {
            rsp.is_done = true
            connection!!.getRequests(uuid.toString()).raycast_request = null
        }
    }
}