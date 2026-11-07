package io.github.cq61tech.konumaldatici

import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Bağlanıyor animasyonundaki şekiller (Soft Burst, Cookie 9, Pentagon, Pill, Sunny, Cookie 4, Oval).
 * Her şekil açıya göre yarıçap dizisi olarak tutulur, böylece iki şekil arasında doğrudan dönüşüm yapılır.
 * Resmî MaterialShapes değildir; onlara benzer şekillerdir.
 */
object ShapeBank {
    const val N = 180

    private val cosT = FloatArray(N) { cos(2.0 * PI * it / N).toFloat() }
    private val sinT = FloatArray(N) { sin(2.0 * PI * it / N).toFloat() }

    private fun mod(x: Double, m: Double) = ((x % m) + m) % m

    private fun build(f: (Double) -> Double): FloatArray {
        val a = FloatArray(N) { i -> f(2.0 * PI * i / N).toFloat() }
        val m = a.max()
        return FloatArray(N) { a[it] / m }
    }

    val circle = FloatArray(N) { 1f }

    val softBurst = build { t -> 0.70 + 0.30 * (0.5 + 0.5 * cos(10 * t)).pow(1.6) }

    val cookie9 = build { t -> 0.84 + 0.16 * cos(9 * t) }

    val pentagon = build { t ->
        val seg = 2.0 * PI / 5
        val phi = mod(t + PI / 2, seg) - seg / 2
        val r = cos(seg / 2) / cos(phi)
        0.6 * r + 0.4 * 0.92
    }

    val pill = build { t ->
        val p = 5.0
        val b = 0.60
        (abs(cos(t)).pow(p) + abs(sin(t)).pow(p) / b.pow(p)).pow(-1.0 / p)
    }

    val sunny = build { t -> 0.74 + 0.26 * abs(cos(4 * t)).pow(0.6) }

    val cookie4 = build { t -> 0.86 + 0.14 * cos(4 * t) }

    val oval = build { t ->
        val u = t - 0.61
        val a = 1.0
        val b = 0.74
        a * b / sqrt((b * cos(u)).pow(2) + (a * sin(u)).pow(2))
    }

    val sequence: List<FloatArray> = listOf(softBurst, cookie9, pentagon, pill, sunny, cookie4, oval)

    fun mix(a: FloatArray, b: FloatArray, f: Float, out: FloatArray) {
        for (i in 0 until N) out[i] = a[i] + (b[i] - a[i]) * f
    }

    fun copy(src: FloatArray, out: FloatArray) {
        for (i in 0 until N) out[i] = src[i]
    }

    fun path(radii: FloatArray, cx: Float, cy: Float, scale: Float, into: Path): Path {
        into.reset()
        for (i in 0 until N) {
            val r = radii[i] * scale
            val x = cx + r * cosT[i]
            val y = cy + r * sinT[i]
            if (i == 0) into.moveTo(x, y) else into.lineTo(x, y)
        }
        into.close()
        return into
    }
}
