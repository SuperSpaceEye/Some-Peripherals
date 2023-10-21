package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Quaternion
import net.spaceeye.someperipherals.util.Vector3d

//TODO doesn't work
fun vectorsToQuat(dir: Vector3d, up: Vector3d, right: Vector3d): Quaternion {
    //https://stackoverflow.com/questions/18558910/direction-vector-to-rotation-matrix
//    val xaxis = up.cross(dir).snormalize()
//    val yaxis = dir.cross(xaxis).snormalize()

    //https://stackoverflow.com/questions/18151845/converting-glmlookat-matrix-to-quaternion-and-back
    val m = arrayListOf(
        arrayListOf(right.x, up.x, dir.x),
        arrayListOf(right.y, up.y, dir.y),
        arrayListOf(right.z, up.z, dir.z),
    )

    //https://github.com/g-truc/glm/blob/47585fde0c49fa77a2bf2fb1d2ead06999fd4b6e/glm/gtc/quaternion.inl#L81

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