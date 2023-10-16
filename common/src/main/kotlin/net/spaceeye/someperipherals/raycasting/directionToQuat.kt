package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Quaternion
import net.minecraft.core.Direction

//I don't remember how i got this, but it works
fun directionToQuat(dir: Direction): Quaternion {
    return when(dir) {
        Direction.DOWN ->  Quaternion(0.707107f, 0f, -0.707107f, 0f)
        Direction.UP ->    Quaternion(0.707107f, 0f, 0.707107f, 0f)
        Direction.NORTH -> Quaternion(1f, 0f, 0f, 0f)
        Direction.EAST ->  Quaternion(0.707107f, 0f, 0f, 0.707107f)
        Direction.SOUTH -> Quaternion(0f, 0f, 0f, 1f)
        Direction.WEST ->  Quaternion(-0.707107f, 0f, 0f, 0.707107f)
    }
}