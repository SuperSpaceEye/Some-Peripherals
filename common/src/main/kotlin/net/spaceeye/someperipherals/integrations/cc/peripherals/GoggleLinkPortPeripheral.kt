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
import net.spaceeye.someperipherals.integrations.cc.JavaToLuaWrapper
import net.spaceeye.someperipherals.raycasting.RaycastERROR
import java.util.*
import kotlin.math.max
import kotlin.math.min

private fun checkConnection(v: LinkUpdate?): Boolean {
    if (v == null) {return false}
    if (Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time - v.timestamp > SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time * 1000) {return false}

    return true
}

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
        item["raycast"] = JavaToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) {
                return@JavaToLuaWrapper mutableListOf(Pair("error", "Connection has been terminated"))
            }

            val timeout = min(
                max(it.getDouble(0), 0.0),
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.max_allowed_waiting_time
            )
            val start = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
            if (port.link_connections.link_response[k] != null) {
                port.link_connections.link_response.remove(k)
            }
            port.link_connections.port_requests[k] = LinkRaycastRequest(
                it.getDouble(1),
                it.optBoolean(2).orElse(false),
                it.optBoolean(3).orElse(false),
                it.optDouble(4).orElse(0.0), // Pitch or Y
                it.optDouble(5).orElse(0.0), // Yaw or X
                it.optDouble(6).orElse(1.0) // planar distance or nil
            )
            val sleep_for =
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.thread_awaiting_sleep_time

            while (Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time - start <= timeout * 1000) {
                val res = port.link_connections.link_response[k]
                if (res == null) {
                    Thread.sleep(sleep_for); continue
                }
                return@JavaToLuaWrapper RaycasterPeripheral.makeRaycastResponse((res as LinkRaycastResponse).result)
            }

            return@JavaToLuaWrapper RaycastERROR("timeout")
        }

        item["getConfigInfo"] = JavaToLuaWrapper {
            val rgc = SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS
            val data = RaycasterPeripheral.makeConfigInfo()

            data["max_allowed_waiting_time"] = rgc.max_allowed_waiting_time
            data["thread_awaiting_sleep_time"] = rgc.thread_awaiting_sleep_time
            data["max_connection_timeout_time"] = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time

            data
        }
    }

    private fun makeBaseGoggleFunctions(
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["get_info"] = JavaToLuaWrapper {
            val v = port.link_connections.constant_updates[k]
            if (!checkConnection(v)) { return@JavaToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated")) }
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