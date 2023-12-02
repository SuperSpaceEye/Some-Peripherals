package net.spaceeye.someperipherals

import dev.architectury.registry.registries.DeferredRegister
import net.minecraft.core.Registry
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.spaceeye.someperipherals.utils.digitizer.DigitizerMenu

object SomePeripheralsMenu {
    private val MENU = DeferredRegister.create(SomePeripherals.MOD_ID, Registry.MENU_REGISTRY)

    @JvmField val DIGITIZER_MENU = MENU.register("digitizer") { MenuType {i: Int, inv: Inventory -> DigitizerMenu(i, inv)} }

    fun register() {
        MENU.register()
    }
}