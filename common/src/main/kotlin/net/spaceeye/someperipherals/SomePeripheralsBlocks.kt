package net.spaceeye.someperipherals

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item


object SomePeripheralsBlocks {
    private val BLOCKS = DeferredRegister.create(SomePeripherals.MOD_ID, Registry.BLOCK_REGISTRY)

    fun register() {
        SomePeripheralsCommonBlocks.registerBaseBlocks()
        BLOCKS.register()
    }

    fun registerItems(items: DeferredRegister<Item>) {
        BLOCKS.forEach {
            items.register(it.id) { BlockItem(it.get(), Item.Properties().tab(SomePeripheralsItems.TAB)) }
        }
    }
}