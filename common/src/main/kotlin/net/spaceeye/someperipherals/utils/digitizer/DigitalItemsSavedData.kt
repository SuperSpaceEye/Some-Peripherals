package net.spaceeye.someperipherals.utils.digitizer

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import net.spaceeye.someperipherals.SomePeripherals
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DigitalItemsSavedData: SavedData() {
    private var digitizedItems = ConcurrentHashMap<UUID, DigitizedItem>()

    override fun save(tag: CompoundTag): CompoundTag {
        val items = ListTag()
        digitizedItems.forEach{_, v -> items.add(v.serialize(CompoundTag())) }
        tag.put("items", items)
        return tag
    }

    fun getItem(uuid: UUID): DigitizedItem? {
        val item = digitizedItems[uuid] ?: return null
        //TODO some other logic idk
        return item
    }

    fun setItem(item: DigitizedItem) { digitizedItems[item.id] = item }

    fun removeItem(uuid: UUID) { digitizedItems.remove(uuid) }

    companion object {
        private var instance: DigitalItemsSavedData? = null

        fun getInstance(level: Level): DigitalItemsSavedData {
            if (instance != null) {return instance!!}
            level as ServerLevel
            instance = level.server.overworld().dataStorage.computeIfAbsent(::load, ::create, SomePeripherals.MOD_ID)
            return instance!!
        }

        fun create(): DigitalItemsSavedData {return DigitalItemsSavedData()}
        fun load(tag: CompoundTag): DigitalItemsSavedData {
            val data = create()

            if (tag.contains("items") && tag["items"] is ListTag) {
                (tag["items"] as ListTag).forEach { itag ->
                    val di = DigitizedItem(itag as CompoundTag)
                    data.digitizedItems[di.id] = di
                }
            }

            return data
        }
     }
}