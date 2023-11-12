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
import net.spaceeye.someperipherals.blockentities.GoggleLinkPortBlockEntity
import net.spaceeye.someperipherals.blocks.GoggleLinkPort
import net.spaceeye.someperipherals.integrations.cc.CallbackToLuaWrapper
import net.spaceeye.someperipherals.integrations.cc.FunToLuaWrapper
import net.spaceeye.someperipherals.utils.linkPort.*
import net.spaceeye.someperipherals.utils.mix.Constants
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.integrations.cc.tableToDoubleArray
import net.spaceeye.someperipherals.integrations.cc.tableToTableArray
import net.spaceeye.someperipherals.utils.configToMap.makeGoggleLinkPortConfigInfoBase
import net.spaceeye.someperipherals.utils.configToMap.makeGoggleLinkPortConfigInfoRange

class GoggleLinkPortPeripheral(private val level: Level, private val pos: BlockPos, private var be:BlockEntity):IPeripheral {
    private val connection: LinkConnectionsManager?
    get() {return (be as GoggleLinkPortBlockEntity).connection}

    private inline fun goggleType(connection: LinkPing) =
        when(connection) {
            is Server_RangeGogglesPing -> "range_goggles"
            is Server_StatusGogglesPing -> "status_goggles"
            else -> throw AssertionError("Unknown goggle type ${connection.javaClass.typeName}")
        }

    private inline fun checkConnection(v: LinkPing?) = v != null && connection!!.tick - v.timestamp <= SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks

    @LuaFunction
    fun getConnected(computer: IComputerAccess): Any {
        val ret = mutableMapOf<String, Any>()

        val to_remove = mutableListOf<String>()
        for ((k, v) in connection!!.constant_pings) {
            if (!checkConnection(v)) { to_remove.add(k); continue }

            val item = mutableMapOf<String, Any>()

            makeBaseGoggleFunctions(computer, item, k)
            if (v is Server_RangeGogglesPing) {makeRangeGogglesFunctions(computer, item, k)}

            ret[k] = item
        }
        to_remove.forEach { connection!!.removeAll(it) }

        return ret
    }

    private fun makeRangeGogglesFunctions(
        computer: IComputerAccess,
        item: MutableMap<String, Any>,
        k: String
    ) {
        item["raycast"] = FunToLuaWrapper {args ->
            val ping = connection!!.constant_pings[k]
            val r = connection!!.getRequests(k)

            if (!checkConnection(ping))    { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (r.raycast_request != null) { return@FunToLuaWrapper makeErrorReturn("Connection already has a raycasting request") }

            val start = connection!!.tick

            val distance = args.getDouble(0)
            // at 0 pitch or y, at 1 yaw or x, at 2 nothing or planar distance
            val variables  = tableToDoubleArray(args.optTable(1).orElse(mutableMapOf(Pair(1.0, 0.0), Pair(2.0, 0.0), Pair(3.0, 1.0))))
            val euler_mode = args.optBoolean(2).orElse(false)
            val im_execute = args.optBoolean(3).orElse(true)
            val do_cache   = args.optBoolean(4).orElse(false)
            var check_for_blocks_in_world = args.optBoolean(5).orElse(true)

            if (variables.size < 2 || variables.size > 3) { return@FunToLuaWrapper makeErrorReturn("Variables table should have 2 or 3 items") }
            val var1 = variables[0]
            val var2 = variables[1]
            val var3 = if (variables.size == 3) {variables[2]} else {1.0}

            connection!!.makeRequest(k, LinkRaycastRequest(distance, euler_mode, do_cache, var1, var2, var3, check_for_blocks_in_world))

            var terminate = false
            var pull: MethodResult? = null

            val callback = CallbackToLuaWrapper {
                val cur_tick = connection!!.tick
                val ping = connection!!.constant_pings[k]
                val timeout = SomePeripheralsConfig.SERVER.LINK_PORT_SETTINGS.max_connection_timeout_time_ticks

                if (terminate)                  { connection!!.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Was terminated") }
                if (cur_tick - start > timeout) { connection!!.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Timeout") }
                if (!checkConnection(ping))     { connection!!.getRequests(k).raycast_request = null; return@CallbackToLuaWrapper makeErrorReturn("Timeout") }

                val res = connection!!.getResponses(k).raycast_response
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
            val ping = connection!!.constant_pings[k]
            val r = connection!!.getRequests(k)

            if (!checkConnection(ping))    { return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (r.raycast_request != null) { return@FunToLuaWrapper makeErrorReturn("Connection already has a raycasting request") }

            val data = mutableListOf<Array<Double>>()
            tableToTableArray(args.getTable(1), "Can't convert to table at ")
                .forEachIndexed { idx, it -> data.add(tableToDoubleArray(it, "Can't convert to table at index ${idx} item at ")) }

            connection!!.makeRequest(k, LinkBatchRaycastRequest(
                args.getDouble(0),
                args.optBoolean(2).orElse(false),
                args.optBoolean(3).orElse(false),
                args.optBoolean(4).orElse(true),
                data.toTypedArray()
            ))

            return@FunToLuaWrapper true
        }

        item["getQueuedData"] = FunToLuaWrapper {
            val ping = connection!!.constant_pings[k]
            val r = connection!!.getResponses(k).raycast_response

            if (!checkConnection(ping))         { connection!!.getResponses(k).raycast_response = null; return@FunToLuaWrapper makeErrorReturn("Connection has been terminated") }
            if (r !is LinkBatchRaycastResponse) {                                                       return@FunToLuaWrapper makeErrorReturn("Response is not LinkBatchRaycastResponse") }

            if (r.is_done) {connection!!.getResponses(k).raycast_response = null}

            val data = mutableMapOf<String, Any>()
            data["is_done"] = r.is_done
            val returns = mutableMapOf<Double, Any>()
            //this is to avoid concurrent modification exception
            for (i in 0 until r.results.size) {returns[i.toDouble()+1.0] = RaycasterPeripheral.makeRaycastResponse(r.results[i])}
            data["results"] = returns

            return@FunToLuaWrapper data
        }

        item["getConfigInfo"] = FunToLuaWrapper { makeGoggleLinkPortConfigInfoRange() }
    }

    private fun makeBaseGoggleFunctions(
        computer: IComputerAccess,
        item: MutableMap<String, Any>,
        k: String
    ) {
        item["getInfo"] = FunToLuaWrapper {args ->
            val im_execute = args.optBoolean(0).orElse(true)

            var terminate = false
            var pull: MethodResult? = null

            connection!!.getResponses(k).status_response = null
            connection!!.makeRequest(k, LinkStatusRequest())

            val callback = CallbackToLuaWrapper {
                val ping = connection!!.constant_pings[k]

                if (terminate)              { connection!!.getRequests(k).status_request = null; return@CallbackToLuaWrapper makeErrorReturn("was terminated") }
                if (!checkConnection(ping)) { connection!!.getRequests(k).status_request = null; return@CallbackToLuaWrapper makeErrorReturn("Connection has been terminated") }

                val r = connection!!.getResponses(k).status_response
                if (r == null) {computer.queueEvent(Constants.GOGGLES_GET_INFO_EVENT_NAME); return@CallbackToLuaWrapper pull!!}

                val item = (r as LinkStatusResponse).data
                item["timestamp"] = ping!!.timestamp

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
        item["type"] = connection!!.constant_pings[k]?.let { goggleType(it) } ?: "terminated"
        item["terminateAll"] = FunToLuaWrapper {
            val reqs = connection!!.getRequests(k)
            reqs.status_request = null
            reqs.raycast_request = null

            val rsps = connection!!.getResponses(k)
            rsps.status_response = null
            rsps.raycast_response = null

            return@FunToLuaWrapper Unit
        }
        item["getConfigInfo"] = FunToLuaWrapper { makeGoggleLinkPortConfigInfoBase() }
    }

    override fun equals(p0: IPeripheral?) = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.GOGGLE_LINK_PORT.get())
    override fun getType() = "goggle_link_port"
}