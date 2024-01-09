package net.spaceeye.someperipherals.blockentities

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.spaceeye.someperipherals.stuff.linkPort.GlobalLinkConnections
import net.spaceeye.someperipherals.stuff.linkPort.LinkConnectionsManager
import net.spaceeye.someperipherals.stuff.utils.Constants
import java.util.*

class GoggleLinkPortBlockEntity(pos: BlockPos, state: BlockState): BlockEntity(CommonBlockEntities.GOOGLE_LINK_PORT.get(), pos, state) {
    var this_manager_key: UUID? = null
    var connection: LinkConnectionsManager? = null

    init {
        load(CompoundTag())
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        if (this_manager_key == null) { load(tag) }
        tag.putUUID(Constants.LINK_UUID_NAME, this_manager_key!!)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        if (!tag.contains(Constants.LINK_UUID_NAME)) {
            tag.putUUID(Constants.LINK_UUID_NAME, UUID.randomUUID())
        }
        this_manager_key = tag.getUUID(Constants.LINK_UUID_NAME)

        connection = LinkConnectionsManager()
        GlobalLinkConnections.links[this_manager_key] = connection
    }
}