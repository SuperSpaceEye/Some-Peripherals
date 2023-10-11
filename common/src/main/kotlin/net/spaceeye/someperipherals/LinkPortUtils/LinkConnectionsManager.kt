package net.spaceeye.someperipherals.LinkPortUtils

import java.util.concurrent.ConcurrentHashMap

class LinkConnectionsManager {
    val constant_updates = ConcurrentHashMap<String, LinkUpdate>()

    val port_requests = ConcurrentHashMap<String, LinkRequest>()
    val link_response = ConcurrentHashMap<String, LinkResponse>()
}