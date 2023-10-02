package net.spaceeye.someperipherals.raycasting

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


class CachedObject(bpos: BlockPos, val state: BlockState) {
    var used = 1
    val x = bpos.x
    val y = bpos.y
    val z = bpos.z
}

class PosCache {
    private var data = HashMap<Int, HashMap<Int, HashMap<Int, CachedObject>>>()
    private var cached = mutableListOf<CachedObject>()
    private val mutex: Lock = ReentrantLock(true)

    private fun getItem(bpos: BlockPos): CachedObject? {
        val r1 = data[bpos.x] ?: return null
        val r2 = r1  [bpos.y] ?: return null
        return r2[bpos.z]
    }

    private fun setItem(x: Int, y: Int, z: Int, item: CachedObject) {
        var xm = data[x]
        if (xm == null) {xm = HashMap(); data[x] = xm}
        var ym = xm[y]
        if (ym == null) {ym = HashMap(); xm[y] = ym}
        ym[z] = item
    }

    private fun removeItem(x: Int, y: Int, z: Int) {
        val xm = data[x]!!
        val ym = xm[y]!!
        ym.remove(z)

        if (!ym.isEmpty()) {return}
        xm.remove(y)
    }

    private fun cacheNew(bpos: BlockPos, state: BlockState, max_items: Int) {
        val newCache = CachedObject(bpos, state)
        if (cached.size < max_items) {
            cached.add(newCache)
        } else {
            var min_num = Integer.MAX_VALUE
            var min_i = 0
            for ((i, e) in cached.withIndex()) {
                if (e.used < min_num) {
                    min_i = i
                    min_num = e.used
                }
                if (min_num <= 1) {break}
            }
            val to_replace = cached[min_i]
            removeItem(to_replace.x, to_replace.y, to_replace.z)
            data.get(to_replace.x)?.get(to_replace.y)?.remove(to_replace.z)
            cached[min_i] = newCache
        }
        setItem(bpos.x, bpos.y, bpos.z, newCache)
    }

    fun clear() {
        mutex.lock()
        data.clear()
        cached.clear()
        mutex.unlock()
    }

    fun getBlockState(level: Level, bpos: BlockPos, max_items: Int = 1000, do_cache: Boolean = true): BlockState {
        if (!do_cache) {return level.getBlockState(bpos)}

        mutex.lock()
        val item = getItem(bpos)
        if (item != null) {
            item.used += 1
            val state = item.state
            mutex.unlock()
            return state
        }

        val new_state = level.getBlockState(bpos)
        cacheNew(bpos, new_state, max_items)
        mutex.unlock()
        return new_state
    }

}