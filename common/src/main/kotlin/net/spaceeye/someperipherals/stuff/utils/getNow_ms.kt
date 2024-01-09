package net.spaceeye.someperipherals.stuff.utils

import java.util.*

//cc:tweaked uses this
inline fun getNow_ms() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time.time

inline fun getNowFast_ms() = System.currentTimeMillis()