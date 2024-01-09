package net.spaceeye.someperipherals.stuff

import net.minecraft.util.Mth
import net.spaceeye.someperipherals.stuff.utils.linspace
import java.lang.Math.pow
import kotlin.math.*

object BallisticFunctions {
    @JvmStatic
    inline fun rad(deg:Double) = deg * 0.017453292519943295

    @JvmStatic
    inline fun flinspace(start: Double, stop: Double, num_elements:Int, min: Double, max: Double) = linspace(max(start, min), min(stop, max), num_elements)

    @JvmStatic
    fun getRoot(data: ArrayList<Array<Double>>, from_end:Boolean): Array<Double> {
        if (from_end) {
            for (i in data.size-2 downTo  0) { if (data[i][0] > data[i+1][0]) {return data[i+1]} }
            return data[0]
        } else {
            for (i in 1 until data.size) { if (data[i-1][0] < data[i][0]) {return data[i-1]} }
            return data.last()
        }
    }

    @JvmStatic
    fun timeInAir(y_projectile:Double,
                  y_target:Double,
                  projectile_y_velocity:Double,
                  gravity: Double = 0.05,
                  drag: Double = 0.99,
                  max_steps: Int = 1000000): Pair<Int, Int> {
        var t = 0
        var t_below = Int.MAX_VALUE
        var y0 = y_projectile; var Vy = projectile_y_velocity

        if (y0 < y_target) {
            var y0_p: Double
            while (t < max_steps) {
                y0_p = y0
                y0 += Vy
                Vy = drag * Vy - gravity

                t += 1

                if (y0 > y_target) { t_below = t - 1; break }
                if (y0 - y0_p < 0) { return Pair(-1, -1) }
            }
        }

        while (t < max_steps) {
            y0 += Vy
            Vy = drag * Vy - gravity

            t+=1

            if (y0 <= y_target) { return Pair(t_below, t) }
        }

        return Pair(t_below, -1)
    }

    @JvmStatic
    fun tryPitch(
        pitch_to_try: Double,
        initial_speed: Double,
        length: Double,
        distance: Double,
        cannon: Array<Double>,
        target: Array<Double>,
        gravity: Double = 0.05,
        drag: Double = 0.99,
        max_steps: Int = 1000000
    ): Array<Double>? {
        val tp_rad = rad(pitch_to_try)

        val Vw = cos(tp_rad) * initial_speed
        val Vy = sin(tp_rad) * initial_speed

        val x_coord_2d = length * cos(tp_rad)

        if (Vw == 0.0) { return null }
        val part = 1 - (distance - x_coord_2d) / (100 * Vw)
        if (part <= 0.0) { return null }
        val horizontal_time_to_target = abs(ln(part) / ln(drag))

        val y_coord_end_of_barrel = cannon[1] + sin(tp_rad) * length;

        val (t_below, t_above) = timeInAir(y_coord_end_of_barrel, target[1], Vy, gravity, drag, max_steps)

        if (t_below < 0) { return null }

        val delta_t = min(
            abs(horizontal_time_to_target-t_below),
            abs(horizontal_time_to_target-t_above)
        )
        return arrayOf(
            delta_t,
            pitch_to_try,
            (delta_t + horizontal_time_to_target)
        )
    }

    @JvmStatic
    fun tryPitches(
        iter: Iterable<Double>,
        initial_speed: Double,
        length: Double,
        distance: Double,
        cannon: Array<Double>,
        target: Array<Double>,
        gravity: Double = 0.05,
        drag: Double = 0.99,
        max_steps: Int = 1000000
    ): ArrayList<Array<Double>> {
        val delta_times = ArrayList<Array<Double>>()
        for (item in iter) {
            val items = tryPitch(item, initial_speed, length, distance, cannon, target, gravity, drag, max_steps) ?: continue
            delta_times.add(items)
        }
        return delta_times
    }

    @JvmStatic
    fun calculatePitch(
        cannon: Array<Double>,
        target: Array<Double>,
        initial_speed: Double,
        length: Double,
        amin: Int = -30,
        amax: Int = 60,
        gravity: Double = 0.05,
        drag: Double = 0.99,
        max_delta_t_error: Double = 1.0,
        max_steps: Int = 1000000,
        num_iterations: Int = 5,
        num_elements: Int = 20,
        check_impossible: Boolean = true
    ): Pair<Array<Double>, Array<Double>> {
        val Dx = cannon[0] - target[0]
        val Dz = cannon[2] - target[2]
        val distance = sqrt(Dx * Dx + Dz * Dz)

        val delta_times = tryPitches((amax downTo amin).map { it.toDouble() }, initial_speed, length, distance, cannon, target, gravity, drag, max_steps)
        if (delta_times.size == 0) {return Pair(arrayOf(-1.0, -1.0, -1.0), arrayOf(-1.0, -1.0, -1.0))}

        var (dT1, p1, at1) = getRoot(delta_times, false)
        var (dT2, p2, at2) = getRoot(delta_times, true)

        var c1 = true
        var c2 = p1 != p2
        val same_res = p1 == p2

        var dTs1 = ArrayList<Array<Double>>()
        var dTs2 = ArrayList<Array<Double>>()

        for (i in 0 until num_iterations) {
            if (c1) {dTs1 = tryPitches(flinspace(p1-pow(10.0, (-i).toDouble()), p1+pow(10.0, (-i).toDouble()), num_elements, amin.toDouble(), amax.toDouble()), initial_speed, length, distance, cannon, target, gravity, drag, max_steps) }
            if (c2) {dTs2 = tryPitches(flinspace(p2-pow(10.0, (-i).toDouble()), p2+pow(10.0, (-i).toDouble()), num_elements, amin.toDouble(), amax.toDouble()), initial_speed, length, distance, cannon, target, gravity, drag, max_steps) }

            if (c1 && dTs1.size == 0) {c1 = false}
            if (c2 && dTs2.size == 0) {c2 = false}

            if (!c1 && !c2) {return Pair(arrayOf(-1.0, -1.0, -1.0), arrayOf(-1.0, -1.0, -1.0))}

            if (c1) {val t = dTs1.minBy{it[0]}; dT1=t[0]; p1=t[1]; at1=t[2];}
            if (c2) {val t = dTs2.minBy{it[0]}; dT2=t[0]; p2=t[1]; at2=t[2];}
        }

        if (same_res) {dT2 = dT1; p2 = p1; at2 = at1;}

        var r1 = arrayOf(dT1, p1, at1); var r2 = arrayOf(dT2, p2, at2)
        if (check_impossible && dT1 > max_delta_t_error) {r1 = arrayOf(-1.0, -1.0, -1.0)}
        if (check_impossible && dT2 > max_delta_t_error) {r2 = arrayOf(-1.0, -1.0, -1.0)}

        return Pair(r1, r2)
    }

    //https://github.com/Cannoneers-of-Create/CreateBigCannons/blob/5a26459a1a65e65cc6c60cf8efaea96ed3b7fabc/common/src/main/java/rbasamoyai/createbigcannons/munitions/AbstractCannonProjectile.java#L378
    //https://github.com/Cannoneers-of-Create/CreateBigCannons/tree/1.18.2/dev/common/src/main/resources/data/minecraft/dimension_munition_properties
    @JvmStatic
    fun getDrag(base_drag: Double, dimensional_drag_multiplier: Double): Double {
        if (dimensional_drag_multiplier <= 1) { return Mth.lerp(dimensional_drag_multiplier, 1.0, base_drag) }
        val diff = base_drag - 1
        return Mth.clamp(base_drag + diff * (dimensional_drag_multiplier - 1), 0.9, base_drag)
    }
}