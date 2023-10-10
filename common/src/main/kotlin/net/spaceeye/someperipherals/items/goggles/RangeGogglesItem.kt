package net.spaceeye.someperipherals.items.goggles

import net.minecraft.world.entity.Entity
import net.spaceeye.someperipherals.LinkPortUtils.LinkUpdate
import net.spaceeye.someperipherals.LinkPortUtils.Server_RangeGogglesPhysUpdate

class RangeGogglesItem: StatusGogglesItem() {
    override val base_name: String = "item.some_peripherals.tootlip.range_goggles"
    override val linked_name: String = "text.some_peripherals.linked_range_goggles"
    override fun makeConnectionUpdate(entity: Entity): LinkUpdate {
        return Server_RangeGogglesPhysUpdate(entity)
    }
}