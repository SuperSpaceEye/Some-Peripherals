package net.spaceeye.someperipherals.raycasting

import net.spaceeye.someperipherals.util.Vector3d
import kotlin.math.floor

//https://lodev.org/cgtutor/raycasting.html
//https://stackoverflow.com/questions/55263298/draw-all-voxels-that-pass-through-a-3d-line-in-3d-voxel-space
class BresenhamIter(start: Vector3d, stop: Vector3d, up_to: Int):RayIter(start, stop, up_to) {
    private var cpos = Vector3d(start)

    private var rd: Vector3d
    private var tMax : Vector3d
    private var tDelta : Vector3d
    private var step : Vector3d

    init{
        rd = (stop-start).abs()
        tDelta = (rd+1e-60).rdiv(1.0).abs()

        step = Vector3d(
            if (stop.x < start.x) {-1.0} else {1.0},
            if (stop.y < start.y) {-1.0} else {1.0},
            if (stop.z < start.z) {-1.0} else {1.0}
        )

        tMax = Vector3d(
            if (stop.x < start.x) {(cpos.x - floor(cpos.x)) * tDelta.x} else {(floor(cpos.x) + 1.0 - cpos.x) * tDelta.x},
            if (stop.y < start.y) {(cpos.y - floor(cpos.y)) * tDelta.y} else {(floor(cpos.y) + 1.0 - cpos.y) * tDelta.y},
            if (stop.z < start.z) {(cpos.z - floor(cpos.z)) * tDelta.z} else {(floor(cpos.z) + 1.0 - cpos.z) * tDelta.z},
        )
    }

    inline private fun calcNextPos() {
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