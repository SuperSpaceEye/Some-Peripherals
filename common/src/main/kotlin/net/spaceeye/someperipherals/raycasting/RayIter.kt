package net.spaceeye.someperipherals.raycasting

import net.spaceeye.someperipherals.util.Vector3d

abstract class RayIter(var start: Vector3d, var stop: Vector3d, var up_to: Int): Iterator<Vector3d> {
    var cur_i = 0
    var cpos = Vector3d()
}