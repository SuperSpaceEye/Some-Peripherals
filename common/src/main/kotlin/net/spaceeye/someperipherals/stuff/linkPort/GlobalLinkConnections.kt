package net.spaceeye.someperipherals.stuff.linkPort

import java.util.UUID
import java.util.WeakHashMap

object GlobalLinkConnections {
    @JvmField var links = WeakHashMap<UUID, LinkConnectionsManager>()
}