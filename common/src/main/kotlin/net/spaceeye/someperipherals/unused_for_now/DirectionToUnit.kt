//package net.spaceeye.someperipherals.util
//
//import com.mojang.math.Vector3d
//import net.minecraft.core.Direction
//
//fun directionToUnit(dir: Direction): Vector3d {
//    return when (dir) {
//        Direction.DOWN ->  Vector3d( 0.0,-1.0, 0.0)
//        Direction.UP ->    Vector3d( 0.0, 1.0, 0.0)
//        Direction.NORTH -> Vector3d( 0.0, 0.0,-1.0)
//        Direction.SOUTH -> Vector3d( 0.0, 0.0, 1.0)
//        Direction.WEST ->  Vector3d(-1.0, 0.0, 0.0)
//        Direction.EAST ->  Vector3d( 1.0, 0.0, 0.0)
//    }
//}