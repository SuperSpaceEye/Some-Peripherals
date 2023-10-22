package net.spaceeye.someperipherals.LinkPortUtils

import net.spaceeye.someperipherals.util.getNow_ms

abstract class LinkPing { var timestamp = getNow_ms() }

open class Server_StatusGogglesPing: LinkPing()
open class Server_RangeGogglesPing: Server_StatusGogglesPing()