package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.LinkPortUtils.*
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.integrations.cc.CallbackToLuaWrapper
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.raycasting.RaycastERROR
import net.spaceeye.someperipherals.util.Constants
import net.spaceeye.someperipherals.util.getNow_ms
import net.spaceeye.someperipherals.util.tableToDoubleArray
import net.spaceeye.someperipherals.util.tableToTableArray

//TODO refactor and simplify logic
class GoggleLinkPortPeripheral(private val level: Level, private val pos: BlockPos):IPeripheral {
    private var be = level.getBlockEntity(pos) as GoggleLinkPortBlockEntity

    private fun goggleType(connection: LinkPing): String {
        return when(connection) {
            is Server_RangeGogglesPing -> "range_goggles"
            is Server_StatusGogglesPing -> "status_goggles"
            else -> throw AssertionError("Unknown goggle type")
        }
    }

    private fun checkConnection(v: LinkPing?): Boolean {
        if (v == null) {return false}
        if (getNow_ms() - v.timestamp > SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ms) {return false}

        return true
    }

    @LuaFunction
    fun getConnected(computer: IComputerAccess): Any {
        val ret = mutableMapOf<String, Any>()
        val port = (be.blockState.block as GoggleLinkPort)

        val to_remove = mutableListOf<String>()
        for ((k, v) in port.link_connections.constant_pings) {
            if (!checkConnection(v)) { to_remove.add(k);continue}

            val item = mutableMapOf<String, Any>()
            makeBaseGoggleFunctions(computer, item, port, k)
            if (v is Server_RangeGogglesPing) {makeRangeGogglesFunctions(item, port, k)}

            ret[k] = item
        }
        to_remove.forEach { port.link_connections.removeAll(it) }

        return ret
    }

    private fun makeRangeGogglesFunctions(
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["raycast"] = FunToLuaWrapper {args ->
            val ping = port.link_connections.constant_pings[k]
            if (!checkConnection(ping)) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated")) }
            val requests = port.link_connections.getRequests(k)

            if (requests.raycast_request != null) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection already has a raycasting request")) }

            val start = getNow_ms()

            val distance = args.getDouble(0)
            // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
            val variables  = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
            val euler_mode = args.optBoolean(2).orElse(false)
            val do_cache   = args.optBoolean(4).orElse(false)

            if (variables.size < 2 || variables.size > 3) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Variables table should have 2 or 3 items")) }
            val var1 = variables[0]
            val var2 = variables[1]
            val var3 = if (variables.size == 3) {variables[2]} else {1.0}

            port.link_connections.makeRequest(k, LinkRaycastRequest(
                distance,
                euler_mode,
                do_cache,
                var1, var2, var3
            ))
            val sleep_for =
                SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS.thread_awaiting_sleep_time_ms

            //TODO rework
            while (getNow_ms() - start <= SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ms/1000) {
                val res = port.link_connections.getResponses(k).raycast_response
                if (res == null) { Thread.sleep(sleep_for); continue }
                if (res !is LinkRaycastResponse) {return@FunToLuaWrapper RaycasterPeripheral.makeRaycastResponse(RaycastERROR("Response type is not LinkRaycastResponse"))}
                return@FunToLuaWrapper RaycasterPeripheral.makeRaycastResponse(res.result)
            }

            return@FunToLuaWrapper RaycasterPeripheral.makeRaycastResponse(RaycastERROR("timeout"))
        }

        item["queueRaycasts"] = FunToLuaWrapper {args->
            val ping = port.link_connections.constant_pings[k]
            if (!checkConnection(ping)) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated")) }
            val requests = port.link_connections.getRequests(k)

            if (requests.raycast_request != null) { return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection already has a raycasting request")) }

            val start = getNow_ms()

            val data = mutableListOf<Array<Double>>()
            tableToTableArray(args.getTable(1), "Can't convert to table at ")
                .forEachIndexed { idx, it -> data.add(tableToDoubleArray(it, "Can't convert to table at index ${idx} item at ")) }

            port.link_connections.makeRequest(k, LinkBatchRaycastRequest(
                args.getDouble(0),
                args.optBoolean(2).orElse(false),
                args.optBoolean(3).orElse(false),
                data.toTypedArray(),
                start,
                1
            ))

            return@FunToLuaWrapper true
        }

        item["getQueuedData"] = FunToLuaWrapper {
            val ping = port.link_connections.constant_pings[k]
            if (!checkConnection(ping)) {
                port.link_connections.getResponses(k).raycast_response = null
                return@FunToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated"))
            }

            val r = port.link_connections.getResponses(k).raycast_response
            if (r !is LinkBatchRaycastResponse) {return@FunToLuaWrapper mutableMapOf(Pair("error", "Response is not batch raycast"))}

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
        computer: IComputerAccess,
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["getInfo"] = FunToLuaWrapper {args ->
            val im_execute = args.optBoolean(0).orElse(true)

            var terminate = false
            var pull: MethodResult? = null

            port.link_connections.getResponses(k).status_response = null
            port.link_connections.makeRequest(k, LinkStatusRequest())

            val callback = CallbackToLuaWrapper {
                if (terminate) {
                    port.link_connections.getRequests(k).status_request = null
                    return@CallbackToLuaWrapper mutableMapOf(Pair("error", "was terminated"))}

                val ping = port.link_connections.constant_pings[k]
                if (!checkConnection(ping)) {
                    port.link_connections.getRequests(k).status_request = null
                    return@CallbackToLuaWrapper mutableMapOf(Pair("error", "Connection has been terminated"))}

                val r = port.link_connections.getResponses(k).status_response
                if (r == null) {computer.queueEvent(Constants.GOGGLES_GET_INFO_EVENT_NAME); return@CallbackToLuaWrapper pull!!}

                val item = (r as LinkStatusResponse).data
                item["timestamp"] = ping!!.timestamp
                item["goggle_type"] = goggleType(ping)

                return@CallbackToLuaWrapper item
            }

            pull = MethodResult.pullEvent(Constants.GOGGLES_GET_INFO_EVENT_NAME, callback)

            if (im_execute) {
                computer.queueEvent(Constants.GOGGLES_GET_INFO_EVENT_NAME)
                return@FunToLuaWrapper pull
            } else {
                return@FunToLuaWrapper mutableMapOf(
                    Pair("begin", FunToLuaWrapper{computer.queueEvent(Constants.GOGGLES_GET_INFO_EVENT_NAME); return@FunToLuaWrapper pull}),
                    Pair("terminate", FunToLuaWrapper { terminate = true; return@FunToLuaWrapper Unit })
                )
            }
        }
        item["type"] = port.link_connections.constant_pings[k]?.let { goggleType(it) } ?: "terminated"
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())
    override fun getType(): String = "goggle_link_port"
}