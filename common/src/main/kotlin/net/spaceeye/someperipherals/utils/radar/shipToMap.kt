package net.spaceeye.someperipherals.utils.radar

import net.spaceeye.someperipherals.SomePeripheralsConfig
import org.valkyrienskies.core.api.ships.ServerShip

private inline fun getPos(ship: ServerShip) = mapOf(
    Pair("x", ship.transform.positionInWorld.x()),
    Pair("y", ship.transform.positionInWorld.y()),
    Pair("z", ship.transform.positionInWorld.z())
)

private inline fun getRot(ship: ServerShip) = mapOf(
    Pair("x", ship.transform.shipToWorldRotation.x()),
    Pair("y", ship.transform.shipToWorldRotation.y()),
    Pair("z", ship.transform.shipToWorldRotation.z()),
    Pair("w", ship.transform.shipToWorldRotation.w())
)

private inline fun getVelocity(ship: ServerShip) = mapOf(
    Pair("x", ship.velocity.x()),
    Pair("y", ship.velocity.y()),
    Pair("z", ship.velocity.z())
)

private inline fun getSize(ship: ServerShip): Map<String, Any> {
    val aabb = ship.shipAABB!!
    return mapOf(
        Pair("x", aabb.maxX() - aabb.minX()),
        Pair("y", aabb.maxY() - aabb.minY()),
        Pair("z", aabb.maxZ() - aabb.minZ())
    )
}

private inline fun getScale(ship: ServerShip) = mapOf(
    Pair("x", ship.transform.shipToWorldScaling.x()),
    Pair("y", ship.transform.shipToWorldScaling.y()),
    Pair("z", ship.transform.shipToWorldScaling.z())
)

private inline fun getMomentOfInertiaTensor(ship: ServerShip): List<List<Double>> {
    val it = ship.inertiaData.momentOfInertiaTensor
    return listOf(
        listOf(it.m00(), it.m01(), it.m02()),
        listOf(it.m10(), it.m11(), it.m12()),
        listOf(it.m20(), it.m21(), it.m22())
    )
}

fun shipToMap(ship: ServerShip): MutableMap<String, Any> {
    val res = mutableMapOf<String, Any>()
    val s = SomePeripheralsConfig.SERVER.RADAR_SETTINGS.ALLOWED_SHIP_DATA_SETTINGS

    res["is_ship"] = true
    if (s.id) res["id"] = ship.id
    if (s.pos) res["pos"] = getPos(ship)
    if (s.mass) res["mass"] = ship.inertiaData.mass
    if (s.rotation) res["rotation"] = getRot(ship)
    if (s.velocity) res["velocity"] = getVelocity(ship)
    if (s.size) res["size"] = getSize(ship)
    if (s.scale) res["scale"] = getScale(ship)
    if (s.moment_of_inertia_tensor) res["moment_of_inertia_tensor"] = getMomentOfInertiaTensor(ship)

    return res
}