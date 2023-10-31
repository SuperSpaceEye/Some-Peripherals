package net.spaceeye.someperipherals.integrations.cc

inline fun makeErrorReturn(error: String): MutableMap<String, String> = mutableMapOf(Pair("error", error))