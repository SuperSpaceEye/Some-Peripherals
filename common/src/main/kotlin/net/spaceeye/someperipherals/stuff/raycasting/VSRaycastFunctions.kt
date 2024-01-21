package net.spaceeye.someperipherals.stuff.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.stuff.raycasting.RaycastFunctions.checkForBlockInWorld
import net.spaceeye.someperipherals.stuff.raycasting.RaycastFunctions.rayIntersectsBox
import net.spaceeye.someperipherals.stuff.utils.JVector3d
import net.spaceeye.someperipherals.stuff.utils.Vector3d
import net.spaceeye.someperipherals.stuff.utils.posShipToWorld
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.max

class Ray(
    var iter: RayIter,
    var ship: Ship,
    var d: Vector3d,
    var ray_distance: Double,
    var dist_to_ray_start: Double,
    var started_from_shipyard: Boolean,
    var world_unit_rd: Vector3d
) {
    var has_entered = false
    var can_iterate = true
    init {
        // check for collision of shipyard ray with ship's aabb
        val aabb = ship.shipAABB!!
        val cpos = iter.start
        if (   cpos.x >= aabb.minX() && cpos.x <= aabb.maxX()
            && cpos.y >= aabb.minY() && cpos.y <= aabb.maxY()
            && cpos.z >= aabb.minZ() && cpos.z <= aabb.maxZ()) {
            has_entered = true
        }
    }

    operator fun hasNext() = iter.hasNext() && can_iterate
    operator fun iterator() = this
    operator fun next(): Vector3d {
        val cpos = iter.cpos
        val aabb = ship.shipAABB!!
        // TODO check if ray doesn't intersect ship boundaries and do early return
        if (!has_entered) {
            if (   cpos.x >= aabb.minX() && cpos.x <= aabb.maxX()
                && cpos.y >= aabb.minY() && cpos.y <= aabb.maxY()
                && cpos.z >= aabb.minZ() && cpos.z <= aabb.maxZ()) {
                has_entered = true
            }
        }

        if (has_entered && !(
               cpos.x >= aabb.minX() && cpos.x <= aabb.maxX()
            && cpos.y >= aabb.minY() && cpos.y <= aabb.maxY()
            && cpos.z >= aabb.minZ() && cpos.z <= aabb.maxZ())) {
            can_iterate = false
        }

        return iter.next()
    }
}

class VSRaycastBlockRes(bpos: BlockPos, res: IBlockRes, t_to_in: Double, @JvmField var ray: Ray): BaseRaycastBlockRes(bpos, res, t_to_in)

object VSRaycastFunctions {
    @JvmStatic
    fun getIntersectingShips(level: Level, pos: Vector3d, radius: Double, start: Vector3d, d: Vector3d): MutableList<Pair<Ship, Double>> {
        val shipsPos = level.transformToNearbyShipsAndWorld(pos.x, pos.y, pos.z, radius)
        val ret = mutableListOf<Pair<Ship, Double>>()
        for (spos in shipsPos) {
            val ship = level.getShipManagingPos(spos) ?: continue
            val res = rayIntersectsBox(ship.worldAABB.toMinecraft(), start, d)
            if (!res.intersects) {continue}
            ret.add(Pair(ship, res.t_to_in))
        }
        return ret
    }

    @JvmStatic
    fun addPossibleShipIntersections(
        possible_ship_intersections: MutableList<Pair<Ship, Double>>,
        future_ship_intersections:   MutableList<Pair<Ship, Double>>,
        ships_already_intersected:   MutableList<Ship>,
        shipyard_rays: MutableList<Ray>
        ) {
        val new_items = mutableListOf<Pair<Ship, Double>>()
        for (item in possible_ship_intersections) {
            if (future_ship_intersections.firstOrNull { it.first.id == item.first.id } != null) {continue}
            if (shipyard_rays            .firstOrNull { it.ship.id  == item.first.id } != null) {continue}
            if (ships_already_intersected.firstOrNull { it.id       == item.first.id } != null) {continue}
            new_items.add(item)
        }
        future_ship_intersections.addAll(new_items)
    }

    @JvmStatic
    fun makeShipyardRay(
        shipyard_ray_start: Vector3d,
        d: Vector3d,
        max_iter_num: Int,
        world_ray_distance: Double,
        world_t: Double,
        ship: Ship,
    ): Ray {
        val ship_wp = ship.transform.positionInWorld
        val ship_sp = ship.transform.positionInShip
        val scale   = ship.transform.shipToWorldScaling

        val start = ((shipyard_ray_start - Vector3d(ship_wp)) / Vector3d(scale)).toJomlVector3d()
        val world_dir = d.rdiv(1.0).snormalize().toJomlVector3d() //transform d back to ray direction

        val sp_start = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(start, start).add(ship_sp))
        val s_dir    = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(world_dir, world_dir))

        val sd = s_dir.rdiv(1.0, Vector3d()) // make d again

        //ship size
        val ss = Vector3d(
            (ship.shipAABB!!.maxX() - ship.shipAABB!!.minX()).toDouble(),
            (ship.shipAABB!!.maxY() - ship.shipAABB!!.minY()).toDouble(),
            (ship.shipAABB!!.maxZ() - ship.shipAABB!!.minZ()).toDouble(),
        )
        val sp_end = sp_start + s_dir * ss.dist()

        val ray = Ray(
            DDAIter(sp_start, sp_end, max_iter_num),
            ship, sd, s_dir.dist(),
            max(world_t, 0.0) * world_ray_distance,
            world_t < 1e-60, // if ray started from shipyard, it's realworld position will start in ship hitbox
            d.rdiv(1.0).snormalize()
        )

        return ray
    }

    // when ship is added to future intersections, its t is also saved, and as collision is calculated from starting point,
    // you can just calculate euclidean distance of diff between starting point and current pos, and compare it with t*ray_distance
    @JvmStatic
    inline fun checkIfRayPassedShip(start: Vector3d, ship: Pair<Ship, Double>, point: Vector3d, ray_distance: Double): Boolean
    = ship.second * ray_distance <= (point - start).dist()

    @JvmStatic
    fun checkForShipIntersections(
        start: Vector3d,
        next: Vector3d,
        ray_distance: Double,
        d: Vector3d,
        rd: Vector3d,
        max_iter_num: Int,
        future_ship_intersections: MutableList<Pair<Ship, Double>>,
        shipyard_rays: MutableList<Ray>
    ) {
        // i've probably overcomplicated it, but who cares. not me
        var i = 0
        var size = future_ship_intersections.size
        while (i < size) {
            val ship = future_ship_intersections[i]
            if (!checkIfRayPassedShip(start, ship, next, ray_distance)) {i++; continue}
            val ship_hit_point = start + rd * max(ship.second, 0.0) // max to prevent negative t
            shipyard_rays.add(makeShipyardRay(ship_hit_point, d, max_iter_num, ray_distance, ship.second, ship.first))

            future_ship_intersections[i] = future_ship_intersections.last()
            future_ship_intersections.removeLast()
            size--
        }
    }

    @JvmStatic
    fun iterateShipRays(level: Level,
                        rays: MutableList<Ray>,
                        ships_already_intersected: MutableList<Ship>,
                        start: Vector3d,
                        shipyard_start: Vector3d,
                        cache: PosManager,
                        onlyDistance: Boolean): MutableList<Pair<RaycastReturn, Double>> {
        val hits = mutableListOf<Pair<RaycastReturn, Double>>()
        var size = rays.size
        var i = 0
        while (i < size) {
            val ray = rays[i]

            if (!ray.hasNext()) {
                ships_already_intersected.add(ray.ship)
                rays[i] = rays.last()
                rays.removeLast()
                size--
                continue
            }

            val point = ray.next()

            // if ray has started from shipyard, then don't check starting pos (ray can clip into raycaster)
            if (ray.started_from_shipyard && point.floorCompare(shipyard_start)) {continue}
            val world_res = checkForBlockInWorld(ray.iter.start, point, ray.d, ray.ray_distance, level, cache, onlyDistance) {bpos, res, dist -> VSRaycastBlockRes(bpos, res, dist, ray)} ?: continue
            val distance_to = world_res.dist_to_in //TODO what was i doing here? "+ ray.dist_to_ray_start"
            hits.add(Pair(RaycastVSShipBlockReturn(start,
                ray.ship, world_res.bpos, world_res.res, distance_to,
                ray.world_unit_rd.normalize()*distance_to+start,
                ray.d.rdiv(1.0).snormalize()*world_res.dist_to_in+ray.iter.start, // norm_shipyard_rd * dist_to_shipyard_block
                ray
                ), distance_to))

            i++
        }
        return hits
    }

    @JvmStatic
    fun calculateReturn(
        world_res: BaseRaycastBlockRes?,
        entity_res: Pair<Entity, Double>?,
        ships_res: MutableList<Pair<RaycastReturn, Double>>,
        world_unit_rd: Vector3d,
        start: Vector3d,
        cache: PosManager
    ): RaycastReturn {
        cache.cleanup()
        val results = ships_res
        if (world_res  != null) {results.add(Pair(RaycastBlockReturn (start, world_res.bpos, world_res.res, world_res.dist_to_in, world_unit_rd* world_res.dist_to_in+start), world_res.dist_to_in))}
        if (entity_res != null) {results.add(Pair(RaycastEntityReturn(start, entity_res.first, entity_res.second, world_unit_rd*entity_res.second+start), entity_res.second))}

        return results.minBy { it.second }.first
    }

    class VSRaycastObj(start: Vector3d, stop: Vector3d, ignore_entity: Entity?,
                       points_iter: RayIter, val pos: Vector3d, val world_unit_rd: Vector3d, val level: Level,
                       check_for_blocks_in_world: Boolean, onlyDistance: Boolean)
        : RaycastFunctions.RaycastObj(start, stop, ignore_entity, points_iter, check_for_blocks_in_world, onlyDistance){

        val shipyard_start = if (level.getShipManagingPos(pos.toBlockPos()) != null) { pos } else { start }

        val future_ship_intersections = mutableListOf<Pair<Ship, Double>>()
        val ships_already_intersected = mutableListOf<Ship>()
        var shipyard_rays = mutableListOf<Ray>()

        var ship_step_counter = 0

        var ship_hit_res: MutableList<Pair<RaycastReturn, Double>> = mutableListOf()

        override fun iterate(point: Vector3d, level: ServerLevel): RaycastReturn? {
            val res = super.iterate(point, level)
            if (res is RaycastERROR) {return res}

            if (check_for_entities && ship_step_counter % er == 0) {
                addPossibleShipIntersections(
                    getIntersectingShips(level, point, er.toDouble(), start,  d),
                    future_ship_intersections, ships_already_intersected, shipyard_rays)
                ship_step_counter = 0
            }
            ship_step_counter++

            //TODO check if it's actually true
            //if you don't add unit_d to point, it incorrectly calculates that ray hits world block instead of ship block.
            // why? idfk, but this fixes it, i think. Idk what will happen if ship touches another ship though.
            checkForShipIntersections(start, point+unit_d, ray_distance, d, rd, points_iter.up_to, future_ship_intersections, shipyard_rays)

            //TODO because i immediately make ray at "point" and then run iterateShipRays, shipyard rays are 1 step farther than they should be, at least i think so.
            ship_hit_res = iterateShipRays(level, shipyard_rays, ships_already_intersected, start, shipyard_start, cache, onlyDistance)

            if (ship_hit_res.isNotEmpty()) {
                val res = ship_hit_res.minBy { it.second }.first as RaycastVSShipBlockReturn
                //TODO TODO TODO
                tryReflectRay(VSRaycastBlockRes(res.bpos, res.res, res.distance_to, res.ray))
            }

            //TODO double calculation of result but idfc
            if (res != null || ship_hit_res.isNotEmpty()) {return calculateReturn(world_res, entity_res, ship_hit_res, world_unit_rd, start, cache)}

            return null
        }

        override fun postCheckForUnreturnedEntity(): RaycastReturn? {
            if (entity_res != null && entity_res!!.second <= points_iter.up_to) {return calculateReturn(world_res, entity_res, ship_hit_res, world_unit_rd, start, cache)}
            return null
        }

        override fun tryReflectRay(
            world_res: BaseRaycastBlockRes?
        ): BaseRaycastBlockRes? {
            if (world_res != null && world_res.bpos == last_reflected_pos) {
                ship_hit_res.clear()
                return null
            }
            if (
                world_res is VSRaycastBlockRes
                && !(SomePeripherals.has_arc && onlyDistance)
                //TODO entities
                ) {
                val ray = world_res.ray
                start = ray.iter.start
                rd = ray.d.rdiv(1.0, Vector3d())
                d = ray.d
                ray_distance = ray.ray_distance
                unit_d = rd.normalize()
            }

            val res = super.tryReflectRay(world_res)
            // if res != world_res, then world_res was a mirror, and reflection happened
            if (res == world_res) {return res}

            if (world_res is VSRaycastBlockRes
                && !(SomePeripherals.has_arc && onlyDistance)) {
                val ship = world_res.ray.ship
                start = posShipToWorld(ship, start)

                rd = Vector3d(ship.transform.transformDirectionNoScalingFromShipToWorld(rd.toJomlVector3d(), JVector3d()))
                d = rd.rdiv(1.0)
                unit_d = rd.normalize()

                val temp_cur_i = points_iter.cur_i - 1
                points_iter = DDAIter(start, start + unit_d * (points_iter.up_to - temp_cur_i), points_iter.up_to)
                points_iter.cur_i = temp_cur_i
                points_iter.next()
            }

            shipyard_rays.clear()
            future_ship_intersections.clear()
            ships_already_intersected.clear()
            ship_hit_res.clear()
            ship_step_counter = 0

            return res
        }
    }
}