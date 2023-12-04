package net.spaceeye.someperipherals.integrations.cc.peripherals

import dan200.computercraft.api.lua.IArguments
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
        val instance = DigitalItemsSavedData.getInstance(level)

        val uuid = UUID.randomUUID()
        instance.setItem(DigitizedItem(uuid,
                be.inventory.extractItem(0, amount, false).copy()
            )
        )

        item.count = item.count - amount
        if (item.count != 0) {
            be.inventory.setStackInSlot(0, item)
        } else {
            be.inventory.setStackInSlot(0, ItemStack.EMPTY)
        }
        instance.setDirty()

        return uuid.toString()
    }

    @LuaFunction(mainThread = true)
    fun rematerializeAmount(uuid: String, amount: Int): Any {
        val uuid = try {UUID.fromString(uuid)} catch (_ : Exception) {return makeErrorReturn("First argument is not a UUID")}
        if (amount <= 0) { return makeErrorReturn("Invalid amount") }
        if (!idExists(uuid)) { return makeErrorReturn("UUID doesn't exist") }

        val instance = DigitalItemsSavedData.getInstance(level)

        val item = instance.getItem(uuid)!!

        val amount = min(amount, item.item.count)

        val limitedAmount = item.item.copy()
        limitedAmount.count = amount

        val remaining = be.inventory.insertItemInSlot(0, limitedAmount, true)
        if (remaining.count != 0) { return makeErrorReturn("Failed to merge item inside digitizer with already rematerialized items") }

        be.inventory.insertItemInSlot(0, limitedAmount, false)
        item.item.count = item.item.count - amount

        if (item.item.count == 0) { instance.removeItem(uuid) }
        instance.setDirty()

        return true
    }

    private fun canItemsStack(a: ItemStack, b: ItemStack): Boolean {
        if (a.isEmpty || b.isEmpty || !a.sameItem(b) || !a.isStackable) return false
        return if (a.hasTag() != b.hasTag()) false else (!a.hasTag() || a.tag == b.tag)
    }

    @LuaFunction(mainThread = true)
    fun mergeDigitalItems(args: IArguments): Any {
        val into = try {UUID.fromString(args.getString(0))} catch (_ : Exception) {return makeErrorReturn("First argument is not a UUID")}
        val from = try {UUID.fromString(args.getString(1))} catch (_ : Exception) {return makeErrorReturn("Second argument is not a UUID")}
        var amount = args.optInt(2).orElse(Int.MAX_VALUE)

        if (amount <= 0) {return makeErrorReturn("Invalid amount")}
        if (into == from) {return makeErrorReturn("Items have same id")}

        if (!idExists(into)) {return makeErrorReturn("First id does not exists")}
        if (!idExists(from)) {return makeErrorReturn("Second id does not exists")}

        val instance = DigitalItemsSavedData.getInstance(level)

        val intoItem = instance.getItem(into)!!
        val fromItem = instance.getItem(from)!!

        if (!canItemsStack(intoItem.item, fromItem.item)) {return makeErrorReturn("Can't stack items")}

        if (amount > fromItem.item.count) {amount = fromItem.item.count}

        intoItem.item.count += amount
        fromItem.item.count -= amount

        if (fromItem.item.count == 0) {instance.removeItem(from)}
        instance.setDirty()

        return true
    }

    @LuaFunction(mainThread = true)
    fun separateItem(from: String, amount: Int): Any {
        val from = try {UUID.fromString(from)} catch (_ : Exception) {return makeErrorReturn("First argument is not a UUID")}

        if (amount <= 0) {return makeErrorReturn("Invalid amount")}
        if (!idExists(from)) {return makeErrorReturn("id does not exists")}

        val instance = DigitalItemsSavedData.getInstance(level)

        val fromItem = instance.getItem(from)!!

        if (!fromItem.item.isStackable) {return makeErrorReturn("Item is not stackable")}
        if (fromItem.item.count <= amount) {return makeErrorReturn("Cannot separate stack")}

        val copy = fromItem.item.copy()

        copy.count = amount
        fromItem.item.count -= amount

        val newUUID = UUID.randomUUID()
        val newItem = instance.setItem(DigitizedItem(newUUID, copy))

        instance.setDirty()

        return newItem.id.toString()
    }

    @LuaFunction(mainThread = true)
    fun checkID(uuid: String): Any {
        val uuid = try {UUID.fromString(uuid)} catch (_ : Exception) {return makeErrorReturn("First argument is not a UUID")}
        if (!idExists(uuid)) { return makeErrorReturn("UUID doesn't exist") }

        val item = DigitalItemsSavedData.getInstance(level).getItem(uuid)!!

        val ret = HashMap<String, Any>()
        ret["item"] = ItemData.fill(mutableMapOf(), item.item)
        return ret
    }

    @LuaFunction(mainThread = true)
    fun getItemInSlot(): Map<String, Any> = ItemData.fill(mutableMapOf(), be.inventory.getStackInSlot(0))

    @LuaFunction(mainThread = true)
    fun getItemLimitInSlot(): Int = be.inventory.getSlotLimit(0)

    override fun equals(p0: IPeripheral?): Boolean = level.getBlockState(pos).`is`(SomePeripheralsCommonBlocks.DIGITIZER.get())
    override fun getType(): String = "digitizer"
}