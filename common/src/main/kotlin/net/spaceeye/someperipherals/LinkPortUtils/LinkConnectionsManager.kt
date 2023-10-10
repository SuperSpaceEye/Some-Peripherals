package net.spaceeye.someperipherals.LinkPortUtils

import java.util.concurrent.ConcurrentHashMap

class LinkConnectionsManager {
    val updates = ConcurrentHashMap<String, LinkUpdate>()
}