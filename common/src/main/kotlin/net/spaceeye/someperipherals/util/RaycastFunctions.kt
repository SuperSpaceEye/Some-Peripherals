package net.spaceeye.someperipherals.util

import com.mojang.math.Quaternion
import com.mojang.math.Vector3d
import com.mojang.math.Vector3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.SomePeripheralsConfig

object RaycastFunctions {
    @JvmStatic
    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    private fun rayIsIntersecting(prev: Vector3d, cur: Vector3d, at: BlockPos, boxes: List<AABB>): Boolean {
        val eps = 1e-16
        val d = Vector3d(1.0/(cur.x - prev.x + eps), 1.0/(cur.y - prev.y + eps), 1.0/(cur.z - prev.z + eps))
        val r = Vector3d(prev.x - at.x, prev.y - at.y, prev.z - at.z)

        // if function is used then it's already "near" blockpos, so no need to sub ray origin
        for (box in boxes) {
            val t1: Double = (box.minX - r.x) * d.x
            val t2: Double = (box.maxX - r.x) * d.x
            val t3: Double = (box.minY - r.y) * d.y
            val t4: Double = (box.maxY - r.y) * d.y
            val t5: Double = (box.minZ - r.z) * d.z
            val t6: Double = (box.maxZ - r.z) * d.z

            val tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6))
            val tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6))
            if (tmax < 0 || tmin > tmax) {continue}
            return true
        }

        return false
    }

    @JvmStatic
    fun check_for_block_in_world(prev: Ref<Vector3d>,
                                 start: Vector3d,
                                 point: Vector3d,
                                 bpos: Ref<BlockPos>,
                                 res: Ref<BlockState>,
                                 level: Level): Pair<BlockPos, BlockState>? {
        if (point.x == start.x && point.y == start.y && point.z == start.z) {return null}
        bpos.it = BlockPos(point.x, point.y, point.z)
        res.it = level.getBlockState(bpos.it)

        if (res.it.isAir) {prev.it=point; return null}
        if (SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_block_model_ray_intersection
            && !rayIsIntersecting(prev.it, point, bpos.it, res.it.getShape(level, bpos.it).toAabbs())) {return null}

        return Pair(bpos.it, res.it)
    }
    @JvmStatic
    fun raycast(level: Level, pointsIter: IterateBetweenTwoPointsIter): Pair<BlockPos, BlockState> {
        val start= pointsIter.next()
        val prev = Ref(start)
        val bpos = Ref(BlockPos(start.x, start.y, start.z))
        val res = Ref(level.getBlockState(bpos.it))

        for (point in pointsIter) {
            val world_res = check_for_block_in_world(prev, start, point, bpos, res, level)

            if (world_res != null) {return world_res}
            if (SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_for_entities) {}
        }

        return Pair(bpos.it, res.it)
    }

    @JvmStatic
    fun fisheyeRotationCalc(be: BlockEntity, roll:Double, pitch: Double, yaw: Double): Vector3d {
        val direction = directionToQuat(be.blockState.getValue(BlockStateProperties.FACING))
        val rotation = Quaternion(roll.toFloat(), pitch.toFloat(), yaw.toFloat(), false)
        direction.mul(rotation)
        return quatToUnit(direction)
    }

    @JvmStatic
    fun orthogonalRotationCalc(be: BlockEntity, posX:Double, posY: Double): Vector3d {
        val dir_enum = be.blockState.getValue(BlockStateProperties.FACING)
        val dir: Vector3f = dir_enum.step()

        val up    = Vector3f(0f, 1f, 0f)
        val right = Vector3f(0f, 1f, 0f); right.cross(dir)

//        dir = dir + posX*right + posY*updir = dir.Normalize();
        if (dir_enum == Direction.NORTH || dir_enum == Direction.SOUTH) {
            right.mul(posX.toFloat(), 0f, 0f)
            up.mul(0f, posY.toFloat(), 0f)
        } else if (dir_enum == Direction.WEST || dir_enum == Direction.EAST) {
            right.mul(0f, 0f, posX.toFloat())
            up.mul(0f, posY.toFloat(), 0f)
        } else {
            right.mul(posX.toFloat(), 0f, 0f)
            up.mul(0f, 1f, posY.toFloat())
        }
        dir.add(right)
        dir.add(up)
        dir.normalize()
        return Vector3d(dir.x().toDouble(), dir.y().toDouble(), dir.z().toDouble())
    }

    @JvmStatic
    fun castRay(level: Level, be: BlockEntity, pos: BlockPos,
                distance: Double, var1:Double, var2: Double, var3:Double,
                use_fisheye: Boolean = true, check_for_entities: Boolean = true): Pair<BlockPos, BlockState> {
        val unit_d = if(use_fisheye) {fisheyeRotationCalc(be, var1, var2, var3)} else {orthogonalRotationCalc(be, var1, var2)}

        val start = Vector3d(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5)
        val stop = Vector3d(
            unit_d.x * distance + start.x,
            unit_d.y * distance + start.y,
            unit_d.z * distance + start.z
        )

        val result = raycast(level, IterateBetweenTwoPointsIter(start, stop, SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_raycast_iterations))

        return result
    }
}