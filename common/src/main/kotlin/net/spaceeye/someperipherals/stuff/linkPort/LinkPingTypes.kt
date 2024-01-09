package net.spaceeye.someperipherals.stuff.linkPort

abstract class LinkPing(var timestamp: Long)

open class Server_StatusGogglesPing(timestamp: Long): LinkPing(timestamp)
open class Server_RangeGogglesPing(timestamp: Long): Server_StatusGogglesPing(timestamp)