package net.spaceeye.someperipherals

import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.items.goggles.RangeGogglesItem
import net.spaceeye.someperipherals.items.goggles.StatusGogglesItem

object SomePeripheralsItems {
    val ITEMS = DeferredRegister.create(SomePeripherals.MOD_ID, Registry.ITEM_REGISTRY)
    val TAB: CreativeModeTab = CreativeTabRegistry.create(
        ResourceLocation(
            SomePeripherals.MOD_ID,
            "someperipherals_tab"
        )
    ) {ItemStack(LOGO.get())}

    var LOGO: RegistrySupplier<Item> = ITEMS.register("someperipherals_logo") { Item(Item.Properties()) }

    var STATUS_GOGGLES: RegistrySupplier<Item> = ITEMS.register("status_goggles") { StatusGogglesItem() }
    var RANGE_GOGGLES: RegistrySupplier<Item> = ITEMS.register("range_goggles") { RangeGogglesItem() }

    fun register() {
        SomePeripheralsBlocks.registerItems(ITEMS)
        SomePeripheralsCommonBlocks.registerItems(ITEMS)
        ITEMS.register()
    }
}