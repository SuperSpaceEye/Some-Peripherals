package net.spaceeye.someperipherals.util

import com.mojang.math.Vector3d
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.util.RaycastFunctions.checkForBlockInWorld
import net.spaceeye.someperipherals.util.RaycastFunctions.checkForIntersectedEntity
import net.spaceeye.someperipherals.util.RaycastFunctions.rayIntersectsBox
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.sqrt

class Ray(
    var iter: ray_iter_type,
    var ship: ServerShip
    ) {}

object VSRaycastFunctions {
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

    fun addPossibleShipIntersections(
        possible_ship_intersections: MutableList<Pair<ServerShip, Double>>,
        future_ship_intersections:   MutableList<Pair<ServerShip, Double>>,
        shipyard_rays: MutableList<Ray>
        ) {
        val new_items = mutableListOf<Pair<ServerShip, Double>>()
        for (item in possible_ship_intersections) {
            if (future_ship_intersections.firstOrNull { it.first.id == item.first.id } != null) {continue}
            if (shipyard_rays            .firstOrNull { it.ship.id  == item.first.id } != null) {continue}
            new_items.add(item)
        }
        future_ship_intersections.addAll(new_items)
    }

    @JvmStatic
    fun checkRayPassedShip(
        start: Vector3d, ship: Pair<ServerShip, Double>, point:Vector3d, ray_distance: Double): Boolean {
        val pd = Vector3d(point.x - start.x, point.y - start.y, point.z - start.z)
        val point_dist = pd.x*pd.x + pd.y*pd.y + pd.z*pd.z

        val ship_dist = ship.second * ray_distance

        return ship_dist >= point_dist
    }

    @JvmStatic
    fun makeShipyardRay(): Ray {
        TODO()
    }

    @JvmStatic
    fun checkMoveShips(
        start: Vector3d,
        point: Vector3d,
        ray_distance: Double,
        future_ship_intersections: MutableList<Pair<ServerShip, Double>>,
        shipyard_rays: MutableList<Ray>
    ) {
        var i = 0
        var size = future_ship_intersections.size
        while (i < size) {
            val ship = future_ship_intersections[i]
            if (!checkRayPassedShip(start, ship, point, ray_distance)) {i++; continue}
            shipyard_rays.add(makeShipyardRay())

            future_ship_intersections[i] = future_ship_intersections.last()
            future_ship_intersections.removeLast()
            size--
        }
    }

//    @JvmStatic
//    fun shipAABBIntersectsRay(ship: ServerShip, ray:Vector3d) {
////        val ship_quat = ship.transform.shipToWorldRotation.toMinecraft()
////        val inv_quat = Quaternion(ship_quat.i(), -ship_quat.k(), ship_quat.j(), -ship_quat.r())
////        val ray_quat = Quaternion(0.0f, ray.x.toFloat(), ray.y.toFloat(), ray.z.toFloat())
////        SomePeripherals.logger.warn("SHIP QUAT ${ship_quat}")
////        SomePeripherals.logger.warn("INV QUAT ${inv_quat}")
////        SomePeripherals.logger.warn("RAY QUAT ${ray_quat}")
////
////        ship_quat.mul(ray_quat)
////        ship_quat.mul(inv_quat)
////        val new_ray = Vector3d(ship_quat.k().toDouble(), ship_quat.j().toDouble(), ship_quat.r().toDouble())
//
////        SomePeripherals.logger.warn("NEW RAY ${new_ray.x} ${new_ray.y} ${new_ray.z}")
////        SomePeripherals.logger.warn("SHIP WORLD AABB ${ship.worldAABB.minX()} ${ship.worldAABB.minY()} ${ship.worldAABB.minZ()} | ${ship.worldAABB.maxX()} ${ship.worldAABB.maxY()} ${ship.worldAABB.maxZ()}")
////        SomePeripherals.logger.warn("SHIP SHIPYARD AABB ${ship.shipAABB?.minX()} ${ship.shipAABB?.minY()} ${ship.shipAABB?.minZ()} | ${ship.shipAABB?.maxX()} ${ship.shipAABB?.maxY()} ${ship.shipAABB?.maxZ()}")
//    }

    @JvmStatic
    fun vsRaycast(level: Level, pointsIter: ray_iter_type): Any {
        val start = pointsIter.start // starting position

        val bpos = Ref(BlockPos(start.x, start.y, start.z))
        val res = Ref(level.getBlockState(bpos.it))

        val eps = 1e-16
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
        val shipyard_rays = mutableListOf<Ray>()

        for (point in pointsIter) {
            addPossibleShipIntersections(
                getIntersectingShips(level, point, er.toDouble(), start,  d),
                future_ship_intersections, shipyard_rays)



            // Pair of (Pair of bpos, blockState), t
            val world_res = checkForBlockInWorld(start, point, bpos, res, d, level)

            //if the block and intersected entity are both hit, then we need to find out actual intersection as
            // checkForIntersectedEntity checks "er" block radius
//            if (world_res != null) {
//                SomePeripherals.logger.warn("DISTANCE TO BLOCK IS ${world_res.second * ray_distance}")
//            }
//            if (intersected_entity != null) {
//                SomePeripherals.logger.warn("DISTANCE TO ENTITY IS ${intersected_entity.second * ray_distance}")
//            }

            if (world_res != null && intersected_entity != null) { return if (world_res.second < intersected_entity.second) {world_res.first} else {intersected_entity.first} }
            if (world_res != null) {return world_res.first}

            //if ray hits entity and any block wasn't hit before another check, then previous intersected entity is the actual hit place
            if (check_for_entities && entity_step_counter % er == 0) {
                if (intersected_entity != null) { return intersected_entity.first }

                // Pair of Entity, t
                intersected_entity = checkForIntersectedEntity(start, point, level, d, er)
                entity_step_counter = 0
            }
            entity_step_counter++
        }

        return Pair(bpos.it, res.it)
    }
}