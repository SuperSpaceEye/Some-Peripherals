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
import java.lang.Math.pow

object RaycastFunctions {
    private fun rayIntersects(box: AABB, r: Vector3d, d: Vector3d): Pair<Boolean, Double> {
        val t1: Double = (box.minX - r.x) * d.x
        val t2: Double = (box.maxX - r.x) * d.x
        val t3: Double = (box.minY - r.y) * d.y
        val t4: Double = (box.maxY - r.y) * d.y
        val t5: Double = (box.minZ - r.z) * d.z
        val t6: Double = (box.maxZ - r.z) * d.z

        val tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6))
        val tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return Pair(false, tmax)}
        return Pair(true, tmin)
    }
    @JvmStatic
    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    private fun rayIsIntersecting(prev: Vector3d, cur: Vector3d, at: BlockPos, boxes: List<AABB>): Boolean {
        val eps = 1e-16
        val d = Vector3d(1.0/(cur.x - prev.x + eps), 1.0/(cur.y - prev.y + eps), 1.0/(cur.z - prev.z + eps))
        val r = Vector3d(prev.x - at.x, prev.y - at.y, prev.z - at.z)

        for (box in boxes) {
            if (rayIntersects(box, r, d).first) {return true}
        }

        return false
    }

    @JvmStatic
    fun checkForBlockInWorld(prev: Ref<Vector3d>,
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
    fun checkForIntersectedEntity(prev: Vector3d,
                                  cur: Vector3d,
                                  level: Level,
                                  er: Int): Entity? {
        val eps = 1e-16
        val d = Vector3d(1.0/(cur.x - prev.x + eps), 1.0/(cur.y - prev.y + eps), 1.0/(cur.z - prev.z + eps))

        val entities = level.getEntities(null, AABB(
            cur.x-er, cur.y-er, cur.z-er,
            cur.x+er, cur.y+er, cur.z+er))

        val intersecting_entities = mutableListOf<Entity>()
        for (entity in entities) {
            if (entity == null) {continue}
            val r = Vector3d(prev.x, prev.y, prev.z) //bounding box in in global coordinates
            if (!rayIntersects(entity.boundingBox, r, d).first) {continue}
            intersecting_entities.add(entity)
        }

        if (intersecting_entities.size == 0) {return null}
        intersecting_entities.maxBy { pow(it.x - cur.x, 2.0) + pow(it.y - cur.y, 2.0) + pow(it.z - cur.z, 2.0) }
        return intersecting_entities[0]
    }

    // returns either Pair<BlockPos, BlockState> or Entity
    @JvmStatic
    fun raycast(level: Level, pointsIter: IterateBetweenTwoPointsIter): Any {
        val start= pointsIter.next() // starting position
        val prev = Ref(start) // previous position
        val eprev = Ref(start) // previous position when entity check was made
        val bpos = Ref(BlockPos(start.x, start.y, start.z))
        val res = Ref(level.getBlockState(bpos.it))

        val check_for_entities = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_for_entities
        val er = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.entity_check_radius

        var intersected_entity: Entity? = null
        var entity_step_counter = 0
        for (point in pointsIter) {
            val world_res = checkForBlockInWorld(prev, start, point, bpos, res, level)


            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null && intersected_entity != null) {
                SomePeripherals.logger.warn("THERE IS BOTH BLOCK AND ENTITY HOLY FUCJK")
                val eps = 1e-16
                val d = Vector3d(1.0/(point.x - eprev.it.x + eps), 1.0/(point.y - eprev.it.y + eps), 1.0/(point.z - eprev.it.z + eps))
                val rb = Vector3d(eprev.it.x - bpos.it.x, eprev.it.y - bpos.it.y, eprev.it.z - bpos.it.z)
                val re = Vector3d(eprev.it.x - intersected_entity.x, eprev.it.y - intersected_entity.y, eprev.it.z - intersected_entity.z)

                val (_, te) = rayIntersects(intersected_entity.boundingBox, re, d)
                var tb: Double = 0.0
                for (box in res.it.getShape(level, bpos.it).toAabbs()) {
                    val (intersects, tb_) = rayIntersects(box, rb, d)
                    if (intersects) {tb = tb_; break}
                }

                return if (tb < te) {world_res} else {intersected_entity}
            }
            if (world_res != null) {return world_res}

            //if ray hits entity and any block wasnt hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % (er+1) == 0) {
                SomePeripherals.logger.warn("CHECKING FOR INTERSECTING ENTITITES")
                if (intersected_entity != null) {
                    return intersected_entity
                }

                intersected_entity = checkForIntersectedEntity(prev.it, point, level, er)
                entity_step_counter = 0
                eprev.it = prev.it

                if (intersected_entity != null) {
                    SomePeripherals.logger.warn("DETECTING INTERSECTING ENTITY HOLY SHIT OMG!!!! ${intersected_entity}")
                }
            }
            entity_step_counter++
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
                distance: Double, var1:Double, var2: Double, var3:Double,
                use_fisheye: Boolean = true): Any {
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