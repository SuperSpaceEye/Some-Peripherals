package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.spaceeye.someperipherals.blockentities.DigitizerBlockEntity
import net.spaceeye.someperipherals.blocks.SomePeripheralsCommonBlocks
import net.spaceeye.someperipherals.integrations.cc.makeErrorReturn
import net.spaceeye.someperipherals.utils.digitizer.DigitalItemsSavedData
import net.spaceeye.someperipherals.utils.digitizer.DigitizedItem
import net.spaceeye.someperipherals.utils.digitizer.ItemData
import java.util.*
import kotlin.math.min

class DigitizerPeripheral(private val level: Level, private val pos: BlockPos, be: BlockEntity): IPeripheral {
    private val be = be as DigitizerBlockEntity

    private inline fun idExists(uuid: UUID): Boolean =
        DigitalItemsSavedData.getInstance(level).getItem(uuid) != null

    @LuaFunction(mainThread = true)
    fun digitizeAmount(amount: Int): Any {
        if (amount <= 0) { return makeErrorReturn("Invalid amount") }
        val item = be.inventory.getStackInSlot(0)
        if (item.count == 0) { return makeErrorReturn("Empty slot") }

        val amount = min(amount, item.count)

        val uuid = UUID.randomUUID()
        DigitalItemsSavedData.getInstance(level).setItem(
            DigitizedItem(
                uuid,
                be.inventory.extractItem(0, amount, false).copy()
            )
        )

        item.count = item.count - amount
        if (item.count != 0) {
            be.inventory.setStackInSlot(0, item)
        } else {
            be.inventory.setStackInSlot(0, ItemStack.EMPTY)
        }
        DigitalItemsSavedData.getInstance(level).setDirty()

        return uuid.toString()
    }

    @LuaFunction(mainThread = true)
    fun rematerializeAmount(uuid: String, amount: Int): Any {
        val uuid = UUID.fromString(uuid)
        if (amount <= 0) { return makeErrorReturn("Invalid amount") }
        if (!idExists(uuid)) { return makeErrorReturn("UUID doesn't exist") }

        val item = DigitalItemsSavedData.getInstance(level).getItem(uuid)!!

        val amount = min(amount, item.item.count)

        val limitedAmount = item.item.copy()
        limitedAmount.count = amount

        val remaining = be.inventory.insertItemInSlot(0, limitedAmount, true)
        if (remaining.count != 0) { return makeErrorReturn("Failed to merge item inside digitizer with already rematerialized items") }

        be.inventory.insertItemInSlot(0, limitedAmount, false)
        item.item.count = item.item.count - amount

        if (item.item.count == 0) { DigitalItemsSavedData.getInstance(level).removeItem(uuid) }
        DigitalItemsSavedData.getInstance(level).setDirty()

        return true
    }

    @LuaFunction(mainThread = true)
    fun checkID(uuid: String): Any {
        val uuid = UUID.fromString(uuid)
        if (!idExists(uuid)) { return makeErrorReturn("UUID doesn't exist") }

        val item = DigitalItemsSavedData.getInstance(level).getItem(uuid)!!

        val ret = HashMap<String, Any>()
        ret.put("item", ItemData.fill(mutableMapOf(), item.item))
        return ret
    }

    @LuaFunction(mainThread = true)
    fun getItemInSlot(): Map<String, Any> = ItemData.fill(mutableMapOf(), be.inventory.getStackInSlot(0))

    @LuaFunction(mainThread = true)
    fun getItemLimitInSlot(): Int = be.inventory.getSlotLimit(0)

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.DIGITIZER.get())
    override fun getType(): String = "digitizer"
}