package net.spaceeye.someperipherals.stuff.digitizer

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.Block
import java.util.stream.Collectors
import java.util.stream.Stream


object DataHelpers {
    fun <T> getTags(`object`: Holder.Reference<T>): Map<String, Boolean> {
        return getTags(`object`.tags())
    }

    fun <T> getTags(tags: Stream<TagKey<T>>): Map<String, Boolean> {
        return tags.collect(
            Collectors.toMap(
                { x: TagKey<T> ->
                    x.location().toString()
                },
                { x: TagKey<T>? -> true })
        )
    }

    fun getId(block: Block?): String? {
        val id = Registry.BLOCK.getKey(block)
        return if (id == null) null else id.toString()
    }

    fun getId(item: Item?): String? {
        val id = Registry.ITEM.getKey(item)
        return if (id == null) null else id.toString()
    }

    fun getId(enchantment: Enchantment?): String? {
        val id = Registry.ENCHANTMENT.getKey(enchantment)
        return id?.toString()
    }
}