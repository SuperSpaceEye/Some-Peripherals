package net.spaceeye.someperipherals.stuff.utils

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.spaceeye.someperipherals.mixin.IServerLevelAccessor
import java.util.concurrent.TimeoutException

fun <T> getEntitiesWithTimeout(
    level: ServerLevel,
    area: AABB,
    timeout_ms: Long,
    fn: (entity: Entity) -> T): MutableList<T> {
    val entities = ArrayList<T>()

    val now = getNowFast_ms()
    try {
        (level as IServerLevelAccessor).entitiesAcc.get(area) {
            if (getNowFast_ms() - now > timeout_ms) { throw TimeoutException() }
            entities.add(fn(it))
        }
    } catch (_: TimeoutException) {
    } catch (_: ConcurrentModificationException) {}
    // ConcurrentModificationException is needed as if getEntitiesWithTimeout is called not from main thread, it can
    // cause error

    return entities
}