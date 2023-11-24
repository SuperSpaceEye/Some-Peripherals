package net.spaceeye.someperipherals.utils.radar

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.utils.mix.entityToMapRadar
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.transformToNearbyShipsAndWorld
import kotlin.math.max
import kotlin.math.min

private fun getScanPos(level: Level, pos: BlockPos): BlockPos {
    if (!SomePeripherals.has_vs) {return pos} else {
        val test = level.getShipManagingPos(pos)
        if (test != null) {
            val pos = test.toWorldCoordinates(pos)
            return BlockPos(pos.x, pos.y, pos.z)
        }
        return pos
    }
}

//TODO If there are too many entities, it will be slow to collect them all, and as cc is on its own thread, mc may
// add new entities, causing ConcurrentModificationException (though it may happen at any point accidentally)
private fun scanForEntities(r: Double, level: ServerLevel, pos: BlockPos): MutableList<Any> {
    val spos = getScanPos(level, pos)

    val res = mutableListOf<Any>()

    for (entity in level.getEntities(null, AABB(
        spos.x-r, spos.y-r, spos.z-r,
        spos.x+r, spos.y+r, spos.z+r
    ))) {
        if (entity == null) {continue;}
        res.add(entityToMapRadar(entity, SomePeripheralsConfig.SERVER.RADAR_SETTINGS.ALLOWED_ENTITY_DATA_SETTINGS))
    }

    return res
}

private fun scanForShips(radius: Double, level: ServerLevel, pos: BlockPos): MutableList<Any> {
    val spos = getScanPos(level, pos)

    val res = mutableListOf<Any>()

    val cur_ship = level.getShipManagingPos(pos)
    if (cur_ship != null) {res.add(shipToMap(cur_ship))}

    for (ship_pos in level.transformToNearbyShipsAndWorld(spos.x.toDouble(), spos.y.toDouble(), spos.z.toDouble(), radius)) {
        val ship = level.getShipManagingPos(ship_pos) ?: continue
        if (ship == cur_ship) {continue}
        res.add(shipToMap(ship))
    }

    return res
}

fun scanInRadius(radius: Double, level: Level, pos: BlockPos): Any {
    if (level.isClientSide) {return makeErrorReturn("Level is clientside. how.")}

    val er = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_entity_search_radius
    val sr = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_ship_search_radius

    val radius = max(radius, 1.0)

    val entity_radius = if (er <= 0) {radius} else {min(radius, er)}
    val ship_radius   = if (sr <= 0) {radius} else {min(radius, sr)}

    val results = mutableListOf<Any>()

    results.addAll(scanForEntities(entity_radius, level as ServerLevel, pos))
    if (SomePeripherals.has_vs) results.addAll(scanForShips(ship_radius, level, pos))

    return results
}