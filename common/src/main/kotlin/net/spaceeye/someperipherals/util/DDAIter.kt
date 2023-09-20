//TODO TODO TODO TODO TODO TODO
//https://github.com/francisengelmann/fast_voxel_traversal/blob/master/main.cpp
//https://www.researchgate.net/publication/233899848_Efficient_implementation_of_the_3D-DDA_ray_traversal_algorithm_on_GPU_and_its_application_in_radiation_dose_calculation
//https://lodev.org/cgtutor/raycasting.html
//https://github.com/OneLoneCoder/olcPixelGameEngine/blob/147c25a018c917030e59048b5920c269ef583c50/olcPixelGameEngine.h#L738
//https://github.com/OneLoneCoder/Javidx9/blob/master/PixelGameEngine/SmallerProjects/OneLoneCoder_PGE_RayCastDDA.cpp
//https://www.youtube.com/watch?v=NbSee-XM7WA
//https://www.youtube.com/watch?v=gYRrGTC7GtA
//https://www.youtube.com/watch?v=PC1RaETIx3Y
//https://www.youtube.com/watch?v=w0Bm4IA-Ii8

//https://stackoverflow.com/questions/55263298/draw-all-voxels-that-pass-through-a-3d-line-in-3d-voxel-space

//package net.spaceeye.someperipherals.util
//
//import com.mojang.math.Vector3d
//import java.lang.Math.pow
//import kotlin.math.abs
//import kotlin.math.sqrt
//
//class Vec3i(x_: Number, y_: Number, z_: Number) {
//    var x = x_.toInt()
//    var y = y_.toInt()
//    var z = z_.toInt()
//
//    constructor(other: Vec3i): this(other.x, other.y, other.z) {}
//
//    fun toVector3d(): Vector3d {return Vector3d(x.toDouble(), y.toDouble(), z.toDouble())}
//}

//https://lodev.org/cgtutor/raycasting.html
//https://github.com/OneLoneCoder/Javidx9/blob/master/PixelGameEngine/SmallerProjects/OneLoneCoder_PGE_RayCastDDA.cpp
//class DDAIter(val start: Vector3d, pos2: Vector3d, val max_distance: Double):Iterator<Vector3d> {
//    private val eps = 1e-16
//
//    private var cpos = start
//    private var cpos_i = Vec3i(start.x, start.y, start.z)
//
//    private var cur_i = 0
//
//    private var rd: Vector3d
//    private var d      : Vector3d
//    private var side_d : Vector3d
//    private var step   : Vec3i
//
//    private var distance = 0.0
//
//    private fun norm(vec: Vector3d): Vector3d {
//        val magn = sqrt(pow(vec.x, 2.0) + pow(vec.y, 2.0) + pow(vec.z, 2.0))
//        vec.x /= magn
//        vec.y /= magn
//        vec.z /= magn
//        return vec
//    }
//
//    init{
//        rd = Vector3d(pos2.x - start.x, pos2.y - start.y, pos2.z - start.z)
//        d = Vector3d(
//            abs(1.0/(rd.x + eps)),
//            abs(1.0/(rd.y + eps)),
//            abs(1.0/(rd.z + eps)))
//
//        step = Vec3i(
//            if (rd.x < 0) {-1} else {1},
//            if (rd.y < 0) {-1} else {1},
//            if (rd.z < 0) {-1} else {1}
//        )
//
//        side_d = Vector3d(
//            if (rd.x < 0) {(cpos.x - cpos_i.x) * d.x} else {(cpos_i.x + 1.0 - cpos.x) * d.x},
//            if (rd.y < 0) {(cpos.y - cpos_i.y) * d.y} else {(cpos_i.y + 1.0 - cpos.y) * d.y},
//            if (rd.z < 0) {(cpos.z - cpos_i.z) * d.z} else {(cpos_i.z + 1.0 - cpos.z) * d.z},
//        )
//    }
//
//    private fun calcNextWall(): Vec3i {
//        val ret = Vec3i(cpos_i)
//        if        (side_d.x < side_d.y && side_d.x < side_d.z) {
//            distance = side_d.x
//            side_d.x += d.x
//            cpos_i.x += step.x
//        } else if (side_d.y < side_d.x && side_d.y < side_d.z) {
//            distance = side_d.y
//            side_d.y += d.y
//            cpos_i.y += step.y
//        } else {
//            distance = side_d.z
//            side_d.z += d.z
//            cpos_i.z += step.z
//        }
//        return ret
//    }
//
//    override fun hasNext(): Boolean {
//        return cur_i <= max_distance
//    }
//
//    override fun next(): Vector3d {
//        cur_i++
//        calcNextWall()
//        return Vector3d(
//            start.x + rd.x * distance,
//            start.y + rd.y * distance,
//            start.z + rd.z * distance)
//    }
//
//    fun nextNoStep(): Vector3d {
//        val temp_side_d = Vector3d(side_d.x, side_d.y, side_d.z)
//        val temp_cpos_i = Vec3i(cpos_i)
//        calcNextWall()
//        val res = Vector3d(
//            start.x + rd.x * distance,
//            start.y + rd.y * distance,
//            start.z + rd.z * distance)
//        cpos_i = temp_cpos_i
//        side_d = temp_side_d
//        return res
//    }
//}

//class DDAIter(val start: Vector3d, pos2: Vector3d, val max_distance: Double):Iterator<Vector3d> {
//    private val eps = 1e-16
//
//    private var cpos = start
//    private var cpos_i = Vec3i(start.x, start.y, start.z)
//
//    private var cur_i = 0
//
//    private var rd: Vector3d
//    private var next_voxel_boundary: Vector3d
//    private var tMax : Vector3d
//    private var tDelta : Vector3d
//    private var step : Vector3d
//
//    private var distance = 0.0
//
//    private fun norm(vec: Vector3d): Vector3d {
//        val magn = sqrt(pow(vec.x, 2.0) + pow(vec.y, 2.0) + pow(vec.z, 2.0))
//        vec.x /= magn
//        vec.y /= magn
//        vec.z /= magn
//        return vec
//    }
//
//    init{
//        rd = Vector3d(pos2.x - start.x, pos2.y - start.y, pos2.z - start.z)
//
//        step = Vector3d(
//            if (rd.x < 0) {-1.0} else {1.0},
//            if (rd.y < 0) {-1.0} else {1.0},
//            if (rd.z < 0) {-1.0} else {1.0}
//        )
//
//        next_voxel_boundary = Vector3d(
//            cpos_i.x + step.x,
//            cpos_i.y + step.y,
//            cpos_i.z + step.z
//        )
//
//        tMax = Vector3d(
//            (next_voxel_boundary.x - start.x) / (if (rd.x != 0.0) {rd.x} else {eps}),
//            (next_voxel_boundary.y - start.y) / (if (rd.y != 0.0) {rd.y} else {eps}),
//            (next_voxel_boundary.z - start.z) / (if (rd.z != 0.0) {rd.z} else {eps}),
//        )
//
//        tDelta = Vector3d(
//            1.0/((if (rd.x != 0.0) {rd.x} else {eps}) * step.x),
//            1.0/((if (rd.y != 0.0) {rd.y} else {eps}) * step.y),
//            1.0/((if (rd.z != 0.0) {rd.z} else {eps}) * step.z),
//        )
//    }
//
//    private fun calcNextWall() {
//        if (tMax.x < tMax.y) {
//            if (tMax.x < tMax.z) {
//                cpos.x += step.x
//                tMax.x += tDelta.x
//
//                distance = tMax.x
//            } else {
//                cpos.z += step.z
//                tMax.z += tDelta.z
//
//                distance = tMax.z
//            }
//        } else {
//            if (tMax.y < tMax.z) {
//                cpos.y += step.y
//                tMax.y += tDelta.y
//
//                distance = tMax.y
//            } else {
//                cpos.z += step.z
//                tMax.z += tDelta.z
//
//                distance = tMax.z
//            }
//        }
//    }
//
//    override fun hasNext(): Boolean {
//        return cur_i <= max_distance
//    }
//
//    override fun next(): Vector3d {
//        cur_i++
//        calcNextWall()
//        return Vector3d(
//            start.x + rd.x * distance,
//            start.y + rd.y * distance,
//            start.z + rd.z * distance)
//    }
//
//    fun nextNoStep(): Vector3d {
//        val temp_tmax = Vector3d(tMax.x, tMax.y, tMax.z)
//        val temp_cpos_i = Vec3i(cpos_i)
//        calcNextWall()
//        val res = Vector3d(
//            start.x + rd.x * distance,
//            start.y + rd.y * distance,
//            start.z + rd.z * distance)
//        cpos_i = temp_cpos_i
//        tMax = temp_tmax
//        return res
//    }
//}

//class DDAIter(val start: Vector3d, stop: Vector3d, val max_distance: Double):Iterator<Vector3d> {
//    private var cpos = start
//
//    private var cur_i = 0
//
//    private var d: Vector3d
//    private var tMax : Vector3d
//    private var tDelta : Vector3d
//    private var step : Vector3d
//
//    private var distance = 0.0
//
//    private fun hypot(vec: Vector3d): Double {
//        return sqrt(pow(vec.x, 2.0) + pow(vec.y, 2.0) + pow(vec.z, 2.0))
//    }
//
//    init{
//        d = Vector3d(abs(stop.x - start.x), abs(stop.y - start.y), abs(stop.z - start.z))
//
//        step = Vector3d(
//            if (stop.x < start.x) {-1.0} else {1.0},
//            if (stop.y < start.y) {-1.0} else {1.0},
//            if (stop.z < start.z) {-1.0} else {1.0}
//        )
//
//        val hypotenuse = hypot(d)
//
//        tMax = Vector3d(
//            hypotenuse * 0.5 / d.x,
//            hypotenuse * 0.5 / d.y,
//            hypotenuse * 0.5 / d.z,
//        )
//
//        tDelta = Vector3d(
//            hypotenuse / d.x,
//            hypotenuse / d.y,
//            hypotenuse / d.z,
//        )
//    }
//
//    private fun calcNextWall() {
//        if (tMax.x < tMax.y) {
//            if (tMax.x < tMax.z) {
//                cpos.x += step.x
//                tMax.x += tDelta.x
//            } else if (tMax.x > tMax.z) {
//                cpos.z += step.z
//                tMax.z += tDelta.z
//            } else {
//                cpos.x += step.x
//                tMax.x += tDelta.x
//
//                cpos.z += step.z
//                tMax.z += tDelta.z
//            }
//        } else if (tMax.x > tMax.y) {
//            if (tMax.y < tMax.z) {
//                cpos.y += step.y
//                tMax.y += tDelta.y
//            } else if (tMax.y > tMax.z) {
//                cpos.z += step.z
//                tMax.z += tDelta.z
//            } else {
//                cpos.y += step.y
//                tMax.y += tDelta.y
//
//                cpos.z += step.z
//                tMax.z += tDelta.z
//            }
//        } else {
//            if (tMax.y < tMax.z) {
//                cpos.y += step.y
//                tMax.y += tDelta.y
//
//                cpos.x += step.x
//                tMax.x += tDelta.x
//            } else if (tMax.y > tMax.z) {
//                cpos.z += step.z
//                tMax.z += tDelta.z
//
//            } else {
//                cpos.x += step.x
//                tMax.x += tDelta.x
//
//                cpos.y += step.y
//                tMax.y += tDelta.y
//
//                cpos.z += step.z
//                tMax.z += tDelta.z
//            }
//        }
//    }
//
//    override fun hasNext(): Boolean {
//        return cur_i <= max_distance
//    }
//
//    override fun next(): Vector3d {
//        cur_i++
//        calcNextWall()
//        return Vector3d(cpos.x, cpos.y, cpos.z)
//    }
//
//    fun nextNoStep(): Vector3d {
//        val temp_tmax = Vector3d(tMax.x, tMax.y, tMax.z)
//        val temp_cpos = Vector3d(cpos.x, cpos.y, cpos.z)
//        calcNextWall()
//        val res = Vector3d(cpos.x, cpos.y, cpos.z)
//        cpos = temp_cpos
//        tMax = temp_tmax
//        return res
//    }
//}