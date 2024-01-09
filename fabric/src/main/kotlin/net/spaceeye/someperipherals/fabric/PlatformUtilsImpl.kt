package net.spaceeye.someperipherals.fabric

import dan200.computercraft.api.peripheral.IPeripheralProvider
import io.github.fabricators_of_create.porting_lib.transfer.item.SlotItemHandler
import net.minecraft.world.inventory.Slot
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity
import net.spaceeye.someperipherals.config.AbstractConfigBuilder
import net.spaceeye.someperipherals.fabric.integrations.cc.SomePeripheralsPeripheralProviderFabric
import net.spaceeye.someperipherals.stuff.utils.CommonBlockEntityInventory

object PlatformUtilsImpl {
    @JvmStatic
    fun getPeripheralProvider(): IPeripheralProvider = SomePeripheralsPeripheralProviderFabric()

    @JvmStatic
    fun getConfigBuilder(): AbstractConfigBuilder = FabricConfigBuilder()

    @JvmStatic
    fun makeCommonBlockEntityInventory(size: Int): CommonBlockEntityInventory = FabricBlockEntityInventory(size)

    @JvmStatic
    fun setDigitizerStuff(be: BlockEntity, addSlot: (slot: Slot) -> Slot) {
        addSlot(SlotItemHandler(
            ((be as DigitizerBlockEntity)
                .inventory as FabricBlockEntityInventory).inventory, 0, 80, 35)
        )
    }
}