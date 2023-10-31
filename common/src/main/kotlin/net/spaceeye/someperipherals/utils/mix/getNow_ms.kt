package net.spaceeye.someperipherals.utils.mix

import java.util.*

inline fun getNow_ms() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time