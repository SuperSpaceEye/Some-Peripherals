package net.spaceeye.someperipherals.utils.mix

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.spaceeye.someperipherals.SomePeripheralsConfig

//TODO this is stupid

fun entityToMapGoggles(entity: Entity, s: SomePeripheralsConfig.Server.GogglesSettings.GogglesAllowedEntityData): MutableMap<String, Any> {
    val item = mutableMapOf<String, Any>()

    item["is_entity"] = true
    if(s.pos) item["pos"] = listOf(entity.x, entity.y, entity.z)
    if(s.eye_pos) item["eye_pos"] = listOf(entity.eyePosition.x, entity.eyePosition.y, entity.eyePosition.z)
    if(s.eye_height) item["eye_height"] = entity.eyeHeight.toDouble()
    if(s.look_angle) item["look_angle"] = listOf(entity.lookAngle.x, entity.lookAngle.y, entity.lookAngle.z)
    if(s.dimension) item["dimension"] = entity.level.dimension().toString()
    if(s.entity_type) item["entity_type"] = entity.type.descriptionId
    if(s.air_supply) item["air_supply"] = entity.airSupply
    if(s.max_air_supply) item["max_air_supply"] = entity.maxAirSupply

    //TODO main hand? off hand? effects?
    if (entity is LivingEntity) {
        if (s.health) item["health"] = entity.health.toDouble()
        if (s.max_health) item["max_health"] = entity.maxHealth.toDouble()
        if (s.armor_value) item["armor_value"] = entity.armorValue
        if (s.armor_cover_percentage) item["armor_cover_percentage"] = entity.armorCoverPercentage.toDouble()
        if (s.absorption_amount) item["absorption_amount"] = entity.absorptionAmount.toDouble()
        if (s.is_baby) item["is_baby"] = entity.isBaby
        if (s.is_blocking) item["is_blocking"] = entity.isBlocking
        if (s.is_sleeping) item["is_sleeping"] = entity.isSleeping
        if (s.is_fall_flying) item["is_fall_flying"] = entity.isFallFlying
        if (s.speed) item["speed"] = entity.speed.toDouble()
        if (s.xRot) item["xRot"] = entity.xRot.toDouble() // pitch in degrees
        if (s.yHeadRot) item["yHeadRot"] = entity.yHeadRot.toDouble()
        if (s.yBodyRot) item["yBodyRot"] = entity.yBodyRot.toDouble() // yaw in degrees
    }

    if (entity is Player) {
        item["is_player"] = true

        if (s.nickname) item["nickname"] = entity.gameProfile.name
        if (s.experience_level) item["experience_level"] = entity.experienceLevel
        if (s.xp_needed_for_next_level) item["xp_needed_for_next_level"] = entity.xpNeededForNextLevel
        if (s.experience_progress) item["experience_progress"] = entity.experienceProgress
    }
    return item
}

fun entityToMapRadar(entity: Entity, s: SomePeripheralsConfig.Server.RadarSettings.RadarAllowedEntityData): MutableMap<String, Any> {
    val item = mutableMapOf<String, Any>()

    item["is_entity"] = true
    if(s.pos) item["pos"] = listOf(entity.x, entity.y, entity.z)
    if(s.eye_pos) item["eye_pos"] = listOf(entity.eyePosition.x, entity.eyePosition.y, entity.eyePosition.z)
    if(s.eye_height) item["eye_height"] = entity.eyeHeight.toDouble()
    if(s.look_angle) item["look_angle"] = listOf(entity.lookAngle.x, entity.lookAngle.y, entity.lookAngle.z)
    if(s.dimension) item["dimension"] = entity.level.dimension().toString()
    if(s.entity_type) item["entity_type"] = entity.type.descriptionId
    if(s.air_supply) item["air_supply"] = entity.airSupply
    if(s.max_air_supply) item["max_air_supply"] = entity.maxAirSupply

    //TODO main hand? off hand? effects?
    if (entity is LivingEntity) {
        if (s.health) item["health"] = entity.health.toDouble()
        if (s.max_health) item["max_health"] = entity.maxHealth.toDouble()
        if (s.armor_value) item["armor_value"] = entity.armorValue
        if (s.armor_cover_percentage) item["armor_cover_percentage"] = entity.armorCoverPercentage.toDouble()
        if (s.absorption_amount) item["absorption_amount"] = entity.absorptionAmount.toDouble()
        if (s.is_baby) item["is_baby"] = entity.isBaby
        if (s.is_blocking) item["is_blocking"] = entity.isBlocking
        if (s.is_sleeping) item["is_sleeping"] = entity.isSleeping
        if (s.is_fall_flying) item["is_fall_flying"] = entity.isFallFlying
        if (s.speed) item["speed"] = entity.speed.toDouble()
        if (s.xRot) item["xRot"] = entity.xRot.toDouble() // pitch in degrees
        if (s.yHeadRot) item["yHeadRot"] = entity.yHeadRot.toDouble()
        if (s.yBodyRot) item["yBodyRot"] = entity.yBodyRot.toDouble() // yaw in degrees
    }

    if (entity is Player) {
        item["is_player"] = true

        if (s.nickname) item["nickname"] = entity.gameProfile.name
        if (s.experience_level) item["experience_level"] = entity.experienceLevel
        if (s.xp_needed_for_next_level) item["xp_needed_for_next_level"] = entity.xpNeededForNextLevel
        if (s.experience_progress) item["experience_progress"] = entity.experienceProgress
    }
    return item
}