package net.spaceeye.someperipherals.util

import com.mojang.math.Quaternion
import com.mojang.math.Vector3d
import com.mojang.math.Vector3f
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import java.lang.Math.*

object RaycastFunctions {
    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    private fun rayIntersectsBox(box: AABB, r: Vector3d, d: Vector3d): Pair<Boolean, Double> {
        val t1: Double = (box.minX - r.x) * d.x
        val t2: Double = (box.maxX - r.x) * d.x
        val t3: Double = (box.minY - r.y) * d.y
        val t4: Double = (box.maxY - r.y) * d.y
        val t5: Double = (box.minZ - r.z) * d.z
        val t6: Double = (box.maxZ - r.z) * d.z

        val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
        val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return Pair(false, tmax)}
        return Pair(true, tmin)
    }
    //Will check block's model
    @JvmStatic
    private fun rayIntersectsBlock(start: Vector3d, at: BlockPos, d: Vector3d, boxes: List<AABB>): Pair<Boolean, Double> {
        val r = Vector3d(-at.x + start.x, -at.y + start.y, -at.z + start.z)

        //TODO
        for (box in boxes) {
            val (res, t) = rayIntersectsBox(box, r, d)
            if (res) {return Pair(true, t)}
        }

        return Pair(false, 1.0)
    }

    @JvmStatic
    private fun checkForBlockInWorld(
                             start: Vector3d,
                             point: Vector3d,
                             bpos: Ref<BlockPos>,
                             res: Ref<BlockState>,
                             d: Vector3d,
                             level: Level): Pair<Pair<BlockPos, BlockState>, Double>? {
        if (point.x == start.x && point.y == start.y && point.z == start.z) {return null}
        bpos.it = BlockPos(point.x, point.y, point.z)
        res.it = level.getBlockState(bpos.it)
        var t = 1.0

        if (res.it.isAir) {return null}
        if (SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_block_model_ray_intersection) {
            val (test_res, t_) = rayIntersectsBlock(start, bpos.it, d, res.it.getShape(level, bpos.it).toAabbs())
            if (!test_res) {return null}
            t = t_
        }
        return Pair(Pair(bpos.it, res.it), t)
    }
    @JvmStatic
    private fun checkForIntersectedEntity(start: Vector3d,
                                          cur: Vector3d,
                                          level: Level,
                                          d: Vector3d,
                                          er: Int): Pair<Entity, Double>? {
        //null for any entity
        val entities = level.getEntities(null, AABB(
            cur.x-er, cur.y-er, cur.z-er,
            cur.x+er, cur.y+er, cur.z+er))

        val intersecting_entities = mutableListOf<Pair<Entity, Double>>()
        for (entity in entities) {
            if (entity == null) {continue}
            val (res, t) = rayIntersectsBox(entity.boundingBox, start, d)
            if (!res) {continue}
            intersecting_entities.add(Pair(entity, t))
        }

        if (intersecting_entities.size == 0) {return null}
        return intersecting_entities.minBy {
            pow(it.first.x - cur.x, 2.0) + pow(it.first.y - cur.y, 2.0)+ pow(it.first.z - cur.z, 2.0) }
    }

    // returns either Pair<BlockPos, BlockState> or Entity
    @JvmStatic
    fun raycast(level: Level, pointsIter: IterateBetweenTwoPointsIter): Any {
        val start = pointsIter.start // starting position

        val bpos = Ref(BlockPos(start.x, start.y, start.z))
        val res = Ref(level.getBlockState(bpos.it))

        val eps = 1e-16
        val next = pointsIter.nextNoStep()
        // unit vector of ray direction
        val d = Vector3d(1.0/(next.x - start.x + eps), 1.0/(next.y - start.y + eps), 1.0/(next.z - start.z + eps))

        val check_for_entities = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_for_entities
        val er = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.entity_check_radius

        var intersected_entity: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        for (point in pointsIter) {
            // Pair of (Pair of bpos, blockState), t
            val world_res = checkForBlockInWorld(start, point, bpos, res, d, level)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null && intersected_entity != null) { return if (world_res.second < intersected_entity.second) {world_res.first} else {intersected_entity.first} }
            if (world_res != null) {return world_res.first}

            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % (er+1) == 0) {
                if (intersected_entity != null) { return intersected_entity.first }

                // Pair of Entity, t
                intersected_entity = checkForIntersectedEntity(start, point, level, d, er)
                entity_step_counter = 0
            }
            entity_step_counter++
        }

        return Pair(bpos.it, res.it)
    }

    @JvmStatic
    fun fisheyeRotationCalc(be: BlockEntity, pitch_: Double, yaw_: Double): Vector3d {
        val pitch = (if (pitch_ < 0) { pitch_.coerceAtLeast(-SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_pitch_angle) } else { pitch_.coerceAtMost(SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_pitch_angle) })
        val yaw   = (if (yaw_ < 0)   { yaw_  .coerceAtLeast(-SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_yaw_angle  ) } else { yaw_  .coerceAtMost(SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_yaw_angle  ) })

        val direction = directionToQuat(be.blockState.getValue(BlockStateProperties.FACING))
        //idk why roll is yaw, and it needs to be inverted so that +yaw is right and -yaw is left
        val rotation = Quaternion(-yaw.toFloat(), pitch.toFloat(), 0f, false)
        direction.mul(rotation)
        return quatToUnit(direction)
    }

    @JvmStatic
    fun vectorRotationCalc(be: BlockEntity, posY: Double, posX:Double,): Vector3d {
        val dir_enum = be.blockState.getValue(BlockStateProperties.FACING)
        val dir: Vector3f = dir_enum.step()

        //thanks getitemfromblock for this
//      dir = dir + posX*right + posY*updir = dir.Normalize();

        val right: Vector3f; val up: Vector3f
        if (dir_enum != Direction.UP && dir_enum != Direction.DOWN) {
            up = Vector3f(0f, 1f, 0f)
            right = Vector3f(0f, 1f, 0f); right.cross(dir)

            if (dir_enum == Direction.NORTH || dir_enum == Direction.SOUTH) {
                right.mul(posX.toFloat(), 0f, 0f)
                up.mul(0f, posY.toFloat(), 0f)
            } else if (dir_enum == Direction.WEST || dir_enum == Direction.EAST) {
                right.mul(0f, 0f, posX.toFloat())
                up.mul(0f, posY.toFloat(), 0f)
            }
        } else {
            up = Vector3f(0f, 0f, 1f)
            right = Vector3f(0f, 0f, 1f); right.cross(dir)

            right.mul(posX.toFloat(), 0f, 0f)
            up.mul(0f, 0f, posY.toFloat())
        }
        dir.add(right)
        dir.add(up)
        dir.normalize()
        return Vector3d(dir.x().toDouble(), dir.y().toDouble(), dir.z().toDouble())
    }

    // returns either Pair<BlockPos, BlockState> or Entity
    @JvmStatic
    fun castRay(level: Level, be: BlockEntity, pos: BlockPos,
                distance: Double, var1:Double, var2: Double,
                use_fisheye: Boolean = true): Any {
        if (level.isClientSide) {return "Level is clientside. how."}

        val unit_d = if(use_fisheye || !SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.vector_rotation_enabled)
        {fisheyeRotationCalc(be, var1, var2)} else {vectorRotationCalc(be, var1, var2)}

        //TODO why
//        val start = Vector3d(
//            pos.x.toDouble() + if (pos.x >= 0) {0.5} else {-0.5},
//            pos.y.toDouble() + if (pos.y >= 0) {0.5} else {-0.5},
//            pos.z.toDouble() + if (pos.z >= 0) {0.5} else {-0.5})
        val start = Vector3d(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5)
        val stop = Vector3d(
            unit_d.x * distance + start.x,
            unit_d.y * distance + start.y,
            unit_d.z * distance + start.z
        )

//        val iter = DDAIter(start, stop,
//            if (SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_raycast_iterations > 0)
//            {distance.coerceAtMost(SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_raycast_iterations.toDouble())}
//            else {distance})

        val iter = IterateBetweenTwoPointsIter(start, stop, SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_raycast_iterations)
        val result = raycast(level, iter)

        return result
    }
}