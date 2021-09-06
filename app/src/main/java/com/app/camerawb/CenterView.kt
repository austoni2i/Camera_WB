package com.app.camerawb

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class CenterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : View(context, attrs, defStyle, defStyleRes) {

    private var viewWidth: Float = 0f
    private var viewHeight: Float = 0f
    private var swipeListener : OnSwipeListener? = null
    private var bgFrameColor = "#202025"

    val radius = 195f
    private val circlePaint = Paint()

    private var calibrationMode = false
    var centerX = 0f
    var centerY = 0f



    init {
        circlePaint.color = Color.RED
        val dashPath = DashPathEffect(floatArrayOf(10f, 10f), 1.0.toFloat())
        circlePaint.isAntiAlias = true
        circlePaint.pathEffect = dashPath
        circlePaint.style = Paint.Style.STROKE
        circlePaint.strokeWidth = 5.0f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val bmp = getBitmap()
            it.drawBitmap(bmp, 0f, 0f, null)
        }

        if (calibrationMode) {
            canvas?.drawCircle(
                viewWidth / 2,
                viewHeight / 2,
                radius,
                circlePaint
            )
        }
    }


    private fun getBitmap(): Bitmap {
        val bitmap =
            Bitmap.createBitmap(viewWidth.toInt(), viewHeight.toInt(), Bitmap.Config.ARGB_8888)
        val osCanvas = Canvas(bitmap)


        val rectf = RectF(0f,0f,viewWidth,viewHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor(bgFrameColor)
        //paint.alpha = 120

        osCanvas.drawRect(rectf, paint)

        paint.color = Color.TRANSPARENT
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
        centerX = viewWidth / 2
        centerY = viewHeight / 2

        osCanvas.drawCircle(centerX, centerY, radius, paint)

        return bitmap
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        if (widthMode == MeasureSpec.EXACTLY) {
            viewWidth = widthSize.toFloat()
        }

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (heightMode == MeasureSpec.EXACTLY) {
            viewHeight = heightSize.toFloat()
        }

        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (isCalibrationMode)
            swipeListener?.onSwipe(it)
        }
        return true
    }

    fun setSwipeListener(listener: OnSwipeListener) {
        swipeListener = listener
    }

    var frameColor : String
            get() = bgFrameColor
            set(value) { bgFrameColor = value}

    var isCalibrationMode: Boolean
        get() = calibrationMode
        set(value) {
            if (value){
                frameColor = "#80202025"
            }else{
                frameColor = "#202025"
            }
            calibrationMode = value
            invalidate()
        }

    interface OnSwipeListener {
        fun onSwipe(event: MotionEvent)
    }


}