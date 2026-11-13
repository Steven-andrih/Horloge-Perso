package com.horloge.perso

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object ClockRenderer {
    val modeNames = listOf("Accueil + verrouillage", "Verrouillage seulement", "Accueil seulement")
    val styleNames = listOf("Classique", "Empilé", "Contour", "Dégradé", "Analogique")
    val dateFormatNames = listOf("Long", "Court", "Jour seul")
    private val datePatterns = listOf("EEEE d MMMM", "EEE d MMM", "EEEE")

    val textColors = intArrayOf(
        0xFFFFFFFF.toInt(), 0xFFEEEEEE.toInt(), 0xFFFFF3E0.toInt(), 0xFFBDBDBD.toInt(),
        0xFF000000.toInt(), 0xFF7BC74D.toInt(), 0xFFFF5252.toInt(), 0xFFFF8A65.toInt(),
        0xFFFFB74D.toInt(), 0xFFFFD54F.toInt(), 0xFFDCE775.toInt(), 0xFF4DB6AC.toInt(),
        0xFF4DD0E1.toInt(), 0xFF64B5F6.toInt(), 0xFF7986CB.toInt(), 0xFFBA68C8.toInt(),
        0xFFF06292.toInt()
    )
    val bgColors = intArrayOf(
        0xFF000000.toInt(), 0xFF101018.toInt(), 0xFF222831.toInt(), 0xFF393E46.toInt(),
        0xFF37474F.toInt(), 0xFF1B3A4B.toInt(), 0xFF0D47A1.toInt(), 0xFF3E2C6B.toInt(),
        0xFF4A148C.toInt(), 0xFF3B1F2B.toInt(), 0xFFB71C1C.toInt(), 0xFFE65100.toInt(),
        0xFF1E3D2F.toInt(), 0xFF1B5E20.toInt(), 0xFFFFE0B2.toInt(), 0xFFB3E5FC.toInt(),
        0xFFEEEEEE.toInt()
    )

    fun draw(c: Canvas, w: Int, h: Int, p: Prefs, showClock: Boolean = true, isLock: Boolean = true) {
        drawBackground(c, w, h, p, isLock)
        if (!showClock) return

        val now = Date()
        val locale = Locale.getDefault()
        val a = p.opacity.coerceIn(5, 100) * 255 / 100
        val y = h * p.posY / 100f
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = p.textColor
            alpha = a
            textAlign = Paint.Align.CENTER
            typeface = Fonts.typeface(p.ctx, p.fontKey)
            if (p.shadow) setShadowLayer(w * 0.012f, 0f, w * 0.005f, Color.argb(170, 0, 0, 0))
        }
        val dateGap = w * 0.1f

        val dateY = when (p.clockStyle) {
            1 -> drawStacked(c, w, y, p, base, now, locale)
            4 -> drawAnalog(c, w, y, p, base, a, now)
            else -> drawLine(c, w, y, p, base, now, locale)
        } + dateGap

        if (p.showDate) {
            val pattern = datePatterns[p.dateFormat.coerceIn(0, datePatterns.lastIndex)]
            val text = SimpleDateFormat(pattern, locale).format(now).replaceFirstChar { it.titlecase(locale) }
            val dp = Paint(base).apply {
                style = Paint.Style.FILL
                shader = null
                textSize = w * 0.055f
                alpha = a * 85 / 100
            }
            c.drawText(text, w / 2f, dateY, dp)
        }
    }

    private fun drawBackground(c: Canvas, w: Int, h: Int, p: Prefs, isLock: Boolean) {
        val bmp = if (!isLock) (p.homeBackground() ?: p.background()) else p.background()
        if (bmp != null) {
            c.drawColor(Color.BLACK)
            val scale = max(w / bmp.width.toFloat(), h / bmp.height.toFloat())
            val m = Matrix().apply {
                postScale(scale, scale)
                postTranslate((w - bmp.width * scale) / 2f, (h - bmp.height * scale) / 2f)
            }
            c.drawBitmap(bmp, m, Paint(Paint.FILTER_BITMAP_FLAG))
            if (p.dim > 0) c.drawColor(Color.argb(p.dim * 255 / 100, 0, 0, 0))
        } else {
            c.drawColor(p.bgColor)
        }
    }

    private fun timeFmt(p: Prefs, pattern24: String, pattern12: String, locale: Locale, now: Date) =
        SimpleDateFormat(if (p.is24h) pattern24 else pattern12, locale).format(now)

    /** Classique, contour et dégradé : une seule ligne. Retourne la ligne de base. */
    private fun drawLine(c: Canvas, w: Int, y: Float, p: Prefs, base: Paint, now: Date, locale: Locale): Float {
        val ts = w * p.size / 100f
        val paint = Paint(base).apply { textSize = ts }
        when (p.clockStyle) {
            2 -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = max(2f, ts * 0.025f)
                paint.strokeJoin = Paint.Join.ROUND
            }
            3 -> paint.shader = LinearGradient(0f, y - ts * 0.8f, 0f, y, p.textColor, p.color2, Shader.TileMode.CLAMP)
        }
        c.drawText(timeFmt(p, "HH:mm", "h:mm", locale, now), w / 2f, y, paint)
        return y
    }

    /** Heures au-dessus, minutes en dessous. */
    private fun drawStacked(c: Canvas, w: Int, y: Float, p: Prefs, base: Paint, now: Date, locale: Locale): Float {
        val ts = w * p.size / 100f * 1.5f
        val paint = Paint(base).apply { textSize = ts }
        val lineGap = ts * 0.92f
        c.drawText(timeFmt(p, "HH", "h", locale, now), w / 2f, y, paint)
        c.drawText(SimpleDateFormat("mm", locale).format(now), w / 2f, y + lineGap, paint)
        return y + lineGap
    }

    /** Horloge à aiguilles. Retourne le bas du cadran. */
    private fun drawAnalog(c: Canvas, w: Int, cy: Float, p: Prefs, base: Paint, a: Int, now: Date): Float {
        val r = w * (p.size.coerceIn(10, 45) / 45f) * 0.85f / 2f
        val cx = w / 2f

        val face = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.argb(70, 0, 0, 0) }
        c.drawCircle(cx, cy, r, face)

        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            color = p.textColor
            alpha = a
        }
        stroke.strokeWidth = r * 0.03f
        c.drawCircle(cx, cy, r, stroke)

        for (i in 0 until 12) {
            val big = i % 3 == 0
            val len = if (big) r * 0.14f else r * 0.07f
            stroke.strokeWidth = if (big) r * 0.035f else r * 0.02f
            val rad = Math.toRadians(i * 30.0)
            val s = sin(rad).toFloat()
            val co = cos(rad).toFloat()
            c.drawLine(cx + s * r * 0.88f, cy - co * r * 0.88f, cx + s * (r * 0.88f - len), cy - co * (r * 0.88f - len), stroke)
        }

        val cal = Calendar.getInstance().apply { time = now }
        val hour = (cal.get(Calendar.HOUR) + cal.get(Calendar.MINUTE) / 60f) * 30.0
        val min = cal.get(Calendar.MINUTE) * 6.0

        fun hand(deg: Double, len: Float, width: Float, color: Int) {
            stroke.color = color
            stroke.alpha = a
            stroke.strokeWidth = width
            val rad = Math.toRadians(deg)
            c.drawLine(cx, cy, cx + sin(rad).toFloat() * len, cy - cos(rad).toFloat() * len, stroke)
        }
        hand(hour, r * 0.5f, r * 0.07f, p.textColor)
        hand(min, r * 0.75f, r * 0.05f, p.color2)

        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = p.color2; alpha = a }
        c.drawCircle(cx, cy, r * 0.06f, dot)
        return cy + r
    }
}
