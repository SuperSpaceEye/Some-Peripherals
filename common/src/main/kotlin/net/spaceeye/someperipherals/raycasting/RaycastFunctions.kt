package net.spaceeye.someperipherals.raycasting

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
import net.spaceeye.someperipherals.util.directionToQuat
import net.spaceeye.someperipherals.util.quatToUnit
import net.spaceeye.someperipherals.raycasting.VSRaycastFunctions.vsRaycast
import net.spaceeye.someperipherals.util.BresenhamIter
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import java.lang.Math.*

typealias ray_iter_type = RayIter

object RaycastFunctions {
    private val logger = SomePeripherals.slogger
    val eps  = 1e-30
    val heps = 1/eps

    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    @JvmStatic
    fun rayIntersectsBox(box: AABB, or: Vector3d, d: Vector3d): Pair<Boolean, Double> {
        val t1: Double = (box.minX - or.x) * d.x
        val t2: Double = (box.maxX - or.x) * d.x
        val t3: Double = (box.minY - or.y) * d.y
        val t4: Double = (box.maxY - or.y) * d.y
        val t5: Double = (box.minZ - or.z) * d.z
        val t6: Double = (box.maxZ - or.z) * d.z

        val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
        val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return Pair(false, tmax)}
        if (tmin <= 0) {return Pair(true, 0.0)} // already inside an object so no t
        return Pair(true, tmin)
    }
    //Will check block hitbox
    @JvmStatic
    fun rayIntersectsBlock(start: Vector3d, at: BlockPos, d: Vector3d, boxes: List<AABB>): Pair<Boolean, Double> {
        val r = Vector3d(-at.x + start.x, -at.y + start.y, -at.z + start.z)

        val intersecting = mutableListOf<Double>()
        for (box in boxes) {
            val (res, t) = rayIntersectsBox(box, r, d)
            if (res) {intersecting.add(t)}
        }
        if (intersecting.isEmpty()) {return Pair(false, heps)}
        return Pair(true, intersecting.min())
    }

    @JvmStatic
    fun checkForBlockInWorld(
        start: Vector3d,
        point: Vector3d,
        d: Vector3d,
        ray_distance: Double,
        level: Level): Pair<Pair<BlockPos, BlockState>, Double>? {
        //TODO do i need to do this? idk
        if (point.x == start.x && point.y == start.y && point.z == start.z) {return null}
        val bpos = BlockPos(point.x, point.y, point.z)
        val res = level.getBlockState(bpos)

        if (res.isAir) {return null}
        val (test_res, t) = rayIntersectsBlock(start, bpos, d, res.getShape(level, bpos).toAabbs())
        if (!test_res) {return null}
        return Pair(Pair(bpos, res), t * ray_distance)
    }
    @JvmStatic
    fun checkForIntersectedEntity(start: Vector3d,
                                  cur: Vector3d,
                                  level: Level,
                                  d: Vector3d,
                                  ray_distance: Double,
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
            intersecting_entities.add(Pair(entity, t * ray_distance))
        }

        if (intersecting_entities.size == 0) {return null}
        return intersecting_entities.minBy {
            pow(it.first.x - cur.x, 2.0) + pow(it.first.y - cur.y, 2.0)+ pow(it.first.z - cur.z, 2.0) }
    }

    // returns either Pair<BlockPos, BlockState> or Entity
    @JvmStatic
    fun normalRaycast(level: Level, pointsIter: ray_iter_type): RaycastReturn {
        val start = pointsIter.start
        val stop  = pointsIter.stop

        val rd = Vector3d(stop.x - start.x, stop.y - start.y, stop.z - start.z)
        val d = Vector3d(1.0/(rd.x + eps), 1.0/(rd.y + eps), 1.0/(rd.z + eps))
        val ray_distance = kotlin.math.sqrt(rd.x * rd.x + rd.y * rd.y + rd.z * rd.z)

        val check_for_entities = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_for_entities
        val er = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.entity_check_radius

        var intersected_entity: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        for (point in pointsIter) {
            // Pair of (Pair of bpos, blockState), t
            val world_res = checkForBlockInWorld(start, point, d, ray_distance, level)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null && intersected_entity != null) { return if (world_res.second < intersected_entity.second)
            {RaycastBlockReturn(world_res.first, world_res.second)} else {RaycastEntityReturn(intersected_entity.first, intersected_entity.second)} }

            if (world_res != null) {return RaycastBlockReturn(world_res.first, world_res.second)}

            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (intersected_entity != null) { return RaycastEntityReturn(intersected_entity.first, intersected_entity.second) }

                // Pair of Entity, t
                intersected_entity = checkForIntersectedEntity(start, point, level, d, ray_distance, er)
                entity_step_counter = 0
            }
            entity_step_counter++
        }

        return RaycastNoResultReturn(pointsIter.up_to.toDouble())
    }

    fun raycast(level: Level, pointsIter: ray_iter_type): RaycastReturn {
        return when (SomePeripherals.has_vs) {
            false -> normalRaycast(level, pointsIter)
            true  -> vsRaycast(level, pointsIter)
        }
    }

    fun getStartingPosition(level: Level, pos: BlockPos): BlockPos {
        return when (SomePeripherals.has_vs) {
            false -> pos
            true -> {
                val test = level.getShipManagingPos(pos) ?: return pos
                val new_pos = test.toWorldCoordinates(pos)
                return BlockPos(new_pos.x, new_pos.y, new_pos.z)
            }
        }
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
    fun vectorRotationCalc(be: BlockEntity, posY: Double, posX:Double): Vector3d {
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

    @JvmStatic
    fun castRay(level: Level, be: BlockEntity, pos: BlockPos,
                distance: Double, var1:Double, var2: Double,
                use_fisheye: Boolean = true): RaycastReturn {
        //TODO make separation between normal and VS compat versions at castRay instead of raycast, as i will need
        // a lot of logic for ship -> world translation
        if (level.isClientSide) {return RaycastERROR("Level is clientside. how.")}

        val unit_d = if(use_fisheye || !SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.vector_rotation_enabled)
        { fisheyeRotationCalc(be, var1, var2) } else { vectorRotationCalc(be, var1, var2) }

        val spos = getStartingPosition(level, pos)
        val start = Vector3d(spos.x.toDouble() + 0.5, spos.y.toDouble() + 0.5, spos.z.toDouble() + 0.5)
        val stop = Vector3d(
            unit_d.x * distance + start.x,
            unit_d.y * distance + start.y,
            unit_d.z * distance + start.z
        )

        val max_dist = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.max_raycast_iterations
        val max_iter = if (max_dist <= 0) {distance.toInt()} else {min(distance.toInt(), max_dist)}
        val iter = BresenhamIter(start, stop, max_iter)

        val result = raycast(level, iter)

        return result
    }
}