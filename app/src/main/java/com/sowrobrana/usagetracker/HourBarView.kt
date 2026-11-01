package com.sowrobrana.usagetracker

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/** Simple 24-hour bar chart of foreground minutes. */
class HourBarView @JvmOverloads constructor(
    ctx: Context, attrs: AttributeSet? = null
) : View(ctx, attrs) {

    var hours: LongArray = LongArray(24)
        set(v) {
            field = v
            invalidate()
        }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.bar)
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.bar_axis)
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(ctx, R.color.bar_axis)
        textSize = 22f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val max = (hours.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val slotW = w / 24f
        val barW = slotW * 0.62f
        val labelH = 30f
        val plotH = h - labelH - 8f
        // axis
        canvas.drawLine(0f, h - labelH, w, h - labelH, axisPaint)
        for (i in 0 until 24) {
            val v = hours[i].toFloat() / max
            val barH = v * plotH
            val left = i * slotW + (slotW - barW) / 2f
            val top = (h - labelH) - barH
            canvas.drawRect(left, top, left + barW, h - labelH, barPaint)
            if (i % 6 == 0) {
                canvas.drawText(Bangla.num(i), i * slotW + slotW / 2f, h - 6f, textPaint)
            }
        }
    }
}
