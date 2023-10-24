package net.spaceeye.someperipherals.util

inline fun makeCCErrorReturn(error: String): MutableMap<String, String> {
    return mutableMapOf(Pair("error", error))
}