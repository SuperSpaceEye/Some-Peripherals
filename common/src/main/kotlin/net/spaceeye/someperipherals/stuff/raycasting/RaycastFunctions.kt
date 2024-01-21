package net.spaceeye.someperipherals.stuff.raycasting

import com.mojang.math.Quaternion
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.spaceeye.acceleratedraycasting.api.API
import net.spaceeye.someperipherals.LOG
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.stuff.BallisticFunctions.rad
import net.spaceeye.someperipherals.stuff.utils.*
import org.valkyrienskies.mod.common.getShipManagingPos
import java.lang.Math.*

interface IBlockRes {
    val state: BlockState
}

class FullBlockRes(private val blockState: BlockState): IBlockRes {
    override val state: BlockState
        get() = blockState
}

class PartialBlockRes: IBlockRes {
    override val state: BlockState
        get() = throw AssertionError("No state.")
}

open class BaseRaycastBlockRes(
    @JvmField var bpos: BlockPos,
    @JvmField var res: IBlockRes,
    @JvmField var dist_to_in: Double
)

class WorldRaycastBlockRes(bpos: BlockPos, res: IBlockRes, t_to_in: Double): BaseRaycastBlockRes(bpos, res, t_to_in)

object RaycastFunctions {
    const val eps  = 1e-200
    const val heps = 1/ eps

    class RayIntersectBoxResult(@JvmField var intersects: Boolean, @JvmField var t_to_in: Double, @JvmField var t_to_out: Double)

    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    //first t is time to in collision point, second t is time to out collision point
    @JvmStatic
    fun rayIntersectsBox(box: AABB, ray_origin: Vector3d, d: Vector3d): RayIntersectBoxResult {
        val t1: Double = (box.minX - ray_origin.x) * d.x
        val t2: Double = (box.maxX - ray_origin.x) * d.x
        val t3: Double = (box.minY - ray_origin.y) * d.y
        val t4: Double = (box.maxY - ray_origin.y) * d.y
        val t5: Double = (box.minZ - ray_origin.z) * d.z
        val t6: Double = (box.maxZ - ray_origin.z) * d.z

        val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
        val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return RayIntersectBoxResult(false, tmax, tmin)}
        return RayIntersectBoxResult(true, tmin, tmax)
    }

    // will check all AABBs for intersections, and if intersects more than one, will return the closest AABB with which ray intersects
    @JvmStatic
    fun rayIntersectsAABBs(start: Vector3d, at: BlockPos, d: Vector3d, boxes: List<AABB>): Pair<Boolean, Double> {
        val r = start - Vector3d(at)

        val intersecting = mutableListOf<Double>()
        for (box in boxes) {
            val res = rayIntersectsBox(box, r, d)
            if (res.intersects) {intersecting.add(res.t_to_in)}
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
        manager: PosManager,
        onlyDistance: Boolean,
        constructor: (BlockPos, IBlockRes, Double) -> BaseRaycastBlockRes // this is fucking stupid i hate this
    ): BaseRaycastBlockRes? {
        val bpos = BlockPos(point.x, point.y, point.z)

        LOG(bpos.toString())

        if (SomePeripherals.has_arc) {
            if (!API.getIsSolidState(level, bpos)) { return null }
            if (onlyDistance) {
                val (test_res, t) = rayIntersectsAABBs(start, bpos, d, listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)))
                if (!test_res) {return null}
                return constructor(bpos, PartialBlockRes(), t * ray_distance)
            }
        }

        val res = manager.getBlockState(level, bpos)

        if (!SomePeripherals.has_arc && res.isAir) {return null}
        val (test_res, t) = rayIntersectsAABBs(start, bpos, d, res.getShape(level, bpos).toAabbs())
        if (!test_res) {return null}
        return constructor(bpos, FullBlockRes(res), t * ray_distance)
    }
    @JvmStatic
    fun checkForIntersectedEntity(
        start: Vector3d,
        cur: Vector3d,
        level: ServerLevel,
        d: Vector3d,
        ray_distance: Double,
        er: Int,
        ignore_entity: Entity?,
        timeout_ms: Long
    ): Pair<Entity, Double>? {
        // to prevent mc from dying when there are too many entities
        val entities = getEntitiesWithTimeout(level, AABB(
            cur.x-er, cur.y-er, cur.z-er,
            cur.x+er, cur.y+er, cur.z+er), timeout_ms
        ) { it }

        val intersecting_entities = mutableListOf<Pair<Entity, Double>>()
        for (entity in entities) {
            if (entity == ignore_entity) {continue}
            val res = rayIntersectsBox(entity.boundingBox, start, d)
            if (!res.intersects) {continue}
            intersecting_entities.add(Pair(entity, res.t_to_in * ray_distance))
        }

        if (intersecting_entities.size == 0) {return null}
        return intersecting_entities.minBy {
            pow(it.first.x - cur.x, 2.0) + pow(it.first.y - cur.y, 2.0) + pow(it.first.z - cur.z, 2.0) }
    }

    fun makeResult(
        wres: BaseRaycastBlockRes?,
        entity_res: Pair<Entity, Double>?,
        unit_d: Vector3d,
        start: Vector3d,
        cache: PosManager,
        cummulative_dist: Double
    ): RaycastReturn {
        cache.cleanup()
        return if (wres != null && entity_res != null) {
            if (wres.dist_to_in < entity_res.second) {RaycastBlockReturn(start, wres.bpos, wres.res, wres.dist_to_in + cummulative_dist, unit_d*wres.dist_to_in+start)} else {RaycastEntityReturn(start, entity_res.first, entity_res.second, unit_d*entity_res.second+start)}
        } else if (wres != null) {
            RaycastBlockReturn(start, wres.bpos, wres.res, wres.dist_to_in + cummulative_dist, unit_d*wres.dist_to_in+start)
        } else if (entity_res != null) {
            RaycastEntityReturn(start, entity_res.first, entity_res.second + cummulative_dist, unit_d * entity_res.second+start)
        } else {
            throw AssertionError("makeResult was called but both entity_res and world_res are nil")
        }
    }


    open class RaycastObj(
        var start: Vector3d, stop: Vector3d, val ignore_entity: Entity?,
        var points_iter: RayIter, val check_for_blocks_in_world: Boolean,
        val onlyDistance: Boolean
    ): RaycastObjOrError {
        var rd = stop - start
        var d = (rd+eps).srdiv(1.0)
        var ray_distance = rd.dist()
        var unit_d = rd.normalize()

        val check_for_entities = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.check_for_intersection_with_entities
        val er = if (check_for_blocks_in_world) SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius else SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entities_only_raycast_entity_check_radius

        var entity_res: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        var world_res: BaseRaycastBlockRes? = null

        var cache = PosManager()

        var entity_timeout_ms = SomePeripheralsConfig.SERVER.RAYCASTING_SETTINGS.max_entity_get_time_ms

        var cummulative_distance = 0.0

        var last_reflected_pos: BlockPos? = null

        open fun iterate(point: Vector3d, level: ServerLevel): RaycastReturn? {
            try {
            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (entity_res != null) { return makeResult(null, entity_res, unit_d, start, cache, cummulative_distance) }

                // Pair of Entity, t
                entity_res = checkForIntersectedEntity(start, point, level, d, ray_distance, er, ignore_entity, entity_timeout_ms)
                entity_step_counter = 0
            }
            entity_step_counter++

            if (check_for_blocks_in_world) {world_res = checkForBlockInWorld(start, point, d, ray_distance, level, cache, onlyDistance) { bpos, res, t -> WorldRaycastBlockRes(bpos, res, t) } }

            world_res = tryReflectRay(world_res)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null) {return makeResult(world_res, entity_res, unit_d, start, cache, cummulative_distance)}

            return null
            } catch (e: Exception) {
                when (e) {
                    is ChunkIsNotLoadedException -> return RaycastERROR("Chunk is not loaded")
                    else -> throw e
                }
            }
        }

        //is needed so that when raycasting ended but ray does intersect with some entity, it would return said entity.
        open fun postCheckForUnreturnedEntity(): RaycastReturn? {
            if (entity_res != null && entity_res!!.second <= points_iter.up_to) { return makeResult(null, entity_res, unit_d, start, cache, cummulative_distance) }
            return null
        }

        //https://www.reddit.com/r/raytracing/comments/yxaabc/ray_box_intersection_normal/
        //https://www.shadertoy.com/view/wtSyRd
        //https://asawicki.info/news_1301_reflect_and_refract_functions.html
        open fun tryReflectRay(
            world_res: BaseRaycastBlockRes?
        ): BaseRaycastBlockRes? {
            if (world_res == null) { return world_res}
            if (SomePeripherals.has_arc && onlyDistance) { return world_res }

            val state = world_res.res.state
            if (SomePeripheralsCommonBlocks.PERFECT_MIRROR.get() != state.block) { return world_res }
            if (entity_res != null && entity_res!!.second <= world_res.dist_to_in) { entity_step_counter = 0; return world_res }
            if (last_reflected_pos != null && last_reflected_pos == world_res.bpos) { return null }

            cummulative_distance += world_res.dist_to_in

            val bpos = world_res.bpos
            val boxctr = Vector3d(bpos) + 0.5

            last_reflected_pos = bpos

            val box_hit = boxctr - start + unit_d * world_res.dist_to_in
            val normal = box_hit / max(max(abs(box_hit.x), abs(box_hit.y)), abs(box_hit.z))

            normal.sabs().sclamp(0.0, 1.0).smul(1.0000001).sfloor().snormalize()

            val reflected_rd = unit_d - normal * (unit_d.dot(normal) * 2)

            start = start + unit_d * world_res.dist_to_in

            rd = reflected_rd
            d = (rd+eps).srdiv(1.0)
            ray_distance = rd.dist()
            unit_d = rd.normalize()

            entity_res = null

            val temp_cur_i = points_iter.cur_i
            points_iter = DDAIter(start, start + unit_d * (points_iter.up_to - points_iter.cur_i), points_iter.up_to)
            points_iter.cur_i = temp_cur_i

            points_iter.next()

            entity_step_counter = 0

            return null
        }
    }

    @JvmStatic
    fun timedRaycast(raycastObj: RaycastObj, level: ServerLevel, timeout_ms: Long): Pair<RaycastReturn?, RaycastObj> {
        val start = getNowFast_ms()

        //points_iter can change while iterating
        //TODO think of another way to do this maybe?
        while (raycastObj.points_iter.hasNext()) {
            val point = raycastObj.points_iter.next()

            val res = raycastObj.iterate(point, level)
            if (res != null) {return Pair(res, raycastObj)}

//            if (getNowFast_ms() - start > timeout_ms) {return Pair(null, raycastObj)}
        }
        return Pair(raycastObj.postCheckForUnreturnedEntity() ?: RaycastNoResultReturn(raycastObj.points_iter.up_to.toDouble()), raycastObj)
    }

    @JvmStatic
    fun makeRaycastObj(level: Level, points_iter: RayIter, ignore_entity:Entity?,
                       pos: Vector3d, unit_d: Vector3d, check_for_blocks_in_world: Boolean,
                       onlyDistance: Boolean
    ): RaycastObj {
        return when (SomePeripherals.has_vs) {
            false -> RaycastObj(points_iter.start, points_iter.stop, ignore_entity, points_iter, check_for_blocks_in_world, onlyDistance)
            true  -> VSRaycastFunctions.VSRaycastObj(points_iter.start, points_iter.stop, ignore_entity, points_iter, pos, unit_d, level, check_for_blocks_in_world, onlyDistance)
        }
    }

    //if the dir is down (0 rel y), north (0 rel z) or west (0 rel x), then starting position of ray will be in the
    // raycasting. To prevent that, i iterate DDA once, so that it starts at next position.
    @JvmStatic
    fun dirToStartingRayOffset(direction: Direction): Pair<Vector3d, Boolean> =
        when(direction) {
            Direction.DOWN ->  Pair(Vector3d(0.5, 0  , 0.5), true)
            Direction.UP ->    Pair(Vector3d(0.5, 1  , 0.5), false)
            Direction.NORTH -> Pair(Vector3d(0.5, 0.5, 0  ), true)
            Direction.EAST ->  Pair(Vector3d(1  , 0.5, 0.5), false)
            Direction.SOUTH -> Pair(Vector3d(0.5, 0.5, 1  ), false)
            Direction.WEST ->  Pair(Vector3d(0  , 0.5, 0.5), true)
        }

    //No idea how or why it works, don't use it anywhere else other than eulerRotationCalc
    private fun quatToUnit(rot: Quaternion): Vector3d {
        val quint = Quaternion(0f, 1f, 0f, 0f)
        val rota = Quaternion(rot.i(), -rot.j(), -rot.k(), -rot.r())
        rot.mul(quint); rot.mul(rota)
        return Vector3d(
            -rot.k().toDouble(),
             rot.r().toDouble(),
            -rot.j().toDouble()
        )
    }

    @JvmStatic
    fun eulerRotationCalc(direction: Quaternion, pitch_: Double, yaw_: Double): Vector3d {
        val pitch = (if (pitch_ < 0) { max(pitch_, -PI/2) } else { min(pitch_, PI/2) })
        val yaw   = (if (yaw_ < 0)   { max(yaw_,   -PI/2) } else { min(yaw_  , PI/2) })

        //idk why roll is yaw, and it needs to be inverted so that +yaw is right and -yaw is left
        val rotation = Quaternion(-yaw.toFloat(), pitch.toFloat(), 0f, false)
        direction.mul(rotation)
        return quatToUnit(direction)
    }

    @JvmStatic
    fun vectorRotationCalc(dir_up: Pair<Vector3d, Vector3d>, posY: Double, posX:Double, length: Double,
                           right: Vector3d? = null): Vector3d {
        val dir = dir_up.first
        val l = max(length, 1e-200)

        //thanks getitemfromblock for this

        dir.snormalize()
        val up = dir_up.second.smul(l)
        val right = right ?: -up.cross(dir)

        dir += right * posX + up * posY
        dir.snormalize()
        return dir
    }

    @JvmStatic
    fun commonMakeRaycastObj(level: Level, start: Vector3d, unit_d: Vector3d, distance: Double,
                             pos: Vector3d, ignore_entity: Entity?, check_for_blocks_in_world: Boolean,
                             onlyDistance: Boolean, iterate_once: Boolean=false): RaycastObj {
        val stop = unit_d * distance + start

        val max_dist = if (check_for_blocks_in_world) SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_raycast_distance else SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_entities_only_raycast_distance
        val max_iter = if (max_dist <= 0) { distance.toInt() } else { min(distance.toInt(), max_dist) }
        val iter = DDAIter(start, stop, max_iter + if (iterate_once) 1 else 0)
        if (iterate_once) { iter.next() }

        return makeRaycastObj(level, iter, ignore_entity, pos, unit_d, check_for_blocks_in_world, onlyDistance)
    }

    @JvmStatic
    fun entityMakeRaycastObj(entity: LivingEntity, distance: Double, euler_mode: Boolean,
                             var1:Double, var2: Double, var3: Double, check_for_blocks_in_world: Boolean,
                             onlyDistance: Boolean): RaycastObj {
        val level = entity.getLevel()
        val start = Vector3d(entity.eyePosition)

        //https://gamedev.stackexchange.com/questions/190054/how-to-calculate-the-forward-up-right-vectors-using-the-rotation-angles
        val p = rad(entity.xRot.toDouble()) // picth
        val y =-rad(entity.yHeadRot.toDouble()) // yaw
        val unit_d = if (euler_mode) {
            eulerRotationCalc(Quaternion.fromXYZ(y.toFloat(), -p.toFloat(), PI.toFloat()), var1, var2)
        } else {
            val up  = Vector3d(sin(p)*sin(y),  cos(p), sin(p)*cos(y))
            val dir = Vector3d(cos(p)*sin(y), -sin(p), cos(p)*cos(y))
            val right = Vector3d(-cos(y), 0, sin(y))

            vectorRotationCalc(Pair(dir, up), var1, var2, var3, right)
        }

        return commonMakeRaycastObj(level, start, unit_d, distance, start, entity, check_for_blocks_in_world, onlyDistance)
    }

    @JvmStatic
    fun blockMakeRaycastObj(level: Level, be: BlockEntity, pos: BlockPos,
                            distance: Double, euler_mode: Boolean,
                            var1:Double, var2: Double, var3: Double, check_for_blocks_in_world: Boolean, onlyDistance: Boolean): RaycastObjOrError {
        if (level.isClientSide) { throw AssertionError("Direction is null, how.") }

        var unit_d = if (euler_mode) {
            eulerRotationCalc(
                when(be.blockState.getValue(BlockStateProperties.FACING)) {
                    Direction.DOWN ->  Quaternion(0.707107f, 0f, -0.707107f, 0f)
                    Direction.UP ->    Quaternion(0.707107f, 0f, 0.707107f, 0f)
                    Direction.NORTH -> Quaternion(1f, 0f, 0f, 0f)
                    Direction.EAST ->  Quaternion(0.707107f, 0f, 0f, 0.707107f)
                    Direction.SOUTH -> Quaternion(0f, 0f, 0f, 1f)
                    Direction.WEST ->  Quaternion(-0.707107f, 0f, 0f, 0.707107f)
                    null -> throw AssertionError("Direction is null, how.")
                },
                var1, var2)
        } else {
            val dir_enum = be.blockState.getValue(BlockStateProperties.FACING)
            val up = when(dir_enum) {
                Direction.DOWN ->  Vector3d(0, 0,-1)
                Direction.UP ->    Vector3d(0, 0, 1)
                Direction.NORTH -> Vector3d(0, 1, 0)
                Direction.SOUTH -> Vector3d(0, 1, 0)
                Direction.WEST ->  Vector3d(0, 1, 0)
                Direction.EAST ->  Vector3d(0, 1, 0)
                null -> throw AssertionError("Direction is null, how.")
            }
            val dir = Vector3d(dir_enum.step()).snormalize()
            vectorRotationCalc(Pair(dir, dir.cross(up).scross(dir)), var1, var2, var3)
        }

        val (offset, iterate_once) = dirToStartingRayOffset(be.blockState.getValue(BlockStateProperties.FACING))

        val start = if (!SomePeripherals.has_vs) {Vector3d(pos) + offset} else {
            val ship = level.getShipManagingPos(pos)
            if (ship == null) { Vector3d(pos) + offset } else {
                unit_d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(unit_d.toJomlVector3d(), unit_d.toJomlVector3d()))
                posShipToWorld(ship, Vector3d(pos) + offset)
            }
        }
        return commonMakeRaycastObj(level, start, unit_d, distance, Vector3d(pos), null, check_for_blocks_in_world, onlyDistance, iterate_once)
    }
}