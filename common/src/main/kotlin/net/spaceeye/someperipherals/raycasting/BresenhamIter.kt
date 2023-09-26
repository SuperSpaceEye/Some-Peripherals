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

//https://stackoverflow.com/questions/55263298/draw-all-voxels-that-pass-through-a-3d-line-in-3d-voxel-space

//https://www.flipcode.com/archives/Raytracing_Topics_Techniques-Part_4_Spatial_Subdivisions.shtml
//https://gamedev.stackexchange.com/questions/81267/how-do-i-generalise-bresenhams-line-algorithm-to-floating-point-endpoints/81332#81332
//https://arxiv.org/pdf/1609.00958.pdf
//https://stackoverflow.com/questions/24679963/precise-subpixel-line-drawing-algorithm-rasterization-algorithm


//https://stackoverflow.com/questions/24679963/precise-subpixel-line-drawing-algorithm-rasterization-algorithm/24682318#24682318
package net.spaceeye.someperipherals.util

import com.mojang.math.Vector3d
import net.spaceeye.someperipherals.raycasting.RayIter
import kotlin.math.abs
import kotlin.math.floor

class BresenhamIter(start: Vector3d, stop: Vector3d, up_to: Int):RayIter(start, stop, up_to) {
    private var cpos = Vector3d(start.x, start.y, start.z)

    private var rd: Vector3d
    private var tMax : Vector3d
    private var tDelta : Vector3d
    private var step : Vector3d

    init{
        rd = Vector3d(abs(stop.x - start.x), abs(stop.y - start.y), abs(stop.z - start.z))

        step = Vector3d(
            if (stop.x < start.x) {-1.0} else {1.0},
            if (stop.y < start.y) {-1.0} else {1.0},
            if (stop.z < start.z) {-1.0} else {1.0}
        )

        tDelta = Vector3d(
            abs(1.0/(rd.x + 1e-60)),
            abs(1.0/(rd.y + 1e-60)),
            abs(1.0/(rd.z + 1e-60)))

        tMax = Vector3d(
            if (stop.x < start.x) {(cpos.x - floor(cpos.x)) * tDelta.x} else {(floor(cpos.x) + 1.0 - cpos.x) * tDelta.x},
            if (stop.y < start.y) {(cpos.y - floor(cpos.y)) * tDelta.y} else {(floor(cpos.y) + 1.0 - cpos.y) * tDelta.y},
            if (stop.z < start.z) {(cpos.z - floor(cpos.z)) * tDelta.z} else {(floor(cpos.z) + 1.0 - cpos.z) * tDelta.z},
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
}