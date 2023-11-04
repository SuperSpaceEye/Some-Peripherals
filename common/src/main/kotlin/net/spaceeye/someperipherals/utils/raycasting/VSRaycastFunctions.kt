package net.spaceeye.someperipherals.utils.raycasting

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.checkForBlockInWorld
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.checkForIntersectedEntity
import net.spaceeye.someperipherals.utils.raycasting.RaycastFunctions.rayIntersectsBox
import net.spaceeye.someperipherals.utils.mix.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.coroutines.coroutineContext

data class Ray(
    var iter: RayIter,
    var ship: ServerShip,
    var d: Vector3d,
    var ray_distance: Double,
    var dist_to_ray_start: Double,
    var started_from_shipyard: Boolean,
    var world_unit_rd: Vector3d
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
        start: Vector3d,
        d: Vector3d,
        max_iter_num: Int,
        initial_ray_distance: Double,
        initial_t: Double,
        ship: ServerShip,
    ): Ray {
        val ship_wp = ship.transform.positionInWorld
        val ship_sp = ship.transform.positionInShip
        val scale   = ship.transform.shipToWorldScaling

        val start = ((start - Vector3d(ship_wp)) / Vector3d(scale)).toJomlVector3d()
        val world_dir = d.rdiv(1.0).snormalize().toJomlVector3d() //transform d back to ray direction

        val sp_start = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(start, start).add(ship_sp))
        val s_dir    = Vector3d(ship.transform.transformDirectionNoScalingFromWorldToShip(world_dir, world_dir))

        val sd = s_dir.rdiv(1.0, Vector3d()) // make d again

        //TODO find how to actually find necessary length
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
            initial_t * initial_ray_distance,
            initial_t < 1e-60, // if ray started from shipyard, t to intersection will be 0
            d.rdiv(1.0).snormalize()
        )

        return ray
    }

    // when ship is added to future intersections, its t is also saved, and as collision is calculated from starting point,
    // you can just calculate euclidean distance of diff between starting point and current pos, and compare it with t*ray_distance
    @JvmStatic
    inline fun checkRayPassedShip(start: Vector3d, ship: Pair<ServerShip, Double>, point: Vector3d, ray_distance: Double): Boolean
    = ship.second * ray_distance <= (point - start).dist()

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
            val intersection_point = start + rd * ship.second
            shipyard_rays.add(makeShipyardRay(intersection_point, d, max_iter_num, ray_distance, ship.second, ship.first))

            future_ship_intersections[i] = future_ship_intersections.last()
            future_ship_intersections.removeLast()
            size--
        }
    }

    @JvmStatic
    fun iterateShipRays(level: Level,
                        rays: MutableList<Ray>,
                        ships_already_intersected: MutableList<ServerShip>,
                        start: Vector3d,
                        shipyard_start: Vector3d,
                        cache: PosCache): MutableList<Pair<RaycastReturn, Double>> {
        val hits = mutableListOf<Pair<RaycastReturn, Double>>()
        var size = rays.size
        var i = 0
        while (i < size) {
            val ray = rays[i]

            if (!ray.iter.hasNext()) {
                ships_already_intersected.add(ray.ship)
                rays[i] = rays.last()
                rays.removeLast()
                size--
                continue
            }

            val point = ray.iter.next()

            // if ray has started from shipyard, then don't check starting pos (ray can clip into raycaster)
            if (ray.started_from_shipyard && point.floorCompare(shipyard_start)) {continue}
            val world_res = checkForBlockInWorld(ray.iter.start, point, ray.d, ray.ray_distance, level, cache) ?: continue
            val distance_to = world_res.second + ray.dist_to_ray_start
            hits.add(Pair(RaycastVSShipBlockReturn(
                ray.ship, world_res.first, distance_to,
                ray.world_unit_rd.normalize()*distance_to+start,
                ray.d.rdiv(1.0).snormalize()*world_res.second+ray.iter.start // norm_shipyard_rd * dist_to_shipyard_block
                ), distance_to))

            i++
        }
        return hits
    }

    @JvmStatic
    fun calculateReturn(
        world_res: Pair<Pair<BlockPos, BlockState>, Double>?,
        entity_res: Pair<Entity, Double>?,
        ships_res: MutableList<Pair<RaycastReturn, Double>>,
        world_unit_rd: Vector3d,
        start: Vector3d,
        cache: PosCache
    ): RaycastReturn {
        cache.cleanup()
        val results = ships_res
        if (world_res  != null) {results.add(Pair(RaycastBlockReturn ( world_res.first,  world_res.second, world_unit_rd* world_res.second+start),  world_res.second))}
        if (entity_res != null) {results.add(Pair(RaycastEntityReturn(entity_res.first, entity_res.second, world_unit_rd*entity_res.second+start), entity_res.second))}

        return results.minBy { it.second }.first
    }

    @JvmStatic
    suspend fun vsRaycast(level: Level, points_iter: RayIter, ignore_entity:Entity?=null, cache: PosCache, ctx: RaycastCtx?,
                          pos: Vector3d, unit_d: Vector3d,
                          check_for_blocks_in_world:Boolean=true): RaycastReturnOrCtx {
        val scope = CoroutineScope(coroutineContext)

        val start = points_iter.start // starting position
        val stop = points_iter.stop

        val shipyard_start = if (level.getShipManagingPos(pos.toBlockPos()) != null) { pos } else { start }

        val rd = stop - start
        val d = (rd+RaycastFunctions.eps).srdiv(1.0)
        val ray_distance = rd.dist()
        val world_unit_rd = rd.normalize()

        val check_for_entities = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.check_for_intersection_with_entities
        val er = if (check_for_blocks_in_world) SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius else SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS.entity_check_radius_no_worldchecking

        var entity_res: Pair<Entity, Double>? = ctx?.intersected_entity ?: null
        var entity_step_counter = ctx?.entity_step_counter ?: 0

        val future_ship_intersections = ctx?.future_ship_intersections ?: mutableListOf<Pair<ServerShip, Double>>()
        val ships_already_intersected = mutableListOf<ServerShip>()
        val shipyard_rays = ctx?.shipyard_rays ?: mutableListOf<Ray>()

        var ship_hit_res: MutableList<Pair<RaycastReturn, Double>> = ctx?.ship_hit_res ?: mutableListOf()
        var world_res: Pair<Pair<BlockPos, BlockState>, Double>? = ctx?.world_res ?: null

        for (point in points_iter) {
            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (entity_res != null) { return calculateReturn(world_res, entity_res, ship_hit_res, world_unit_rd, start, cache) }

                entity_res = checkForIntersectedEntity(start, point, level, d, ray_distance, er, ignore_entity)
                addPossibleShipIntersections(
                    getIntersectingShips(level, point, er.toDouble(), start,  d),
                    future_ship_intersections, ships_already_intersected, shipyard_rays)
                entity_step_counter = 0
            }
            entity_step_counter++

            //if you don't add unit_d to point, it incorrectly calculates that ray hits world block instead of ship block.
            // why? idfk, but this fixes it, i think. Idk what will happen if ship touches another ship though.
            checkForShipIntersections(start, point+unit_d, ray_distance, d, rd, points_iter.up_to, future_ship_intersections, shipyard_rays)

            ship_hit_res = iterateShipRays(level, shipyard_rays, ships_already_intersected, start, shipyard_start, cache)
            if (check_for_blocks_in_world) {world_res = checkForBlockInWorld(start, point, d, ray_distance, level, cache) }

            if (world_res != null || !ship_hit_res.isEmpty()) {return calculateReturn(world_res, entity_res, ship_hit_res, world_unit_rd, start, cache) }

            if (!scope.isActive) { return RaycastCtx(points_iter, ignore_entity, cache, pos, unit_d, entity_res, entity_step_counter, future_ship_intersections) }
        }

        if (entity_res != null && entity_res.second <= points_iter.up_to) {return calculateReturn(world_res, entity_res, ship_hit_res, world_unit_rd, start, cache)}

        return RaycastNoResultReturn(points_iter.up_to.toDouble())
    }
}