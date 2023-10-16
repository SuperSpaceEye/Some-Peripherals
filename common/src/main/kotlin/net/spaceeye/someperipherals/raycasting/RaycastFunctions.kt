package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Quaternion
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
import net.spaceeye.someperipherals.blocks.RaycasterBlock
import net.spaceeye.someperipherals.raycasting.VSRaycastFunctions.vsRaycast
import net.spaceeye.someperipherals.util.Vector3d
import org.valkyrienskies.mod.common.getShipManagingPos
import java.lang.Math.*

object RaycastFunctions {
    private val logger = SomePeripherals.slogger
    const val eps  = 1e-200
    const val heps = 1/eps

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
    @JvmStatic
    fun rayIntersectsAABBs(start: Vector3d, at: BlockPos, d: Vector3d, boxes: List<AABB>): Pair<Boolean, Double> {
        val r = start - Vector3d(at)

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
        level: Level,
        cache: PosCache): Pair<Pair<BlockPos, BlockState>, Double>? {
        val bpos = BlockPos(point.x, point.y, point.z)
        val res = cache.getBlockState(level, bpos)

        if (res.isAir) {return null}
        val (test_res, t) = rayIntersectsAABBs(start, bpos, d, res.getShape(level, bpos).toAabbs())
        if (!test_res) {return null}
        return Pair(Pair(bpos, res), t * ray_distance)
    }
    @JvmStatic
    fun checkForIntersectedEntity(start: Vector3d,
                                  cur: Vector3d,
                                  level: Level,
                                  d: Vector3d,
                                  ray_distance: Double,
                                  er: Int,
                                  ignore_entity: Entity?): Pair<Entity, Double>? {
        //null for any entity
        val entities = level.getEntities(null, AABB(
            cur.x-er, cur.y-er, cur.z-er,
            cur.x+er, cur.y+er, cur.z+er))

        val intersecting_entities = mutableListOf<Pair<Entity, Double>>()
        for (entity in entities) {
            if (entity == null || entity == ignore_entity) {continue}
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
    fun normalRaycast(level: Level, pointsIter: RayIter, ignore_entity: Entity?, cache: PosCache): RaycastReturn {
        val start = pointsIter.start
        val stop  = pointsIter.stop

        val rd = stop - start
        val d = (rd+eps).srdiv(1.0)
        val ray_distance = rd.dist()
        val unit_d = rd.normalize()

        val check_for_entities = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.check_for_intersection_with_entities
        val er = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius

        var intersected_entity: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        for (point in pointsIter) {
            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (intersected_entity != null) { return RaycastEntityReturn(intersected_entity.first, intersected_entity.second, unit_d * intersected_entity.second+start) }

                // Pair of Entity, t
                intersected_entity = checkForIntersectedEntity(start, point, level, d, ray_distance, er, ignore_entity)
                entity_step_counter = 0
            }
            entity_step_counter++

            val world_res = checkForBlockInWorld(start, point, d, ray_distance, level, cache)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null && intersected_entity != null) { return if (world_res.second < intersected_entity.second)
            {RaycastBlockReturn(world_res.first, world_res.second, unit_d*world_res.second+start)}
            else {RaycastEntityReturn(intersected_entity.first, intersected_entity.second, unit_d*intersected_entity.second+start)} }

            if (world_res != null) {return RaycastBlockReturn(world_res.first, world_res.second, unit_d*world_res.second+start)}
        }

        return RaycastNoResultReturn(pointsIter.up_to.toDouble())
    }

    fun raycast(level: Level, pointsIter: RayIter, ignore_entity:Entity?=null, cache: PosCache, pos: Vector3d, unit_d:Vector3d): RaycastReturn {
        return when (SomePeripherals.has_vs) {
            false -> normalRaycast(level, pointsIter, ignore_entity, cache)
            true  -> vsRaycast(level, pointsIter, ignore_entity, cache, pos, unit_d)
        }
    }


    @JvmStatic
    fun dirToStartingOffset(direction: Direction): Vector3d {
        val e = -1e-4 //if its just zero, then the blockpos will floor into raycaster, so small (but too small) negative offset
        return when(direction) { //Looks better in IDEA
            Direction.DOWN ->  Vector3d(0.5, e     , 0.5)
            Direction.UP ->    Vector3d(0.5, 1  , 0.5)
            Direction.NORTH -> Vector3d(0.5, 0.5, e     )
            Direction.EAST ->  Vector3d(1  , 0.5, 0.5)
            Direction.SOUTH -> Vector3d(0.5, 0.5, 1  )
            Direction.WEST ->  Vector3d(e     , 0.5, 0.5)
        }
    }

    @JvmStatic
    fun eulerRotationCalc(direction: Quaternion, pitch_: Double, yaw_: Double): Vector3d {
        val pitch = (if (pitch_ < 0) { pitch_.coerceAtLeast(-SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_pitch_angle) } else { pitch_.coerceAtMost(SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_pitch_angle) })
        val yaw   = (if (yaw_ < 0)   { yaw_  .coerceAtLeast(-SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_yaw_angle  ) } else { yaw_  .coerceAtMost(SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_yaw_angle  ) })

        //idk why roll is yaw, and it needs to be inverted so that +yaw is right and -yaw is left
        val rotation = Quaternion(-yaw.toFloat(), pitch.toFloat(), 0f, false)
        direction.mul(rotation)
        return quatToUnit(direction)
    }

    @JvmStatic
    fun vectorRotationCalc(dir_up: Pair<Vector3d, Vector3d>, posY: Double, posX:Double, length: Double): Vector3d {
        val dir = dir_up.first
        val l = max(length, 0.01)

        //thanks getitemfromblock for this
//      dir = dir + posX*right + posY*updir = dir.Normalize();

        val right: Vector3d; val up: Vector3d

        dir.snormalize()
        up = dir_up.second.smul(l)
        right = up.cross(dir)

        dir += right * (-posX) + up * posY
        dir.snormalize()
        return dir
    }

    @JvmStatic
    fun commonCastRay(level: Level, start: Vector3d, unit_d: Vector3d, distance: Double, cache: PosCache, pos: Vector3d, ignore_entity: Entity?): RaycastReturn {
        val stop = unit_d * distance + start

        val max_dist = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_raycast_distance
        val max_iter = if (max_dist <= 0) { distance.toInt() } else { min(distance.toInt(), max_dist) }
        val iter = BresenhamIter(start, stop, max_iter)

        val result = raycast(level, iter, ignore_entity, cache, pos, unit_d)

        return result
    }

    fun castRayEntity(entity: Entity, distance: Double, euler_mode: Boolean = true, do_cache:Boolean = false,
                      var1:Double, var2: Double, var3: Double): RaycastReturn {
        val level = entity.getLevel()

        val dir = Vector3d(entity.lookAngle)

        //https://gamedev.stackexchange.com/questions/190054/how-to-calculate-the-forward-up-right-vectors-using-the-rotation-angles
        val p = entity.rotationVector.x.toDouble()
        val y = entity.rotationVector.y.toDouble()
        val up = Vector3d(sin(p) * sin(y), cos(p), sin(p) * cos(y))

        val unit_d = if (euler_mode || !SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.vector_rotation_enabled) {
            eulerRotationCalc(vectorsToQuat(dir, up), var1, var2)
        } else {
            vectorRotationCalc(Pair(dir, up), var1, var2, var3)
        }

        val start = Vector3d(entity.eyePosition)
        val cache = PosCache()
        cache.do_cache = do_cache

        return commonCastRay(level, start, unit_d, distance, cache, start, entity)
    }

    @JvmStatic
    fun castRayBlock(level: Level, be: BlockEntity, pos: BlockPos,
                     distance: Double, euler_mode: Boolean = true, do_cache:Boolean = false,
                     var1:Double, var2: Double, var3: Double): RaycastReturn {
        if (level.isClientSide) { return RaycastERROR("Level is clientside. how.") }

        val cache = (be.blockState.block as RaycasterBlock).pos_cache
        cache.do_cache = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.do_position_caching && do_cache
        cache.max_items = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_cached_positions

        var unit_d = if (euler_mode || !SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.vector_rotation_enabled) {
            eulerRotationCalc(directionToQuat(be.blockState.getValue(BlockStateProperties.FACING)), var1, var2)
        } else {
            val dir_en = be.blockState.getValue(BlockStateProperties.FACING)
            val up = when(dir_en) {
                Direction.DOWN ->  Vector3d(0, 0, 1)
                Direction.UP ->    Vector3d(0, 0, 1)
                Direction.NORTH -> Vector3d(0, 1, 0)
                Direction.SOUTH -> Vector3d(0, 1, 0)
                Direction.WEST ->  Vector3d(0, 1, 0)
                Direction.EAST ->  Vector3d(0, 1, 0)
                null -> return RaycastERROR("Direction is null, how.")
            }
            val dir = Vector3d(dir_en.step()).snormalize()
            vectorRotationCalc(Pair(dir, dir.cross(up).scross(dir)), var1, var2, var3)
        }

        val start = if (SomePeripherals.has_vs) {
            val ship = level.getShipManagingPos(pos)
            if (ship != null) {
                val scale = Vector3d(ship.transform.shipToWorldScaling)
                val ship_wp = Vector3d(ship.transform.positionInWorld)
                val ship_sp = Vector3d(ship.transform.positionInShip)
                unit_d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(unit_d.toJomlVector3d(), unit_d.toJomlVector3d()))
                Vector3d((ship.transform.transformDirectionNoScalingFromShipToWorld(
                    ((Vector3d(pos) - ship_sp + dirToStartingOffset(be.blockState.getValue(BlockStateProperties.FACING)))*scale).toJomlVector3d(), org.joml.Vector3d()))
                ) + ship_wp
            } else {
                Vector3d(pos) + dirToStartingOffset(be.blockState.getValue(BlockStateProperties.FACING))
            }
        } else {
            Vector3d(pos) + dirToStartingOffset(be.blockState.getValue(BlockStateProperties.FACING))
        }
        return commonCastRay(level, start, unit_d, distance, cache, Vector3d(pos), null)
    }
}