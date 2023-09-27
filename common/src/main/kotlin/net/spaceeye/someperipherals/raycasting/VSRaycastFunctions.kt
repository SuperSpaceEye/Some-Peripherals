package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Vector3d
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.checkForBlockInWorld
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.checkForIntersectedEntity
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.rayIntersectsBox
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.sqrt

class Ray(
    var iter: ray_iter_type,
    var ship: ServerShip,
    var d: Vector3d,
    var ray_distance: Double,
    var dist_to_ray_start: Double,
)

object VSRaycastFunctions {
    private val logger = SomePeripherals.slogger
    @JvmStatic
    fun getIntersectingShips(level: Level, pos: Vector3d, radius: Double, start: Vector3d, d: Vector3d): MutableList<Pair<ServerShip, Double>> {
        if (level.isClientSide) {return mutableListOf()}
        val ship_pos = level.transformToNearbyShipsAndWorld(pos.x, pos.y, pos.z, radius)
        val ret = mutableListOf<Pair<ServerShip, Double>>()
        for (spos in ship_pos) {
            val data = (level as ServerLevel).getShipManagingPos(spos) ?: continue
            val (res, t) = rayIntersectsBox(data.worldAABB.toMinecraft(), start, d)
            if (!res) {continue}
            logger.warn("FOUND INTERSECTING SHIP ${t}")
            ret.add(Pair(data, t))
        }
        return ret
    }

    @JvmStatic
    fun addPossibleShipIntersections(
        possible_ship_intersections: MutableList<Pair<ServerShip, Double>>,
        future_ship_intersections:   MutableList<Pair<ServerShip, Double>>,
        ships_already_intersected:   MutableList<ServerShip>,
        shipyard_rays: MutableList<Ray>
        ) {
        val new_items = mutableListOf<Pair<ServerShip, Double>>()
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
        start_in_world: Vector3d,
        d: Vector3d,
        max_iter_num: Int,
        initial_ray_distance: Double,
        initial_t: Double,
        ship: ServerShip,
    ): Ray {
        val ship_wp = ship.transform.positionInWorld
        val ship_sp = ship.transform.positionInShip

        val start = org.joml.Vector3d(start_in_world.x, start_in_world.y, start_in_world.z)
        val world_dir = org.joml.Vector3d(1.0/d.x, 1.0/d.y, 1.0/d.z).normalize() //transform d back to ray direction

        val sp_start = ship.transform.transformDirectionNoScalingFromWorldToShip(start.sub(ship_wp), start).add(ship_sp)
        val s_dir    = ship.transform.transformDirectionNoScalingFromWorldToShip(world_dir, world_dir)

        val sd = org.joml.Vector3d(1.0/s_dir.x, 1.0/s_dir.y, 1.0/s_dir.z) // make d again

        //TODO find how to actually find necessary length
        //ship size
        val ss = org.joml.Vector3d(
            (ship.shipAABB!!.maxX() - ship.shipAABB!!.minX()).toDouble(),
            (ship.shipAABB!!.maxY() - ship.shipAABB!!.minY()).toDouble(),
            (ship.shipAABB!!.maxZ() - ship.shipAABB!!.minZ()).toDouble(),
        )
        val length = sqrt(ss.x*ss.x + ss.y*ss.y + ss.z*ss.z)
        val sp_end = sp_start.add(s_dir.mul(length, org.joml.Vector3d()), org.joml.Vector3d())

        logger.warn("SHIP D ${sd.x} ${sd.y} ${sd.z}")
        logger.warn("S DIR ${s_dir.x} ${s_dir.y} ${s_dir.z}")
//        logger.warn("UNIT RAY DIR ${s_unit_d.x} ${s_unit_d.y} ${s_unit_d.z}")
        logger.warn("START ${sp_start.x.toInt()} ${sp_start.y.toInt()} ${sp_start.z.toInt()} | ${sp_start.x} ${sp_start.y} ${sp_start.z}")
        logger.warn("STOP ${sp_end.x.toInt()} ${sp_end.y.toInt()} ${sp_end.z.toInt()} | ${sp_end.x} ${sp_end.y} ${sp_end.z}")
        logger.warn("LENGTH ${length}")

        val ray = Ray(
            BresenhamIter(
                Vector3d(sp_start.x, sp_start.y, sp_start.z),
                Vector3d(sp_end.x, sp_end.y, sp_end.z),
                max_iter_num),
            ship,
            Vector3d(sd.x, sd.y, sd.z),
            sqrt(s_dir.x*s_dir.x + s_dir.y*s_dir.y + s_dir.z*s_dir.z),
            initial_t * initial_ray_distance
        )
        //im doing this so that on the first step it'll check the beginning pos in shipyard for block
        ray.iter.start = Vector3d(sp_start.x, sp_start.y, sp_start.z)

        return ray
    }

    // when ship is added to future intersections, its t is also saved, and as collision is calculated from starting point,
    // you can just calculate euclidean distance of diff between starting point and current pos, and compare it with t*ray_distance
    @JvmStatic
    fun checkRayPassedShip(
        start: Vector3d, ship: Pair<ServerShip, Double>, point:Vector3d, ray_distance: Double): Boolean {
        val pd = Vector3d(point.x - start.x, point.y - start.y, point.z - start.z)
        val point_dist = sqrt(pd.x*pd.x + pd.y*pd.y + pd.z*pd.z)

        logger.warn("SHIP INTERSECTION CHECK ${ship.second} ${ship.second * ray_distance} ${point_dist}")

        val ship_dist = ship.second * ray_distance
        return ship_dist <= point_dist
    }

    @JvmStatic
    fun checkForShipIntersections(
        start: Vector3d,
        next: Vector3d,
        ray_distance: Double,
        d: Vector3d,
        rd: Vector3d,
        max_iter_num: Int,
        future_ship_intersections: MutableList<Pair<ServerShip, Double>>,
        shipyard_rays: MutableList<Ray>
    ) {
        // i've probably overcomplicated it, but who cares. not me
        var i = 0
        var size = future_ship_intersections.size
        while (i < size) {
            val ship = future_ship_intersections[i]
            if (!checkRayPassedShip(start, ship, next, ray_distance)) {i++; continue}
            val intersection_point = Vector3d(
                start.x + rd.x*ship.second,
                start.y + rd.y*ship.second,
                start.z + rd.z*ship.second,
            )
            shipyard_rays.add(makeShipyardRay(intersection_point, d, max_iter_num, ray_distance, ship.second, ship.first))

            future_ship_intersections[i] = future_ship_intersections.last()
            future_ship_intersections.removeLast()
            size--
        }
    }

    @JvmStatic
    fun iterateShipRays(level: Level, rays: MutableList<Ray>, ships_already_intersected: MutableList<ServerShip>): MutableList<Pair<RaycastReturn, Double>> {
        val hits = mutableListOf<Pair<RaycastReturn, Double>>()
        val to_remove = mutableListOf<Ray>()
        for (ray in rays) {
            if (!ray.iter.hasNext()) {
                logger.warn("REMOVING SHIP")
                to_remove.add(ray); continue}
            val point = ray.iter.next()
            logger.warn("SHIP POINT ${point.x} ${point.y} ${point.z}")
            val world_res = checkForBlockInWorld(ray.iter.start, point, ray.d, ray.ray_distance, level) ?: continue
            val distance_to = world_res.second + ray.dist_to_ray_start
            hits.add(Pair(RaycastVSShipBlockReturn(ray.ship, world_res.first, distance_to), distance_to))
        }
        rays.removeAll(to_remove)
        ships_already_intersected.addAll(to_remove.map { it.ship })
        return hits
    }

    @JvmStatic
    fun calculateReturn(
        world_res: Pair<Pair<BlockPos, BlockState>, Double>?,
        entity_res: Pair<Entity, Double>?,
        ships_res: MutableList<Pair<RaycastReturn, Double>>,
    ): RaycastReturn {

        val results = ships_res
        if (world_res != null)  {results.add(Pair(RaycastBlockReturn (world_res.first,  world_res.second),  world_res.second))}
        if (entity_res != null) {results.add(Pair(RaycastEntityReturn(entity_res.first, entity_res.second), entity_res.second))}

        return results.minBy { it.second }.first
    }

    @JvmStatic
    fun vsRaycast(level: Level, pointsIter: ray_iter_type): RaycastReturn {
        val start = pointsIter.start // starting position
        val stop = pointsIter.stop

        val eps = RaycastFunctions.eps
        val rd = Vector3d(stop.x - start.x, stop.y - start.y, stop.z - start.z)
        val d = Vector3d(1.0/(rd.x + eps), 1.0/(rd.y + eps), 1.0/(rd.z + eps))
        val ray_distance = sqrt(rd.x*rd.x + rd.y*rd.y + rd.z*rd.z)

        val check_for_entities = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.check_for_entities
        val er = SomePeripheralsConfig.SERVER.COMMON.RAYCASTER_SETTINGS.entity_check_radius

        var intersected_entity: Pair<Entity, Double>? = null
        var entity_step_counter = 0

        val future_ship_intersections = mutableListOf<Pair<ServerShip, Double>>()
        val ships_already_intersected = mutableListOf<ServerShip>()
        val shipyard_rays = mutableListOf<Ray>()

        for (point in pointsIter) {
            logger.warn("WORLD POINT ${point.x} ${point.y} ${point.z}")
            checkForShipIntersections(start, point, ray_distance, d, rd, pointsIter.up_to, future_ship_intersections, shipyard_rays)

            val ship_hit_res = iterateShipRays(level, shipyard_rays, ships_already_intersected)
            val world_res = checkForBlockInWorld(start, point, d, ray_distance, level)

            if ((world_res != null || !ship_hit_res.isEmpty()) && intersected_entity != null) { return calculateReturn(world_res, intersected_entity, ship_hit_res) }
            if ( world_res != null || !ship_hit_res.isEmpty()) {return calculateReturn(world_res, intersected_entity, ship_hit_res) }

            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (intersected_entity != null) { return calculateReturn(world_res, intersected_entity, ship_hit_res) }

                intersected_entity = checkForIntersectedEntity(start, point, level, d, ray_distance, er)
                addPossibleShipIntersections(
                    getIntersectingShips(level, point, er.toDouble(), start,  d),
                    future_ship_intersections, ships_already_intersected, shipyard_rays)
                entity_step_counter = 0
            }
            entity_step_counter++
        }

        return RaycastNoResultReturn(pointsIter.up_to.toDouble())
    }
}