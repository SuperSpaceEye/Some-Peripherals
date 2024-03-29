package net.spaceeye.someperipherals.stuff.utils

import org.valkyrienskies.core.api.ships.Ship

fun posShipToWorld(ship: Ship, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.transform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.transform.positionInWorld)
    val ship_sp = Vector3d(ship.transform.positionInShip)
    return Vector3d((ship.transform.transformDirectionNoScalingFromShipToWorld(
        ((pos - ship_sp)*scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_wp
}

fun posWorldToShip(ship: Ship, pos: Vector3d): Vector3d {
    val scale = Vector3d(ship.transform.shipToWorldScaling)
    val ship_wp = Vector3d(ship.transform.positionInWorld)
    val ship_sp = Vector3d(ship.transform.positionInShip)
    return Vector3d((ship.transform.transformDirectionNoScalingFromWorldToShip(
            ((pos - ship_wp) / scale).toJomlVector3d(), org.joml.Vector3d()))
    ) + ship_sp
}