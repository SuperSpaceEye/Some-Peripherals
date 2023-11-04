package net.spaceeye.someperipherals.utils.raycasting

import net.spaceeye.someperipherals.utils.mix.Vector3d

//https://www.shadertoy.com/view/4dX3zl
//https://lodev.org/cgtutor/raycasting.html

class DDAIter(start: Vector3d, stop: Vector3d, up_to: Int): RayIter(start, stop, up_to) {
    private var sideDist : Vector3d
    private var deltaDist : Vector3d
    private var rayStep : Vector3d

    init{
        cpos = Vector3d(start)
        val mapPos = cpos.floor()
        val rd = (stop-start)

        rayStep = rd.sign()
        deltaDist = (rd+1e-200).rdiv(rd.dist()).sabs()
        sideDist = ((rayStep * (mapPos - cpos)) + (rayStep * 0.5) + 0.5) * deltaDist
    }

    inline private fun calcNextPos() {
        if (sideDist.x < sideDist.y) {
            if (sideDist.x < sideDist.z) {
                sideDist.x += deltaDist.x;
                cpos.x += rayStep.x;
            }
            else {
                sideDist.z += deltaDist.z;
                cpos.z += rayStep.z;
            }
        }
        else {
            if (sideDist.y < sideDist.z) {
                sideDist.y += deltaDist.y;
                cpos.y += rayStep.y;
            }
            else {
                sideDist.z += deltaDist.z;
                cpos.z += rayStep.z;
            }
        }
    }

    override fun hasNext(): Boolean {
        return cur_i <= up_to
    }

    override fun next(): Vector3d {
        cur_i++
        val res = Vector3d(cpos.x, cpos.y, cpos.z)
        calcNextPos()
        return res
    }
}