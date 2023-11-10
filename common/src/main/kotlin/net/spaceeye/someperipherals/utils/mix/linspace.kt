package net.spaceeye.someperipherals.utils.mix

import kotlin.math.abs

fun linspace(start: Double, end: Double, num: Int): ArrayList<Double> {
    var _start = start
    var _end = end

    val linspaced = ArrayList<Double>(num)
    if (num == 0) { return linspaced }
    if (num == 1) { linspaced.add(start); return linspaced }

    var displacement = 0.0

    if (start <= 0) {displacement = abs(start) + 1}

    _start += displacement
    _end   += displacement

    val delta = (_end - _start) / (num - 1)

    for (i in 0 until num) {
        linspaced.add((start+delta*i)-displacement)
    }
    _end -= displacement
    linspaced.add(_end)

    return linspaced
}