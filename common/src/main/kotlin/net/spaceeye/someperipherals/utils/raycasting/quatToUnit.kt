package net.spaceeye.someperipherals.utils.raycasting

import com.mojang.math.Quaternion
import net.spaceeye.someperipherals.utils.mix.Vector3d

fun quatToUnit(rot: Quaternion): Vector3d {
    val quint = Quaternion(0f, 1f, 0f, 0f)
    val rota = Quaternion(rot.i(), -rot.j(), -rot.k(), -rot.r())
    rot.mul(quint); rot.mul(rota)
    return Vector3d(
        -rot.k().toDouble(),
         rot.r().toDouble(),
        -rot.j().toDouble()
        )
}