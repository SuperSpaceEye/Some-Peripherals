package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.lua.MethodResult
import dan200.computercraft.api.peripheral.IComputerAccess
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.integrations.cc.CallbackToLuaWrapper
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.utils.linkPort.*
import net.spaceeye.someperipherals.utils.mix.Constants
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.integrations.cc.tableToDoubleArray
import net.spaceeye.someperipherals.integrations.cc.tableToTableArray
import net.spaceeye.someperipherals.utils.configToMap.makeRaycastingConfigInfo

class GoggleLinkPortPeripheral(private val level: Level, private val pos: BlockPos, private var be:BlockEntity):IPeripheral {
    private var block = (be.blockState.block as GoggleLinkPort)

    private fun goggleType(connection: LinkPing): String {
        return when(connection) {
            is Server_RangeGogglesPing -> "range_goggles"
            is Server_StatusGogglesPing -> "status_goggles"
            else -> throw AssertionError("Unknown goggle type ${connection.javaClass.typeName}")
        }
    }

    private fun checkConnection(v: LinkPing?): Boolean {
        if (v == null) {return false}
        if (block.link_connections.tick - v.timestamp > SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks) {return false}
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
            if (v is Server_RangeGogglesPing) {makeRangeGogglesFunctions(computer, item, port, k)}

            ret[k] = item
        }
        to_remove.forEach { port.link_connections.removeAll(it) }

        return ret
    }

    private fun makeRangeGogglesFunctions(
        computer: IComputerAccess,
        item: MutableMap<String, Any>,
        port: GoggleLinkPort,
        k: String
    ) {
        item["raycast"] = FunToLuaWrapper {args ->
            val ping = port.link_connections.constant_pings[k]
            val requests = port.link_connections.getRequests(k)

            if (!checkConnection(ping))           { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (requests.raycast_request != null) { return@FunToLuaWrapper makeErrorReturn("Connection already has a raycasting request") }

            val start = block.link_connections.tick

            val distance = args.getDouble(0)
            // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
            val variables  = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
            val euler_mode = args.optBoolean(2).orElse(false)
            val do_cache   = args.optBoolean(3).orElse(false)
            val im_execute = args.optBoolean(4).orElse(true)

            if (variables.size < 2 || variables.size > 3) { return@FunToLuaWrapper makeErrorReturn("Variables table should have 2 or 3 items") }
            val var1 = variables[0]
            val var2 = variables[1]
            val var3 = if (variables.size == 3) {variables[2]} else {1.0}

            port.link_connections.makeRequest(k, LinkRaycastRequest(distance, euler_mode, do_cache, var1, var2, var3))

            var terminate = false
            var pull: MethodResult? = null

            val callback = CallbackToLuaWrapper {
                val cur_tick = block.link_connections.tick
                val ping = port.link_connections.constant_pings[k]
                val timeout = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks

                if (terminate)                  { port.link_connections.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Was terminated") }
                if (cur_tick - start > timeout) { port.link_connections.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Timeout") }
                if (!checkConnection(ping))     { port.link_connections.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Timeout") }

                val res = port.link_connections.getResponses(k).raycast_response
                if (res == null) {computer.queueEvent(Constants.GOGGLES_RAYCAST_EVENT_NAME); return@CallbackToLuaWrapper pull!!}
                if (res !is LinkRaycastResponse) {return@CallbackToLuaWrapper makeErrorReturn("res not a LinkRaycastResponse") }

                return@CallbackToLuaWrapper RaycasterPeripheral.makeRaycastResponse(res.result)
            }

            pull = MethodResult.pullEvent(Constants.GOGGLES_RAYCAST_EVENT_NAME, callback)

            if (im_execute) {
                computer.queueEvent(Constants.GOGGLES_RAYCAST_EVENT_NAME)
                return@FunToLuaWrapper pull
            } else {
                return@FunToLuaWrapper mutableMapOf(
                    Pair("begin", FunToLuaWrapper{computer.queueEvent(Constants.GOGGLES_RAYCAST_EVENT_NAME); return@FunToLuaWrapper pull}),
                    Pair("terminate", FunToLuaWrapper { terminate = true; return@FunToLuaWrapper Unit })
                )
            }
        }

        item["queueRaycasts"] = FunToLuaWrapper {args->
            val ping = port.link_connections.constant_pings[k]
            val requests = port.link_connections.getRequests(k)

            if (!checkConnection(ping))           { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (requests.raycast_request != null) { return@FunToLuaWrapper makeErrorReturn("Connection already has a raycasting request") }

            val data = mutableListOf<Array<Double>>()
            tableToTableArray(args.getTable(1), "Can't convert to table at ")
                .forEachIndexed { idx, it -> data.add(tableToDoubleArray(it, "Can't convert to table at index ${idx} item at ")) }

            port.link_connections.makeRequest(k, LinkBatchRaycastRequest(
                args.getDouble(0),
                args.optBoolean(2).orElse(false),
                args.optBoolean(3).orElse(false),
                data.toTypedArray()
            ))

            var terminated = false

            return@FunToLuaWrapper mutableMapOf(
        Pair("getQueuedData", FunToLuaWrapper {
            if (terminated) { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            val ping = port.link_connections.constant_pings[k]
            val r = port.link_connections.getResponses(k).raycast_response

            if (!checkConnection(ping))         { port.link_connections.getResponses(k).raycast_response = null; return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (r !is LinkBatchRaycastResponse) {                                                                return@FunToLuaWrapper makeErrorReturn("Response is not LinkBatchRaycastResponse") }

            if (r.is_done) {port.link_connections.getResponses(k).raycast_response = null}

            val data = mutableMapOf<String, Any>()
            data["is_done"] = r.is_done
            val returns = mutableMapOf<Double, Any>()
            //this is to avoid concurrent modification exception
            for (i in 0 until r.results.size) {returns[i.toDouble()+1.0] = RaycasterPeripheral.makeRaycastResponse(r.results[i])}
            data["results"] = returns

            return@FunToLuaWrapper data
        }),
        Pair("terminate", FunToLuaWrapper{
            if (terminated) { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            val ping = port.link_connections.constant_pings[k]
            val r = port.link_connections.getRequests(k).raycast_request

            if (!checkConnection(ping))        { port.link_connections.getResponses(k).raycast_response = null; return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (r !is LinkBatchRaycastRequest) {                                                                return@FunToLuaWrapper makeErrorReturn("Response is not LinkBatchRaycastResponse") }

            r.do_terminate = true
            terminated = true

            return@FunToLuaWrapper true
        })
            )
        }

        item["getConfigInfo"] = FunToLuaWrapper {
            val data = makeRaycastingConfigInfo()
            val rgc = SomePeripheralsConfig.SERVER.GOGGLE_SETTINGS.RANGE_GOGGLES_SETTINGS

            data["max_allowed_waiting_time"] = rgc.max_allowed_raycast_waiting_time_ms
            data["max_connection_timeout_time"] = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks

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
                val ping = port.link_connections.constant_pings[k]

                if (terminate)              { port.link_connections.getRequests(k).status_request = null; return@CallbackToLuaWrapper makeErrorReturn("was terminated") }
                if (!checkConnection(ping)) { port.link_connections.getRequests(k).status_request = null; return@CallbackToLuaWrapper makeErrorReturn("Connection has been terminated") }

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
        item["terminateAll"] = FunToLuaWrapper {
            val reqs = port.link_connections.getRequests(k)
            reqs.status_request = null
            reqs.raycast_request = null

            val rsps = port.link_connections.getResponses(k)
            rsps.status_response = null
            rsps.raycast_response = null

            return@FunToLuaWrapper Unit
        }
    }

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())
    override fun getType(): String = "goggle_link_port"
}