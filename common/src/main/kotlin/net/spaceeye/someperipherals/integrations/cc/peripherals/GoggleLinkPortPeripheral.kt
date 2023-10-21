package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.*
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.raycasting.RaycastERROR
import net.spaceeye.someperipherals.util.getNow_ms
import net.spaceeye.someperipherals.util.tableToDoubleArray
import net.spaceeye.someperipherals.util.tableToTableArray
import kotlin.math.max
import kotlin.math.min

private fun checkConnection(v: LinkUpdate?): Boolean {
    if (v == null) {return false}
    if (getNow_ms() - v.timestamp > SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ms) {return false}

    return true
}

//TODO refactor and simplify logic
class GoggleLinkPortPeripheral(private val level: Level, private val pos: BlockPos):IPeripheral {
    private var be = level.getBlockEntity(pos) as GoggleLinkPortBlockEntity

    private fun goggleType(connection: LinkUpdate): String {
        return when(connection) {
            is Server_RangeGogglesPhysUpdate -> "range_goggles"
            is Server_EntityPhysUpdate -> "status_goggles"
            else -> throw AssertionError("Unknown goggle type")
        }
    }

    @LuaFunction
    fun getConnected(): Any {
        val ret = mutableMapOf<String, Any>()
        val port = (be.blockState.block as GoggleLinkPort)

        val to_remove = mutableListOf<String>()
        for ((k, v) in port.link_connections.constant_updates) {
            if (!checkConnection(v)) { to_remove.add(k);continue}

            val item = mutableMapOf<String, Any>()
            makeBaseGoggleFunctions(item, port, k)
            when (v) {
                is Server_RangeGogglesPhysUpdate -> makeRangeGogglesFunctions(item, port, k)
            }

            ret[k] = item
        }
        to_remove.forEach { port.link_connections.constant_updates.remove(it) }

        return ret
    }

    private fun makeRangeGogglesFunctions(
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["raycast"] = FunToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) {
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated"))
            }
            val cur_req = port.link_connections.port_requests[k]
            if (   cur_req != null
                && cur_req is LinkBatchRaycastRequest
                && getNow_ms() - cur_req.start < cur_req.timeout) {
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection already has a batch raycast request"))
            }

            val timeout = min(
                max(it.getLong(0), 0L),
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_allowed_raycast_waiting_time_ms
            )
            val start = getNow_ms()
            if (port.link_connections.link_response[k] != null) { port.link_connections.link_response.remove(k) }
            port.link_connections.port_requests[k] = LinkRaycastRequest(
                it.getDouble(1),
                it.optBoolean(2).orElse(false),
                it.optBoolean(3).orElse(false),
                it.optDouble(4).orElse(0.0), // Pitch or Y
                it.optDouble(5).orElse(0.0), // Yaw or X
                it.optDouble(6).orElse(1.0) // planar distance or nil
            )
            val sleep_for =
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.thread_awaiting_sleep_time_ms

            while (getNow_ms() - start <= timeout) {
                val res = port.link_connections.link_response[k]
                if (res == null) {
                    Thread.sleep(sleep_for); continue
                }
                return@FunToLuaWrapper RaycasterPeripheral.makeRaycastResponse((res as LinkRaycastResponse).result)
            }

            return@FunToLuaWrapper RaycasterPeripheral.makeRaycastResponse(RaycastERROR("timeout"))
        }

        item["queue_raycasts"] = FunToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) {
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated"))
            }
            val cur_req = port.link_connections.port_requests[k]
            if (   cur_req != null
                && cur_req is LinkBatchRaycastRequest
                && getNow_ms() - cur_req.start < cur_req.timeout) {
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection already has a batch raycast request"))
            }

            val timeout = min(
                max(it.getLong(0), 0L),
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_allowed_raycast_waiting_time_ms
            )
            val start = getNow_ms()
            if (port.link_connections.link_response[k] != null) { port.link_connections.link_response.remove(k) }

            val data = mutableListOf<Array<Double>>()
            tableToTableArray(it.getTable(4), "Can't convert to table at ")
                .forEachIndexed { idx, it -> data.add(tableToDoubleArray(it, "Can't convert to table at index ${idx} item at ")) }

            port.link_connections.port_requests[k] = LinkBatchRaycastRequest(
                it.getDouble(1),
                it.optBoolean(2).orElse(false),
                it.optBoolean(3).orElse(false),
                data.toTypedArray(),
                start,
                timeout
            )

            return@FunToLuaWrapper mutableMapOf(Pair("queued", true))
        }

        item["get_queued_data"] = FunToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) {
                if (port.link_connections.link_response[k] != null) { port.link_connections.link_response.remove(k) }
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated"))
            }

            val r = port.link_connections.link_response[k]
            if (r == null || r !is LinkBatchRaycastResponse) { return@FunToLuaWrapper mutableMapOf(Pair("error", "No batch raycast response")) }

            val data = mutableMapOf<String, Any>()
            data["is_done"] = r.is_done
            val returns = mutableMapOf<Double, Any>()
            //this is to avoid concurrent modification exception
            for (i in 0 until r.results.size) {returns[i.toDouble()+1.0] = RaycasterPeripheral.makeRaycastResponse(r.results[i])}
            data["results"] = returns

            return@FunToLuaWrapper data
        }

        item["getConfigInfo"] = FunToLuaWrapper {
            val data = RaycasterPeripheral.makeConfigInfo()
            val rgc = SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS

            data["max_allowed_waiting_time"] = rgc.max_allowed_raycast_waiting_time_ms
            data["thread_awaiting_sleep_time"] = rgc.thread_awaiting_sleep_time_ms
            data["max_connection_timeout_time"] = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ms

            data
        }
    }

    private fun makeBaseGoggleFunctions(
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["get_info"] = FunToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated")) }
            val item = (v as Server_EntityPhysUpdate).data
            item["timestamp"] = v.timestamp
            item["goggle_type"] = goggleType(v)
            item
        }
        item["type"] = port.link_connections.constant_updates[k]?.let { goggleType(it) } ?: "terminated"
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())
    override fun getType(): String = "goggle_link_port"
}