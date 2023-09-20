package net.spaceeye.someperipherals.util

import com.mojang.math.Vector3d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

class IterateBetweenTwoPointsIter(val start: Vector3d, pos2: Vector3d, val max_len:Int):Iterator<Vector3d> {
    var cpos = start;
    var x_step: Double = 0.0
    var y_step: Double = 0.0
    var z_step: Double = 0.0
    var cur_i = 0
    var up_to = 0

    init {
        val x_diff = start.x - pos2.x
        val y_diff = start.y - pos2.y
        val z_diff = start.z - pos2.z

        val x_modif = if (x_diff < 0) {1.0} else {-1.0}
        val y_modif = if (y_diff < 0) {1.0} else {-1.0}
        val z_modif = if (z_diff < 0) {1.0} else {-1.0}

        val x_is_larger = abs(x_diff) > abs(y_diff) && abs(x_diff) > abs(z_diff)
        val y_is_larger = abs(y_diff) > abs(x_diff) && abs(y_diff) > abs(z_diff)
        val z_is_larger = abs(z_diff) > abs(y_diff) && abs(z_diff) > abs(x_diff)

        val longer_side_length = max(max(abs(x_diff), abs(y_diff)), abs(z_diff))

        x_step = if(x_is_larger) {1.0} else {abs(x_diff) / longer_side_length} * x_modif
        y_step = if(y_is_larger) {1.0} else {abs(y_diff) / longer_side_length} * y_modif
        z_step = if(z_is_larger) {1.0} else {abs(z_diff) / longer_side_length} * z_modif

        cpos = Vector3d(start.x, start.y, start.z)
        up_to = round(longer_side_length).toInt()
    }

    override fun hasNext(): Boolean {
        return (cur_i < max_len || max_len <= 0) && cur_i <= up_to
    }

    override fun next(): Vector3d {
        cpos.x += x_step
        cpos.y += y_step
        cpos.z += z_step
        cur_i++
        return Vector3d(cpos.x, cpos.y, cpos.z)
    }

    fun nextNoStep(): Vector3d {
        return Vector3d(cpos.x + x_step, cpos.y + y_step, cpos.z + z_step)
    }
}

fun iterateBetweenTwoPoints(pos1: Vector3d, pos2: Vector3d): ArrayList<Vector3d> {
    if (pos1 == pos2) {return arrayListOf(pos1)}

    val points = arrayListOf(pos1)

    val x_diff = pos1.x - pos2.x
    val y_diff = pos1.y - pos2.y
    val z_diff = pos1.z - pos2.z

    val x_modif = if (x_diff < 0) {1.0} else {-1.0}
    val y_modif = if (y_diff < 0) {1.0} else {-1.0}
    val z_modif = if (z_diff < 0) {1.0} else {-1.0}

    val x_is_larger = abs(x_diff) > abs(y_diff) && abs(x_diff) > abs(z_diff)
    val y_is_larger = abs(y_diff) > abs(x_diff) && abs(y_diff) > abs(z_diff)
    val z_is_larger = abs(z_diff) > abs(y_diff) && abs(z_diff) > abs(x_diff)

    val longer_side_length = max(max(abs(x_diff), abs(y_diff)), abs(z_diff))

    val x_step = if(x_is_larger) {1.0} else {abs(x_diff) / longer_side_length} * x_modif
    val y_step = if(y_is_larger) {1.0} else {abs(y_diff) / longer_side_length} * y_modif
    val z_step = if(z_is_larger) {1.0} else {abs(z_diff) / longer_side_length} * z_modif

    val cpos = Vector3d(pos1.x, pos1.y, pos1.z)
    for (i in 0 .. round(longer_side_length).toInt()) {
        cpos.x += x_step
        cpos.y += y_step
        cpos.z += z_step

        points.add(Vector3d(cpos.x, cpos.y, cpos.z))
    }

    return points
}