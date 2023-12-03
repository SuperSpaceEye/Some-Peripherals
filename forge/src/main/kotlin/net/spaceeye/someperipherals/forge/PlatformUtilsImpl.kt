package net.spaceeye.someperipherals.forge

import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.config.AbstractConfigBuilder
import net.spaceeye.someperipherals.forge.integrations.cc.SomePeripheralsPeripheralProviderForge
import net.spaceeye.someperipherals.utils.mix.CommonBlockEntityInventory
import net.spaceeye.someperipherals.utils.mix.CommonNetworkHooks
import net.minecraftforge.items.SlotItemHandler
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = SomePeripheralsPeripheralProviderForge()

    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = ForgeConfigBuilder()

    @JvmStatic
    fun makeCommonBlockEntityInventory(size: Int): CommonBlockEntityInventory = ForgeBlockEntityInventory(size)

    @JvmStatic
    fun setDigitizerStuff(be: BlockEntity, addSlot: (slot: Slot) -> Slot) {
        addSlot(SlotItemHandler(
            ((be as DigitizerBlockEntity)
                .inventory as ForgeBlockEntityInventory).inventory, 0, 80, 35))
    }
}