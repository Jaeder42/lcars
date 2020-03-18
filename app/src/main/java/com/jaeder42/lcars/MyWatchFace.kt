package com.jaeder42.lcars

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder

import java.lang.ref.WeakReference
import java.util.Calendar
import java.util.TimeZone


private const val INTERACTIVE_UPDATE_RATE_MS = 1000

private const val MSG_UPDATE_TIME = 0
private const val SECOND_TICK_STROKE_WIDTH = 1f

class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    private class EngineHandler(reference: MyWatchFace.Engine) : Handler() {
        private val mWeakReference: WeakReference<MyWatchFace.Engine> = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            val engine = mWeakReference.get()
            if (engine != null) {
                when (msg.what) {
                    MSG_UPDATE_TIME -> engine.handleUpdateTimeMessage()
                }
            }
        }
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var mCalendar: Calendar

        private var mRegisteredTimeZoneReceiver = false
        private var mMuteMode: Boolean = false
        private var mCenterX: Float = 0F
        private var mCenterY: Float = 0F

        private lateinit var mSquarePaint: Paint
        private lateinit var mTimePaint: Paint
        private lateinit var mDatePaint: Paint

        private var mWidth: Float = 0f
        private var mHeight: Float = 0f

        private lateinit var mBackgroundPaint: Paint

        private var purpleColor = Color.parseColor("#9e9fca")
        private var redColor = Color.parseColor("#cc5f60")
        private var orangeColor = Color.parseColor("#fd9800")
        private var yellowColor = Color.parseColor("#f6cb92")


        private var mAmbient: Boolean = false
        private var mLowBitAmbient: Boolean = false
        private var mBurnInProtection: Boolean = false

        /* Handler to update the time once a second in interactive mode. */
        private val mUpdateTimeHandler = EngineHandler(this)

        private val mTimeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build())

            mCalendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }


        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */

            mSquarePaint = Paint().apply {
                color = purpleColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
            }
            mTimePaint = Paint().apply {
                color = yellowColor
                textSize = 70f
                strokeWidth= 4f
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
                textAlign= Paint.Align.CENTER

            }
            mDatePaint = Paint().apply {
                color = yellowColor
                textSize = 40f
                strokeWidth= 4f
                isAntiAlias = true
                style = Paint.Style.FILL_AND_STROKE
                textAlign= Paint.Align.LEFT

            }

        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                    WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false)
            mBurnInProtection = properties.getBoolean(
                    WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false)
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            mAmbient = inAmbientMode

            updateWatchHandStyle()

            // Check and trigger whether or not timer should be running (only
            // in active mode).
            updateTimer()
        }

        private fun updateWatchHandStyle() {
            if (mAmbient) {
                mSquarePaint.color = Color.WHITE
                mSquarePaint.isAntiAlias = false
                mSquarePaint.clearShadowLayer()

            } else {

                mSquarePaint.color =purpleColor
                mSquarePaint.isAntiAlias = true

            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            mWidth = width.toFloat()
            mHeight = height.toFloat()
            mCenterX = width / 2f
            mCenterY = height / 2f

        }


        /**
         * Captures tap event (and tap type). The [WatchFaceService.TAP_TYPE_TAP] case can be
         * used for implementing specific logic to handle the gesture.
         */
        override fun onTapCommand(tapType: Int, x: Int, y: Int, eventTime: Long) {
            when (tapType) {
                WatchFaceService.TAP_TYPE_TOUCH -> {
                    // The user has started touching the screen.
                }
                WatchFaceService.TAP_TYPE_TOUCH_CANCEL -> {
                    // The user has started a different gesture or otherwise cancelled the tap.
                }
                WatchFaceService.TAP_TYPE_TAP -> { }
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.

            }
            invalidate()
        }


        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            canvas.drawColor(Color.BLACK)
        }


        private fun drawLeftTop(canvas: Canvas, start: Float) {
            var width = 40f
            var totalHeight = mHeight/3 -10


            if(!mAmbient) {
                mSquarePaint.color = redColor
            }
            canvas.drawArc(0f, start , width * 2, start +
                    totalHeight -10 , 180f, 90f, true, mSquarePaint )
            if(!mAmbient) {
                mSquarePaint.color = purpleColor
            }
             canvas.drawRect(0f, start+totalHeight/2 , width, start+totalHeight/2+10, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = redColor
            }
            canvas.drawArc(0f, mHeight/3 - totalHeight + 10, width * 2, mHeight/3 , 180f, -90f, true, mSquarePaint )

        }

        private fun drawLeftBottom(canvas: Canvas, start: Float) {

             var leftMidHeight:Float = 100f
            var offset = ((mHeight /1.5).toFloat() - leftMidHeight)/2
            var bigHeight = 40f
            var smallHeight = 10f
            var width = 40f

            if(!mAmbient) {
                mSquarePaint.color = redColor
            }
            canvas.drawArc(0f, start + 10, width * 2, start+ (2*offset) -20 , 180f, 90f, true, mSquarePaint )
            if(!mAmbient) {
                mSquarePaint.color = orangeColor
            }
            canvas.drawRect(0f, start+offset , width, start+offset+bigHeight, mSquarePaint)
            canvas.drawRect(0f,start+offset+bigHeight+5, width, start+offset+bigHeight+smallHeight, mSquarePaint)
            canvas.drawRect(0f, start+offset+bigHeight+smallHeight+5, width, start+offset+bigHeight+smallHeight+bigHeight, mSquarePaint)
            canvas.drawRect(0f,start+offset+bigHeight+smallHeight+bigHeight +5, width, start+offset+bigHeight+smallHeight+bigHeight+smallHeight, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = redColor

            }
            canvas.drawArc(0f, mHeight-(2*offset) + 20, width * 2, mHeight -10 , 180f, -90f, true, mSquarePaint )

        }


        private fun drawHorizontal(canvas: Canvas, start: Float) {
            var height = 10f;
            if(!mAmbient) {
                mSquarePaint.color = redColor
            }
            canvas.drawRect(40f, start , 60f, start+height, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = purpleColor
            }
            canvas.drawRect(65f, start , 100f, start+height, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = orangeColor
            }
            canvas.drawRect(105f, start , 180f, start+height, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = redColor
            }
            canvas.drawRect(185f, start , 205f, start+height, mSquarePaint)
            if(!mAmbient) {
                mSquarePaint.color = purpleColor
            }
            canvas.drawRect(210f, start , mWidth, start+height, mSquarePaint)
        }

        private fun dateTimeNumber(input: Int): String{
            if(input < 10) {
                return input.toString().padStart(2, '0')
            }
            else return input.toString()
        }

        private fun drawWatchFace(canvas: Canvas) {
            val seconds =
                    mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f


            mSquarePaint.style = Paint.Style.FILL_AND_STROKE
            // Top
            drawLeftTop(canvas, 0f)
            drawHorizontal(canvas,  0f)
            drawHorizontal(canvas, mHeight/3 -10)
            // Bottom
            drawLeftBottom(canvas, mHeight /3)
            drawHorizontal(canvas, mHeight/3 + 10)
            drawHorizontal(canvas, mHeight- 20)

            //Time
            canvas.drawText("${dateTimeNumber(mCalendar.get(Calendar.HOUR_OF_DAY))}.${dateTimeNumber(mCalendar.get(Calendar.MINUTE))}",  mWidth/2 + 40, mHeight/1.5f + 20, mTimePaint)

            //Date
            canvas.drawText("${dateTimeNumber(mCalendar.get(Calendar.YEAR))}.${dateTimeNumber(mCalendar.get(Calendar.MONTH) + 1)}.${dateTimeNumber(mCalendar.get(Calendar.DATE))}",  50f, mHeight/3 - 35, mDatePaint )

        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer()
        }

        private fun registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(mTimeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return
            }
            mRegisteredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(mTimeZoneReceiver)
        }

        /**
         * Starts/stops the [.mUpdateTimeHandler] timer based on the state of the watch face.
         */
        private fun updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME)
            }
        }

        /**
         * Returns whether the [.mUpdateTimeHandler] timer should be running. The timer
         * should only run in active mode.
         */
        private fun shouldTimerBeRunning(): Boolean {
            return isVisible && !mAmbient
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        fun handleUpdateTimeMessage() {
            invalidate()
            if (shouldTimerBeRunning()) {
                val timeMs = System.currentTimeMillis()
                val delayMs = INTERACTIVE_UPDATE_RATE_MS - timeMs % INTERACTIVE_UPDATE_RATE_MS
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs)
            }
        }
    }
}


