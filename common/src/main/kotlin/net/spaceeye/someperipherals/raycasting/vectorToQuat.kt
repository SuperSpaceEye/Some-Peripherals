package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Quaternion
import net.spaceeye.someperipherals.util.Vector3d

fun vectorsToQuat(dir: Vector3d, up: Vector3d): Quaternion {
    //https://stackoverflow.com/questions/18558910/direction-vector-to-rotation-matrix
    val xaxis = up.cross(dir).snormalize()
    val yaxis = dir.cross(xaxis).snormalize()

    val m = arrayListOf(
        arrayListOf(xaxis.x, yaxis.x, dir.x),
        arrayListOf(xaxis.y, yaxis.y, dir.y),
        arrayListOf(xaxis.z, yaxis.z, dir.z)
    )
    //http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
    val qw = (Math.sqrt(1 + m[0][0] + m[1][1] + m[2][2]) / 2).toFloat()
    val q = Quaternion(
        qw,
        ((m[2][1] - m[1][2]) / (4 * qw)).toFloat(),
        ((m[0][2] - m[2][0]) / (4 * qw)).toFloat(),
        ((m[1][0] - m[0][1]) / (4 * qw)).toFloat()
    )
    return q
}