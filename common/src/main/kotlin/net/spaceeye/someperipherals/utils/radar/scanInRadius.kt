package net.spaceeye.someperipherals.utils.radar

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.SomePeripherals
import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.mixin.IServerLevelAccessor
import net.spaceeye.someperipherals.utils.mix.entityToMapRadar
import net.spaceeye.someperipherals.utils.mix.getNowFast_ms
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

private fun <T> getEntitiesWithTimeout(level: ServerLevel,
                               area: AABB,
                               timeout: Long = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_entity_timeout_ms,
                               fn: (entity: Entity) -> T): MutableList<T> {
    val entities = ArrayList<T>()

    val now = getNowFast_ms()
    try {
        (level as IServerLevelAccessor).entities.get(area) {
            if (getNowFast_ms() - now > timeout) { throw RuntimeException() }
            entities.add(fn(it))
        }
    } catch (_: RuntimeException) {
    } catch (_: ConcurrentModificationException) {}

    return entities
}


private fun scanForEntities(r: Double, level: ServerLevel, pos: BlockPos): MutableList<Any> {
    val pos = getScanPos(level, pos)

    val res: MutableList<Any> = getEntitiesWithTimeout(level, AABB(
        pos.x-r, pos.y-r, pos.z-r,
        pos.x+r, pos.y+r, pos.z+r
    )) {entityToMapRadar(it, SomePeripheralsConfig.SERVER.RADAR_SETTINGS.ALLOWED_ENTITY_DATA_SETTINGS)}

    return res
}

private fun scanForPlayers(r: Double, level: ServerLevel, pos: BlockPos): MutableList<Any> {
    val pos = getScanPos(level, pos)
    val res = arrayListOf<Any>()
    for (player in level.server.playerList.players) {
        if ( !(player.level.dimension() == level.dimension()
            && AABB(
                pos.x-r, pos.y-r, pos.z-r,
                pos.x+r, pos.y+r, pos.z+r
            ).contains(player.x, player.y, player.z)
            )) { continue }
        res.add(entityToMapRadar(player, SomePeripheralsConfig.SERVER.RADAR_SETTINGS.ALLOWED_ENTITY_DATA_SETTINGS))
    }

    return res
}

private fun scanForShips(radius: Double, level: ServerLevel, pos: BlockPos): MutableList<Any> {
    val pos = getScanPos(level, pos)

    val res = mutableListOf<Any>()

    val cur_ship = level.getShipManagingPos(pos)
    if (cur_ship != null) {res.add(shipToMap(cur_ship))}

    for (ship_pos in level.transformToNearbyShipsAndWorld(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), radius)) {
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

fun scanForShipsInRadius(radius: Double, level: Level, pos: BlockPos): Any {
    if (level.isClientSide) {return makeErrorReturn("Level is clientside. how.")}
    if (!SomePeripherals.has_vs) {return makeErrorReturn("No Valkyrien Skies installed.")}

    val sr = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_ship_search_radius
    val radius = max(radius, 1.0)
    val ship_radius = if (sr <= 0) {radius} else {min(radius, sr)}

    return scanForShips(ship_radius, level as ServerLevel, pos)
}

fun scanForEntitiesInRadius(radius: Double, level: Level, pos: BlockPos): Any {
    if (level.isClientSide) {return makeErrorReturn("Level is clientside. how.")}

    val er = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_entity_search_radius
    val radius = max(radius, 1.0)
    val entity_radius = if (er <= 0) {radius} else {min(radius, er)}

    return scanForEntities(entity_radius, level as ServerLevel, pos)
}

fun scanForPlayersInRadius(radius: Double, level: Level, pos: BlockPos): Any {
    if (level.isClientSide) {return makeErrorReturn("Level is clientside. how.")}

    val er = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.max_entity_search_radius
    val radius = max(radius, 1.0)
    val entity_radius = if (er <= 0) {radius} else {min(radius, er)}

    return scanForPlayers(entity_radius, level as ServerLevel, pos)
}