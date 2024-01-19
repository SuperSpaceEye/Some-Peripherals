package net.spaceeye.someperipherals.integrations.cc

import net.spaceeye.someperipherals.SomePeripheralsConfig
import net.spaceeye.someperipherals.stuff.raycasting.*

private fun makeBlockReturn(
    res: RaycastBlockReturn,
    ret: MutableMap<Any, Any>,
    rcc: SomePeripheralsConfig.Server.RaycasterSettings
) {
    val pos = res.bpos
    val bs  = res.res

    ret["is_block"] = true
    if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
    if (rcc.return_hit_pos)  {ret["hit_pos"] = res.hit_position.toArray()}
    if (rcc.return_distance) {ret["distance"] = res.distance_to}
    if (rcc.return_block_type) {ret["block_type"] = if (bs is FullBlockRes) {bs.state.block.descriptionId.toString()} else {""}}
    if (rcc.return_rel_hit_pos) {ret["rel_hit_pos"] = (res.hit_position - res.origin).toArray() }
}

private fun makeEntityReturn(
    res: RaycastEntityReturn,
    ret: MutableMap<Any, Any>,
    rcc: SomePeripheralsConfig.Server.RaycasterSettings
) {
    val entity = res.result

    ret["is_entity"] = true
    if (rcc.return_abs_pos)   {ret["abs_pos"] = mutableListOf(entity.x, entity.y, entity.z)}
    if (rcc.return_hit_pos)   {ret["hit_pos"] = res.hit_position.toArray()}
    if (rcc.return_distance)  {ret["distance"] = res.distance_to}
    if (rcc.return_entity_id) {ret["id"] = entity.id}
    if (rcc.return_rel_hit_pos) {ret["rel_hit_pos"] = (res.hit_position - res.origin).toArray() }

    if (rcc.return_entity_type) {ret["descriptionId"] = entity.type.descriptionId}
}

private fun makeVSBlockReturn(
    res: RaycastVSShipBlockReturn,
    ret: MutableMap<Any, Any>,
    rcc: SomePeripheralsConfig.Server.RaycasterSettings
) {
    val pos = res.bpos
    val bs  = res.res

    ret["is_block"] = true
    if (rcc.return_abs_pos)  {ret["abs_pos"] = mutableListOf(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())}
    if (rcc.return_hit_pos)  {ret["hit_pos"] = res.hit_position.toArray()}
    if (rcc.return_distance) {ret["distance"] = res.distance_to}
    if (rcc.return_block_type) {ret["block_type"] = if (bs is FullBlockRes) {bs.state.block.descriptionId.toString()} else {""}}
    if (rcc.return_rel_hit_pos) {ret["rel_hit_pos"] = (res.hit_position - res.origin).toArray() }

    if (rcc.return_ship_id)  {ret["ship_id"] = res.ship.id.toDouble()}
    if (rcc.return_shipyard_hit_pos) {ret["hit_pos_ship"] = res.hit_position_ship.toArray()}
}

private fun makeNoResultReturn(
    res: RaycastNoResultReturn,
    ret: MutableMap<Any, Any>,
    rcc: SomePeripheralsConfig.Server.RaycasterSettings
) {
    ret["is_block"] = true
    ret["distance"] = res.distance_to
    ret["block_type"] = "block.minecraft.air"
}

fun makeRaycastReturn(res: RaycastReturn): MutableMap<Any, Any> {
    val ret = mutableMapOf<Any, Any>()
    val rcc = SomePeripheralsConfig.SERVER.RAYCASTER_SETTINGS

    when (res) {
        is RaycastBlockReturn -> makeBlockReturn(res, ret, rcc)
        is RaycastEntityReturn -> makeEntityReturn(res, ret, rcc)
        is RaycastNoResultReturn -> makeNoResultReturn(res, ret, rcc)
        is RaycastVSShipBlockReturn -> makeVSBlockReturn(res, ret, rcc)
        is RaycastERROR -> {ret["error"] = res.error_str}
        else -> {ret["error"] = "Something went very, very wrong, as this should never ever happen"}
    }

    return ret
}