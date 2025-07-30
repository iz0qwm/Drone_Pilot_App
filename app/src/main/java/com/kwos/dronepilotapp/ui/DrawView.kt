package com.kwos.dronepilotapp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class DrawView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = 0xFFFF0000.toInt()  // Rosso
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private var startX: Float? = null
    private var startY: Float? = null
    private var endX: Float? = null
    private var endY: Float? = null

    fun setPoints(x1: Float, y1: Float, x2: Float, y2: Float) {
        startX = x1
        startY = y1
        endX = x2
        endY = y2
        invalidate()  // Rinfresca la vista per ridisegnare
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (startX != null && endX != null) {
            canvas.drawLine(startX!!, startY!!, endX!!, endY!!, paint)
        }
    }
}
