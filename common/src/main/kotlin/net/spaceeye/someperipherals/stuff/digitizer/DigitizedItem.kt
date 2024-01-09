package net.spaceeye.someperipherals.stuff.digitizer

import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import java.util.*

class DigitizedItem(@JvmField var id: UUID, @JvmField var item: ItemStack) {
    constructor(tag: CompoundTag):
            this(
                tag.getUUID("uuid"),
                ItemStack.of(tag.getCompound("itemStack"))
            )

    fun serialize(tag: CompoundTag): CompoundTag {
        tag.putUUID("uuid", id)
        tag.put("itemStack", item.save(CompoundTag()))
        return tag
    }
}