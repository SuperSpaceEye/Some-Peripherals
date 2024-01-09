package net.spaceeye.someperipherals.stuff.digitizer

import com.google.gson.JsonParseException
import dan200.computercraft.shared.util.NBTUtil
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.world.item.EnchantedBookItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import java.util.*
import java.util.stream.Collectors


//TODO remove later when mod updates to like 1.20 or smth
object ItemData {
    fun <T : MutableMap<in String, Any>> fillBasicSafe(data: T, stack: ItemStack): T {
        data.put("name", DataHelpers.getId(stack.item)?: "noname")
        data.put("count", stack.count)
        return data
    }

    fun <T : MutableMap<in String, Any>> fillBasic(data: T, stack: ItemStack): T {
        fillBasicSafe(data, stack)
        val hash = NBTUtil.getNBTHash(stack.tag)
        if (hash != null) data.put("nbt", hash)
        return data
    }

    fun <T : MutableMap<in String, Any>> fill(data: T, stack: ItemStack): T {
        if (stack.isEmpty) { return data }

        fillBasic(data, stack)

        data.put("displayName", stack.hoverName.string)
        data.put("maxCount", stack.maxStackSize)
        if (stack.isDamageableItem) {
            data.put("damage", stack.damageValue)
            data.put("maxDamage", stack.maxDamage)
        }
        if (stack.item.isBarVisible(stack)) {
            data.put("durability", stack.item.getBarWidth(stack) / 13.0)
        }

        data.put("tags", DataHelpers.getTags(stack.tags))

        val tag = stack.tag
        if (tag != null && tag.contains("display", Tag.TAG_COMPOUND.toInt())) {
            val displayTag = tag.getCompound("display")
            if (displayTag.contains("Lore", Tag.TAG_LIST.toInt())) {
                val loreTag = displayTag.getList("Lore", Tag.TAG_STRING.toInt())
                data.put("lore", loreTag.stream()
                    .map { obj: Tag -> parseTextComponent(obj) }
                    .filter { obj: Component? ->
                        Objects.nonNull(
                            obj
                        )
                    }
                    .map { obj: Component? -> obj!!.string }
                    .collect(Collectors.toList()))
            }
        }

        val hideFlags = tag?.getInt("HideFlags") ?: 0
        val enchants = getAllEnchants(stack, hideFlags)
        if (!enchants.isEmpty()) data.put("enchantments", enchants)
        if (tag != null && tag.getBoolean("Unbreakable") && hideFlags and 4 == 0) {
            data.put("unbreakable", true)
        }
        return data
    }

    private fun parseTextComponent(x: Tag): Component? {
        return try {
            Component.Serializer.fromJson(x.asString)
        } catch (e: JsonParseException) {
            null
        }
    }

    private fun getAllEnchants(stack: ItemStack, hideFlags: Int): List<Map<String, Any>> {
        val enchants = ArrayList<Map<String, Any>>(0)
        if (stack.item is EnchantedBookItem && hideFlags and 32 == 0) {
            addEnchantments(EnchantedBookItem.getEnchantments(stack), enchants)
        }
        if (stack.isEnchanted && hideFlags and 1 == 0) {
            /*
             * Mimic the EnchantmentHelper.getEnchantments(ItemStack stack) behavior without special case for Enchanted book.
             * I'll do that to have the same data than ones displayed in tooltip.
             * @see EnchantmentHelper.getEnchantments(ItemStack stack)
             */
            addEnchantments(stack.enchantmentTags, enchants)
        }
        return enchants
    }

    private fun addEnchantments(rawEnchants: ListTag, enchants: ArrayList<Map<String, Any>>) {
        if (rawEnchants.isEmpty()) return
        enchants.ensureCapacity(enchants.size + rawEnchants.size)

        for ((enchantment, level) in EnchantmentHelper.deserializeEnchantments(rawEnchants)) {
            val enchant = HashMap<String, Any>(3)
            enchant["name"] = DataHelpers.getId(enchantment) ?: "noname"
            enchant["level"] = level
            enchant["displayName"] = enchantment.getFullname(level).string
            enchants.add(enchant)
        }
    }
}