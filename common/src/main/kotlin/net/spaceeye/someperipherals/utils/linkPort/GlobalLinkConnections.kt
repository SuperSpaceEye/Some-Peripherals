package net.spaceeye.someperipherals.utils.linkPort

import java.util.UUID
import java.util.WeakHashMap

object GlobalLinkConnections {
    @JvmField var links = WeakHashMap<UUID, LinkConnectionsManager>()
}