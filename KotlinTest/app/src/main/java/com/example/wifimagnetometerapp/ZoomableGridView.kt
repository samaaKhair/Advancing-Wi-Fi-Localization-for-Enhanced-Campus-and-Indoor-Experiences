package com.example.wifimagnetometerapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View

class ZoomableGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var scaleFactor = 1.0f
    private val scaleGestureDetector: ScaleGestureDetector
    private val gridPaint = Paint()
    private val textPaint = Paint()
    private val pathPaint = Paint()
    private val userLocationPaint = Paint()
    private val visitedPaint = Paint()

    private var path: List<List<Int>> = emptyList()
    private var userLocation: Coordinate? = null
    private var visitedLocations: Array<BooleanArray>? = null
    private var cellClickListener: ((Int, Int) -> Unit)? = null

    init {
        gridPaint.color = Color.GRAY
        gridPaint.style = Paint.Style.STROKE
        textPaint.color = Color.WHITE
        textPaint.textSize = 30f
        pathPaint.color = Color.RED
        pathPaint.style = Paint.Style.FILL
        userLocationPaint.color = Color.BLUE
        userLocationPaint.style = Paint.Style.FILL
        visitedPaint.color = Color.GREEN

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scaleFactor, scaleFactor)

        val cellSize = 100
        val gridSize = 16

        for (i in 1..gridSize) {
            for (j in 1..gridSize) {
                val left = (i - 1) * cellSize.toFloat()
                val top = (j - 1) * cellSize.toFloat()
                val right = left + cellSize
                val bottom = top + cellSize

                if (visitedLocations != null && visitedLocations!![i - 1][j - 1]) {
                    canvas.drawRect(left, top, right, bottom, visitedPaint)
                } else {
                    canvas.drawRect(left, top, right, bottom, gridPaint)
                }

                if (scaleFactor > 1.5f) {
                    canvas.drawText("$i,$j", left + 10, top + 40, textPaint)
                }
            }
        }

        path.forEach {
            val row = it[0]
            val col = it[1]
            val left = (row - 1) * cellSize.toFloat()
            val top = (col - 1) * cellSize.toFloat()
            val right = left + cellSize
            val bottom = top + cellSize
            canvas.drawRect(left, top, right, bottom, pathPaint)
        }

        userLocation?.let {
            Log.d("ZoomableGridView", "Drawing user location at (${it.row}, ${it.col})")
            val left = (it.row - 1) * cellSize.toFloat()
            val top = (it.col - 1) * cellSize.toFloat()
            val centerX = left + cellSize / 2
            val centerY = top + cellSize / 2
            val radius = (cellSize / 4).toFloat()
            canvas.drawCircle(centerX, centerY, radius, userLocationPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        if (event.action == MotionEvent.ACTION_UP) {
            val cellSize = 100
            val gridSize = 16
            val x = (event.x / scaleFactor).toInt() / cellSize + 1
            val y = (event.y / scaleFactor).toInt() / cellSize + 1
            if (x in 1..gridSize && y in 1..gridSize) {
                cellClickListener?.invoke(x, y)
            }
        }
        return true
    }

    fun setPath(path: List<List<Int>>) {
        this.path = path
        invalidate()
    }

    fun setUserLocation(userLocation: Coordinate?) {
        Log.d("ZoomableGridView", "Setting user location to (${userLocation?.row}, ${userLocation?.col})")
        this.userLocation = userLocation
        invalidate()
    }

    fun setVisitedLocations(visitedLocations: Array<BooleanArray>?) {
        this.visitedLocations = visitedLocations
        invalidate()
    }

    fun setOnCellClickListener(listener: (Int, Int) -> Unit) {
        this.cellClickListener = listener
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            scaleFactor *= scaleGestureDetector.scaleFactor
            scaleFactor = Math.max(1.0f, Math.min(scaleFactor, 5.0f))
            invalidate()
            return true
        }
    }
}
