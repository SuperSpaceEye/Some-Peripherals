package net.spaceeye.someperipherals.raycasting

import com.mojang.math.Vector3d
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.util.IterateBetweenTwoPointsIter
import net.spaceeye.someperipherals.util.Ref
import net.spaceeye.someperipherals.util.hamilton_prod
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.checkForBlockInWorld
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.checkForIntersectedEntity
import net.spaceeye.someperipherals.raycasting.RaycastFunctions.rayIntersectsBox
import org.joml.Quaterniond
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
    var initial_t: Double,
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
//            logger.warn("FOUND INTERSECTING SHIP")
            ret.add(Pair(data, t))
        }
        return ret
    }

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
    fun checkRayPassedShip(
        start: Vector3d, ship: Pair<ServerShip, Double>, point:Vector3d, ray_distance: Double): Boolean {
        val pd = Vector3d(point.x - start.x, point.y - start.y, point.z - start.z)
        val point_dist = sqrt(pd.x*pd.x + pd.y*pd.y + pd.z*pd.z)

        val ship_dist = ship.second * ray_distance
//        if (ship_dist >= point_dist) {
//            logger.warn("RAY PASSED THE SHIP")
//        } else {
//            logger.warn("RAY IS YET TO PASS THE SHIP ${ship_dist - point_dist} $ship_dist, $point_dist")
//        }
        return ship_dist >= point_dist
    }

    @JvmStatic
    fun makeShipyardRay(
        start_in_world: Vector3d,
        d: Vector3d,
        current_iter_i: Int,
        max_iter_num: Int,
        ray_distance: Double,
        initial_t: Double,
        ship: ServerShip,
    ): Ray {
        val qrot = ship.transform.shipToWorldRotation.invert(Quaterniond())
        val ship_wp = ship.transform.positionInWorld
        val ship_sp = ship.transform.positionInShip

//        logger.warn("SHIP QUAT $qrot")
        //TODO for some reason if y is around 0 it just doesnt capture ship blocks, idk why
        // it also depends on eps, so idk why it happens
        // it does point in the other direction when this happens
//        d.y = min(1e+12, max(-1e+12, d.y))

        val start = org.joml.Vector3d(start_in_world.x, start_in_world.y, start_in_world.z)
        val joml_d = org.joml.Vector3d(d.x, d.y, d.z)

        logger.warn("D $joml_d")

        val sp_start = hamilton_prod(start.sub(ship_wp), qrot).add(ship_sp)
        val sp_d     = hamilton_prod(joml_d, qrot)
        val ray_unit_dir = org.joml.Vector3d(1/sp_d.x, 1/sp_d.y, 1/sp_d.z).normalize()

        //TODO find how to actually find necessary length
        //ship size
        val ss = org.joml.Vector3d(
            (ship.shipAABB!!.maxX() - ship.shipAABB!!.minX()).toDouble(),
            (ship.shipAABB!!.maxY() - ship.shipAABB!!.minY()).toDouble(),
            (ship.shipAABB!!.maxZ() - ship.shipAABB!!.minZ()).toDouble(),
        )
        val length = sqrt(ss.x*ss.x + ss.y*ss.y + ss.z*ss.z)
        val sp_end = sp_start.add(ray_unit_dir.mul(length, org.joml.Vector3d()), org.joml.Vector3d())

        logger.warn("SP_D ${sp_d.x} ${sp_d.y} ${sp_d.z}")
        logger.warn("UNIT RAY DIR ${ray_unit_dir.x} ${ray_unit_dir.y} ${ray_unit_dir.z}")
        logger.warn("START ${sp_start.x.toInt()} ${sp_start.y.toInt()} ${sp_start.z.toInt()}")
        logger.warn("STOP ${sp_end.x.toInt()} ${sp_end.y.toInt()} ${sp_end.z.toInt()}")
        logger.warn("LENGTH ${length}")

        val ray_max_len = max_iter_num - current_iter_i
        val ray = Ray(
            IterateBetweenTwoPointsIter(
                Vector3d(sp_start.x, sp_start.y, sp_start.z),
                Vector3d(sp_end  .x, sp_end  .y, sp_end  .z),
                ray_max_len),
            ship,
            Vector3d(sp_d.x, sp_d.y, sp_d.z),
            ray_distance,
            initial_t
        )

        return ray
    }

    @JvmStatic
    fun checkForShipIntersections(
        cur: Vector3d,
        next: Vector3d,
        ray_distance: Double,
        d: Vector3d,
        current_iter_i: Int,
        max_iter_num: Int,
        future_ship_intersections: MutableList<Pair<ServerShip, Double>>,
        ships_already_intersected: MutableList<ServerShip>,
        shipyard_rays: MutableList<Ray>
    ) {
        var i = 0
        var size = future_ship_intersections.size
        while (i < size) {
            val ship = future_ship_intersections[i]
            if (!checkRayPassedShip(cur, ship, next, ray_distance)) {i++; continue}
            shipyard_rays.add(makeShipyardRay(cur, d, current_iter_i, max_iter_num, ray_distance, ship.second, ship.first))

            ships_already_intersected.add(ship.first)
            future_ship_intersections[i] = future_ship_intersections.last()
            future_ship_intersections.removeLast()
            size--
        }
    }

    fun iterateShipRays(level: Level, rays: MutableList<Ray>): MutableList<Pair<RaycastReturn, Double>> {
        val hits = mutableListOf<Pair<RaycastReturn, Double>>()
        val to_remove = mutableListOf<Ray>()
        for (ray in rays) {
            if (!ray.iter.hasNext()) {
//                logger.warn("REMOVING RAY")
                to_remove.add(ray); continue}

            val point = ray.iter.next()
            val start = ray.iter.start
            val bpos = Ref(BlockPos(point.x, point.y, point.z))
            val res = Ref(level.getBlockState(bpos.it))
//            logger.warn("RAY POINT ${bpos.it.x} ${bpos.it.y} ${bpos.it.z}")

            val world_res = checkForBlockInWorld(start, point, bpos, res, ray.d, ray.ray_distance, level)
            if (world_res == null) {continue}

            val distance_to = (world_res.second + ray.initial_t) * ray.ray_distance
            hits.add(Pair(RaycastVSShipBlockReturn(ray.ship, world_res.first, distance_to), distance_to))
        }
        rays.removeAll(to_remove)
        return hits
    }

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

        val bpos = Ref(BlockPos(start.x, start.y, start.z))
        val res = Ref(level.getBlockState(bpos.it))

        val eps = 1e-60
        val next = pointsIter.nextNoStep()
        // unit vector of ray direction
        val rd = Vector3d(next.x - start.x, next.y - start.y, next.z - start.z)
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
            checkForShipIntersections(start, point, ray_distance, d, pointsIter.cur_i, pointsIter.max_len, future_ship_intersections, ships_already_intersected, shipyard_rays)
            val ship_hit_res = iterateShipRays(level, shipyard_rays)

            // Pair of (Pair of bpos, blockState), t
            val world_res = checkForBlockInWorld(start, point, bpos, res, d, ray_distance, level)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
            if ((world_res != null || !ship_hit_res.isEmpty()) && intersected_entity != null) { return calculateReturn(world_res, intersected_entity, ship_hit_res) }
            if ( world_res != null || !ship_hit_res.isEmpty()) {return calculateReturn(world_res, intersected_entity, ship_hit_res) }

//            if (world_res != null && !ship_hit_res.isEmpty()) {return calculateReturn(world_res, intersected_entity, ship_hit_res)}

            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (intersected_entity != null) { return calculateReturn(world_res, intersected_entity, ship_hit_res) }

                // Pair of Entity, t
                intersected_entity = checkForIntersectedEntity(start, point, level, d, ray_distance, er)
                addPossibleShipIntersections(
                    getIntersectingShips(level, point, er.toDouble(), start,  d),
                    future_ship_intersections, ships_already_intersected, shipyard_rays)
                entity_step_counter = 0
            }
            entity_step_counter++
        }

        return RaycastBlockReturn(Pair(bpos.it, res.it), pointsIter.max_len.toDouble())
    }
}