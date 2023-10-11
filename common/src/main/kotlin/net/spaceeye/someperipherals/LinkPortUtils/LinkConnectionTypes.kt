package net.spaceeye.someperipherals.LinkPortUtils

import java.util.*

abstract class LinkUpdate() {
    var timestamp = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time
}

open class Server_EntityPhysUpdate(var data: MutableMap<String, Any>): LinkUpdate()
class Server_RangeGogglesPhysUpdate(data: MutableMap<String, Any>): Server_EntityPhysUpdate(data)