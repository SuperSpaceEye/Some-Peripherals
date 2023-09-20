package net.spaceeye.someperipherals.util

import com.mojang.math.Vector3d
import kotlin.math.abs

class Vec3i(x_: Number, y_: Number, z_: Number) {
    var x = x_.toInt()
    var y = y_.toInt()
    var z = z_.toInt()

    constructor(other: Vec3i): this(other.x, other.y, other.z) {}

    fun toVector3d(): Vector3d {return Vector3d(x.toDouble(), y.toDouble(), z.toDouble())}
}

//https://lodev.org/cgtutor/raycasting.html
//https://github.com/OneLoneCoder/Javidx9/blob/master/PixelGameEngine/SmallerProjects/OneLoneCoder_PGE_RayCastDDA.cpp
class DDAIter(val start: Vector3d, pos2: Vector3d, val max_distance: Double):Iterator<Vector3d> {
    private val eps = 1e-16

    private var cpos = start
    private var cpos_i = Vec3i(start.x, start.y, start.z)

    private var cur_i = 0

    private var ray_dir: Vector3d
    private var d      : Vector3d
    private var side_d : Vector3d
    private var step   : Vec3i

    private var distance = 0.0

    init{
        ray_dir = Vector3d(pos2.x - start.x, pos2.y - start.y, pos2.z - start.z)
        d = Vector3d(
            abs(1.0/(ray_dir.x + eps)),
            abs(1.0/(ray_dir.y + eps)),
            abs(1.0/(ray_dir.z + eps)))

        step = Vec3i(
            if (d.x < 0) {-1} else {1},
            if (d.y < 0) {-1} else {1},
            if (d.z < 0) {-1} else {1}
        )

        side_d = Vector3d(
            if (d.x < 0) {(cpos.x - cpos_i.x) * d.x} else {(cpos_i.x + 1.0 - cpos.x) * d.x},
            if (d.y < 0) {(cpos.y - cpos_i.y) * d.y} else {(cpos_i.y + 1.0 - cpos.y) * d.y},
            if (d.z < 0) {(cpos.z - cpos_i.z) * d.z} else {(cpos_i.z + 1.0 - cpos.z) * d.z},
        )
    }

    private fun calcNextWall(): Vec3i {
        val ret = Vec3i(cpos_i)
        if        (side_d.x < side_d.y && side_d.x < side_d.z) {
            distance = side_d.x
            side_d.x += d.x
            cpos_i.x += step.x
        } else if (side_d.y < side_d.x && side_d.y < side_d.z) {
            distance = side_d.y
            side_d.y += d.y
            cpos_i.y += step.y
        } else {
            distance = side_d.z
            side_d.z += d.z
            cpos_i.z += step.z
        }
        return ret
    }

    override fun hasNext(): Boolean {
        return cur_i <= max_distance
    }

    override fun next(): Vector3d {
        cur_i++
        calcNextWall()
        return Vector3d(
            start.x + ray_dir.x * distance,
            start.y + ray_dir.y * distance,
            start.z + ray_dir.z * distance)
    }

    fun nextNoStep(): Vector3d {
        val temp_side_d = Vector3d(side_d.x, side_d.y, side_d.z)
        val temp_cpos_i = Vec3i(cpos_i)
        calcNextWall()
        val res = Vector3d(
            start.x + ray_dir.x * distance,
            start.y + ray_dir.y * distance,
            start.z + ray_dir.z * distance)
        cpos_i = temp_cpos_i
        side_d = temp_side_d
        return res
    }
}