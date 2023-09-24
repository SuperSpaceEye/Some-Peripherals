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
//https://lodev.org/cgtutor/raycasting.html
//https://github.com/OneLoneCoder/Javidx9/blob/master/PixelGameEngine/SmallerProjects/OneLoneCoder_PGE_RayCastDDA.cpp

//https://stackoverflow.com/questions/55263298/draw-all-voxels-that-pass-through-a-3d-line-in-3d-voxel-space

package net.spaceeye.someperipherals.util

import com.mojang.math.Vector3d
import java.lang.Math.pow
import kotlin.math.abs
import kotlin.math.sqrt

class DDAIter(val start: Vector3d, val stop: Vector3d, val up_to: Int):Iterator<Vector3d> {
    private var cpos = Vector3d(start.x, start.y, start.z)

    var cur_i = 0

    private var rd: Vector3d
    private var tMax : Vector3d
    private var tDelta : Vector3d
    private var step : Vector3d

    private fun hypot(vec: Vector3d): Double {
        return sqrt(pow(vec.x, 2.0) + pow(vec.y, 2.0) + pow(vec.z, 2.0))
    }

    init{
        rd = Vector3d(abs(stop.x - start.x), abs(stop.y - start.y), abs(stop.z - start.z))

        step = Vector3d(
            if (stop.x < start.x) {-1.0} else {1.0},
            if (stop.y < start.y) {-1.0} else {1.0},
            if (stop.z < start.z) {-1.0} else {1.0}
        )

        val hypotenuse = hypot(rd)

        tMax = Vector3d(
            hypotenuse * 0.5 / rd.x,
            hypotenuse * 0.5 / rd.y,
            hypotenuse * 0.5 / rd.z,
        )

        tDelta = Vector3d(
            hypotenuse / rd.x,
            hypotenuse / rd.y,
            hypotenuse / rd.z,
        )
    }

    private fun calcNextPos() {
        if (tMax.x < tMax.y) {
            if (tMax.x < tMax.z) {
                cpos.x += step.x
                tMax.x += tDelta.x
            } else if (tMax.x > tMax.z) {
                cpos.z += step.z
                tMax.z += tDelta.z
            } else {
                cpos.x += step.x
                tMax.x += tDelta.x

                cpos.z += step.z
                tMax.z += tDelta.z
            }
        } else if (tMax.x > tMax.y) {
            if (tMax.y < tMax.z) {
                cpos.y += step.y
                tMax.y += tDelta.y
            } else if (tMax.y > tMax.z) {
                cpos.z += step.z
                tMax.z += tDelta.z
            } else {
                cpos.y += step.y
                tMax.y += tDelta.y

                cpos.z += step.z
                tMax.z += tDelta.z
            }
        } else {
            if (tMax.y < tMax.z) {
                cpos.y += step.y
                tMax.y += tDelta.y

                cpos.x += step.x
                tMax.x += tDelta.x
            } else if (tMax.y > tMax.z) {
                cpos.z += step.z
                tMax.z += tDelta.z

            } else {
                cpos.x += step.x
                tMax.x += tDelta.x

                cpos.y += step.y
                tMax.y += tDelta.y

                cpos.z += step.z
                tMax.z += tDelta.z
            }
        }
    }

    override fun hasNext(): Boolean {
        return cur_i <= up_to
    }

    override fun next(): Vector3d {
        cur_i++
        calcNextPos()
        return Vector3d(cpos.x, cpos.y, cpos.z)
    }

    fun nextNoStep(): Vector3d {
        val temp_tmax = Vector3d(tMax.x, tMax.y, tMax.z)
        val temp_cpos = Vector3d(cpos.x, cpos.y, cpos.z)
        calcNextPos()
        val res = Vector3d(cpos.x, cpos.y, cpos.z)
        cpos = temp_cpos
        tMax = temp_tmax
        return res
    }
}