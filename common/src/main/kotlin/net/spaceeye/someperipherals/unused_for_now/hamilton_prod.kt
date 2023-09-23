//package net.spaceeye.someperipherals.util
//
//import org.joml.Quaterniond
//import org.joml.Quaterniondc
//import org.joml.Vector3d
//
////https://math.stackexchange.com/questions/40164/how-do-you-rotate-a-vector-by-a-unit-quaternion
//fun hamilton_prod(vec: Vector3d, quat: Quaterniondc): Vector3d {
//    val qvec = Quaterniond(vec.x, vec.y, vec.z, 0.0)
//    val inv_quat = quat.invert(Quaterniond())
//    quat.mul(qvec, qvec)
//    qvec.mul(inv_quat, qvec)
//
//    return Vector3d(qvec.x, qvec.y, qvec.z)
//}