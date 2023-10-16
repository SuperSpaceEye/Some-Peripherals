package net.spaceeye.someperipherals.util

import java.util.*

inline fun getNow_ms() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time