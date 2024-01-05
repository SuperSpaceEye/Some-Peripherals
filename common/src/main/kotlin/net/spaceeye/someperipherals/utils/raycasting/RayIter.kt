package net.spaceeye.someperipherals.utils.raycasting

import net.spaceeye.someperipherals.utils.mix.Vector3d

//TODO inline this
abstract class RayIter(var start: Vector3d, var stop: Vector3d, var up_to: Int): Iterator<Vector3d> {
    var cur_i = 0
    var cpos = Vector3d()
}