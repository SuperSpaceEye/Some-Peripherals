package net.spaceeye.someperipherals.utils.raycasting

import com.mojang.math.Quaternion
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.AABB
import net.spaceeye.acceleratedraycasting.api.API
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.utils.mix.BallisticFunctions.rad
import net.spaceeye.someperipherals.utils.mix.ChunkIsNotLoadedException
import net.spaceeye.someperipherals.utils.mix.Vector3d
import net.spaceeye.someperipherals.utils.mix.getNowFast_ms
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

object RaycastFunctions {
    const val eps  = 1e-200
    const val heps = 1/ eps

    //https://gamedev.stackexchange.com/questions/18436/most-efficient-aabb-vs-ray-collision-algorithms
    //first t is time to in collision point, second t is time to out collision point
    @JvmStatic
    fun rayIntersectsBox(box: AABB, or: Vector3d, d: Vector3d): Pair<Boolean, Pair<Double, Double>> {
        val t1: Double = (box.minX - or.x) * d.x
        val t2: Double = (box.maxX - or.x) * d.x
        val t3: Double = (box.minY - or.y) * d.y
        val t4: Double = (box.maxY - or.y) * d.y
        val t5: Double = (box.minZ - or.z) * d.z
        val t6: Double = (box.maxZ - or.z) * d.z

        val tmin = max(max(min(t1, t2), min(t3, t4)), min(t5, t6))
        val tmax = min(min(max(t1, t2), max(t3, t4)), max(t5, t6))
        if (tmax < 0 || tmin > tmax) {return Pair(false, Pair(tmax, tmin))}
        return Pair(true, Pair(tmin, tmax))
    }

    // will check all AABBs for intersections, and if intersects more than one, will return the closest AABB with which ray intersects
    @JvmStatic
    fun rayIntersectsAABBs(start: Vector3d, at: BlockPos, d: Vector3d, boxes: List<AABB>): Pair<Boolean, Double> {
        val r = start - Vector3d(at)

        val intersecting = mutableListOf<Double>()
        for (box in boxes) {
            val (res, t) = rayIntersectsBox(box, r, d)
            if (res) {intersecting.add(t.first)}
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
        onlyDistance: Boolean
    ): Pair<Pair<BlockPos, IBlockRes>, Double>? {
        val bpos = BlockPos(point.x, point.y, point.z)

        if (SomePeripherals.has_arc) {
            if (!API.getIsSolidState(level, bpos)) { return null }
            if (onlyDistance) {
                val (test_res, t) = rayIntersectsAABBs(start, bpos, d, listOf(AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)))
                if (!test_res) {return null}
                return Pair(Pair(bpos, PartialBlockRes()), t * ray_distance)
            }
        }

        val res = manager.getBlockState(level, bpos)

        if (!SomePeripherals.has_arc && res.isAir) {return null}
        val (test_res, t) = rayIntersectsAABBs(start, bpos, d, res.getShape(level, bpos).toAabbs())
        if (!test_res) {return null}
        return Pair(Pair(bpos, FullBlockRes(res)), t * ray_distance)
    }
    @JvmStatic
    fun checkForIntersectedEntity(
        start: Vector3d,
        cur: Vector3d,
        level: Level,
        d: Vector3d,
        ray_distance: Double,
        er: Int,
        ignore_entity: Entity?
    ): Pair<Entity, Double>? {
        //null is for any entity
        //TODO timeout code?
        val entities = level.getEntities(null, AABB(
            cur.x-er, cur.y-er, cur.z-er,
            cur.x+er, cur.y+er, cur.z+er))

        val intersecting_entities = mutableListOf<Pair<Entity, Double>>()
        for (entity in entities) {
            if (entity == null || entity == ignore_entity) {continue}
            val (res, t) = rayIntersectsBox(entity.boundingBox, start, d)
            if (!res) {continue}
            intersecting_entities.add(Pair(entity, t.first * ray_distance))
        }

        if (intersecting_entities.size == 0) {return null}
        return intersecting_entities.minBy {
            pow(it.first.x - cur.x, 2.0) + pow(it.first.y - cur.y, 2.0) + pow(it.first.z - cur.z, 2.0) }
    }

    fun makeResult(
        world_res: Pair<Pair<BlockPos, IBlockRes>, Double>?,
        entity_res: Pair<Entity, Double>?,
        unit_d: Vector3d,
        start: Vector3d,
        cache: PosManager
    ): RaycastReturn {
        cache.cleanup()
        return if (world_res != null && entity_res != null) {
            if (world_res.second < entity_res.second) {RaycastBlockReturn(start, world_res.first, world_res.second, unit_d*world_res.second+start)} else {RaycastEntityReturn(start, entity_res.first, entity_res.second, unit_d*entity_res.second+start)}
        } else if (world_res != null) {
            RaycastBlockReturn(start, world_res.first, world_res.second, unit_d*world_res.second+start)
        } else if (entity_res != null) {
            RaycastEntityReturn(start, entity_res.first, entity_res.second, unit_d * entity_res.second+start)
        } else {
            throw AssertionError("makeResult was called but both entity_res and world_res are nil")
        }
    }


    open class RaycastObj(
        val start: Vector3d, stop: Vector3d, val ignore_entity: Entity?,
        val points_iter: RayIter, val check_for_blocks_in_world: Boolean,
        val onlyDistance: Boolean
    ): RaycastObjOrError {
        val rd = stop - start
        val d = (rd+eps).srdiv(1.0)
        val ray_distance = rd.dist()
        val unit_d = rd.normalize()

        val check_for_entities = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.check_for_intersection_with_entities
        val er = if (check_for_blocks_in_world) SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius else SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius_no_worldchecking

        var entity_res: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        var world_res: Pair<Pair<BlockPos, IBlockRes>, Double>? = null

        var cache = PosManager()

        open fun iterate(point: Vector3d, level: Level): RaycastReturn? {
            try {
            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (entity_res != null) { return makeResult(null, entity_res, unit_d, start, cache) }

                // Pair of Entity, t
                entity_res = checkForIntersectedEntity(start, point, level, d, ray_distance, er, ignore_entity)
                entity_step_counter = 0
            }
            entity_step_counter++

            if (check_for_blocks_in_world) {world_res = checkForBlockInWorld(start, point, d, ray_distance, level, cache, onlyDistance)}

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if (world_res != null) {return makeResult(world_res, entity_res, unit_d, start, cache)}

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
            if (entity_res != null && entity_res!!.second <= points_iter.up_to) { return makeResult(null, entity_res, unit_d, start, cache) }
            return null
        }
    }

    @JvmStatic
    fun timedRaycast(raycastObj: RaycastObj, level: Level, timeout_ms: Long): Pair<RaycastReturn?, RaycastObj> {
        val start = getNowFast_ms()

        for (point in raycastObj.points_iter) {
            val res = raycastObj.iterate(point, level)
            if (res != null) {return Pair(res, raycastObj)}

            if (getNowFast_ms() - start > timeout_ms) {return Pair(null, raycastObj)}
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

        val max_dist = if (check_for_blocks_in_world) SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_raycast_distance else SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.max_raycast_no_worldcheking_distance
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
                val scale = Vector3d(ship.transform.shipToWorldScaling)
                val ship_wp = Vector3d(ship.transform.positionInWorld)
                val ship_sp = Vector3d(ship.transform.positionInShip)
                unit_d = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(unit_d.toJomlVector3d(), unit_d.toJomlVector3d()))
                Vector3d((ship.transform.transformDirectionNoScalingFromShipToWorld(
                    ((Vector3d(pos) - ship_sp + offset)*scale).toJomlVector3d(), org.joml.Vector3d()))
                ) + ship_wp
            }
        }
        return commonMakeRaycastObj(level, start, unit_d, distance, Vector3d(pos), null, check_for_blocks_in_world, onlyDistance, iterate_once)
    }
}