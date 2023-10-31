package net.spaceeye.someperipherals.forge.integrations.cc

import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.api.peripheral.IPeripheralProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraftforge.common.util.LazyOptional
import net.spaceeye.someperipherals.integrations.cc.getPeripheralCommon

class SomePeripheralsPeripheralProviderForge: IPeripheralProvider {
    override fun getPeripheral(level: Level, blockPos: BlockPos, direction: Direction): LazyOptional<IPeripheral> {
        val peripheral = getPeripheralCommon(level, blockPos, direction) ?: return LazyOptional.empty()
        return LazyOptional.of{peripheral}
    }
}