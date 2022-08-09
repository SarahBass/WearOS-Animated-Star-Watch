package com.academy.testwatch3
//Import Animation ---------------------------------
//--------------------------------------------------
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.util.Log
import android.view.SurfaceHolder
import androidx.palette.graphics.Palette
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 1000

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0
private const val HOUR_STROKE_WIDTH = 12f
private const val MINUTE_STROKE_WIDTH = 10f
private const val SECOND_TICK_STROKE_WIDTH = 5f
private const val CENTER_GAP_AND_CIRCLE_RADIUS = 4f
private const val SHADOW_RADIUS = 7f


/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn"t
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 *
 *
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */



class MyWatchFace : CanvasWatchFaceService() {



    override fun onCreate() {
        Log.i("tag", "onStart Service ${this.application.applicationInfo}")

        super.onCreate()
    }

    override fun onStart(intent: Intent?, startId: Int) {
        Log.i("tag", "onStart Service $intent")

        super.onStart(intent, startId)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("tag", "onStart Service $intent")

//        val CHANNEL_ID = "my_channel_01"
//        val channel = NotificationChannel(
//            CHANNEL_ID,
//            "Channel human readable title",
//            NotificationManager.IMPORTANCE_DEFAULT
//        )
//
//        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
//            channel
//        )
//
//        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
//            .setContentTitle("")
//            .setContentText("").build()
//
//        this.startForeground(1, notification)

        return super.onStartCommand(intent, flags, startId)
    }

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

        private var mSecondHandLength: Float = 0F
        private var sMinuteHandLength: Float = 0F
        private var sHourHandLength: Float = 0F

        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
        private var mWatchHandColor: Int = 0
        private var mWatchHandHighlightColor: Int = 0
        private var mWatchHandShadowColor: Int = 0

        private lateinit var mHourPaint: Paint
        private lateinit var mMinutePaint: Paint
        private lateinit var mSecondPaint: Paint
        private lateinit var mTickAndCirclePaint: Paint


        private lateinit var mForegroundBitmap: Bitmap

        private lateinit var mBackgroundPaint: Paint
        private lateinit var mBackgroundBitmap: Bitmap
        private lateinit var mGrayBackgroundBitmap: Bitmap

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

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setAcceptsTapEvents(true)
                    .build()
            )

            mCalendar = Calendar.getInstance()

//            setContentView(R.layout.rainbowanimate)
//
//            val rocketImage = findViewById<ImageView>(R.id.rainbowstar).apply {
//                setBackgroundResource(R.drawable.rocket_thrust)
//                rocketAnimation = background as AnimationDrawable
//            }
//
//            rocketImage.setOnClickListener({ rainbowanimation.start() })

            initializeBackground()
            initializeWatchFace()
        }

        private lateinit var rocketAnimation: AnimationDrawable

        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }


            val sdf = SimpleDateFormat("EEE")
            val sdf1 = SimpleDateFormat("EEEE")
            val sdf2 = SimpleDateFormat("MMMM")
            val sdf3 = SimpleDateFormat("d")
            val sdf4 = SimpleDateFormat("yyyy")
            val sdf5 = SimpleDateFormat("MMMM d yyyy")
            val d = Date()
            val dayOfTheWeek: String = sdf.format(d)
            val dayOfTheWeekLong: String = sdf1.format(d)
            val monthOfYear: String = sdf2.format(d)
            val dayOfMonth: String = sdf3.format(d)
            val year4digits: String = sdf4.format(d)
            val fullDateSpaces : String = sdf5.format(d)
            val easterArray = arrayOf( "April 9 2023","March 31 2024", "April 20 2025", "April 5 2026", "March 28 2027", "April 16 2028", "April 1 2029", "April 21 2030", "April 13 2031", "March 28 2032" )

            if (monthOfYear == "October") {
                if (dayOfMonth == "31" || dayOfMonth == "30" || dayOfMonth == "1" ) {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.october1)}
                else {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.october2)}}
            else if (monthOfYear == "November") {
                if (dayOfTheWeek == "Mon" || dayOfTheWeek == "Wed" || dayOfTheWeek == "Fri") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.november1)}
                else {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.november2)}}
            else if (monthOfYear == "December") {
                //Christmas & Christmas Eve
                if (dayOfMonth == "25" || dayOfMonth == "24"){
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.december1)}
                //https://www.calendardate.com/hanukkah_2030.htm has dates up to 2030 for Hanukah or use HebrewCalendar (YEAR, 2, 25)
                else if ((Integer.parseInt(year4digits) == 2022 && Integer.parseInt(dayOfMonth) in 18..23 ) ||
                    (Integer.parseInt(year4digits) == 2023 && Integer.parseInt(dayOfMonth) in 7..15 ) ||
                    (Integer.parseInt(year4digits) == 2024 && Integer.parseInt(dayOfMonth) in 26..30 ) ||
                    (Integer.parseInt(year4digits) == 2025 && Integer.parseInt(dayOfMonth) in 14..22 ) ||
                    (Integer.parseInt(year4digits) == 2026 && Integer.parseInt(dayOfMonth) in 4..12 ) ||
                    (Integer.parseInt(year4digits) == 2027 && Integer.parseInt(dayOfMonth) in 26..30 ) ||
                    (Integer.parseInt(year4digits) == 2028 && Integer.parseInt(dayOfMonth) in 12..20 ) ||
                    (Integer.parseInt(year4digits) == 2029 && Integer.parseInt(dayOfMonth) in 1..9 ) ||
                    (Integer.parseInt(year4digits) == 2030 && Integer.parseInt(dayOfMonth) in 20..23 )){
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.jewishholiday)}
                else {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.december2)}}
            else if (monthOfYear == "February") {
                if (Integer.parseInt(dayOfMonth) in 1..15 ){
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.feb14) }}
            else if (monthOfYear == "March") {
                if (Integer.parseInt(dayOfMonth) in 1..18 ){
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.march17)}
                else if (easterArray.contains(fullDateSpaces)) {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.easter)}}
            else if (monthOfYear == "April") {
                if (easterArray.contains(fullDateSpaces)) {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.easter)}
                else {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.springflower)}}
            else if (monthOfYear == "July" || monthOfYear == "August") {
                mBackgroundBitmap =
                    BitmapFactory.decodeResource(resources, R.drawable.summerbeach) }
            else {

                if (dayOfTheWeek == "Mon") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.monday) }
                else if (dayOfTheWeek == "Tue") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.tuesday) }
                else if (dayOfTheWeek == "Wed") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.wednesday) }
                else if (dayOfTheWeek == "Thu") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.icerainbow) }
                else if (dayOfTheWeek == "Fri") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.friday) }
                else if (dayOfTheWeek == "Sat") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.saturday) }
                else if (dayOfTheWeek == "Sun") {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.sunday) }
                else {
                    mBackgroundBitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.icerainbow)}
            }

            /* Extracts colors from background image to improve watchface style. */
            Palette.from(mBackgroundBitmap).generate {
                it?.let {
                    mWatchHandHighlightColor = it.getVibrantColor(Color.WHITE)
                    mWatchHandColor = it.getLightVibrantColor(Color.WHITE)
                    mWatchHandShadowColor = it.getDarkMutedColor(Color.BLACK)
                    updateWatchHandStyle()
                }
            }
        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
            mWatchHandColor = Color.WHITE
            mWatchHandHighlightColor = Color.WHITE
            mWatchHandShadowColor = Color.BLACK

            mHourPaint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = HOUR_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }

            mMinutePaint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = MINUTE_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }

            mSecondPaint = Paint().apply {
                color = mWatchHandHighlightColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
                setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }

            mTickAndCirclePaint = Paint().apply {
                color = mWatchHandColor
                strokeWidth = SECOND_TICK_STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE
                setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }
        }

        override fun onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME)
            super.onDestroy()
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            mLowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            mBurnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
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
                mHourPaint.color = Color.WHITE
                mMinutePaint.color = Color.WHITE
                mSecondPaint.color = Color.WHITE
                mTickAndCirclePaint.color = Color.WHITE

                mHourPaint.isAntiAlias = false
                mMinutePaint.isAntiAlias = false
                mSecondPaint.isAntiAlias = false
                mTickAndCirclePaint.isAntiAlias = false

                mHourPaint.clearShadowLayer()
                mMinutePaint.clearShadowLayer()
                mSecondPaint.clearShadowLayer()
                mTickAndCirclePaint.clearShadowLayer()

            } else {
                mHourPaint.color = mWatchHandColor
                mMinutePaint.color = mWatchHandColor
                mSecondPaint.color = mWatchHandHighlightColor
                mTickAndCirclePaint.color = mWatchHandColor

                mHourPaint.isAntiAlias = true
                mMinutePaint.isAntiAlias = true
                mSecondPaint.isAntiAlias = true
                mTickAndCirclePaint.isAntiAlias = true

                mHourPaint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
                mMinutePaint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
                mSecondPaint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
                mTickAndCirclePaint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, mWatchHandShadowColor
                )
            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode
                mHourPaint.alpha = if (inMuteMode) 100 else 255
                mMinutePaint.alpha = if (inMuteMode) 100 else 255
                mSecondPaint.alpha = if (inMuteMode) 80 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f
            mCenterY = height / 2f

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (mCenterX * 0.6).toFloat()
            sMinuteHandLength = (mCenterX * 0.6).toFloat()
            sHourHandLength = (mCenterX * 0.6).toFloat()

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            val scale = width.toFloat() / mBackgroundBitmap.width.toFloat()


            mBackgroundBitmap = Bitmap.createScaledBitmap(
                mBackgroundBitmap,
                (mBackgroundBitmap.width * scale).toInt(),
                (mBackgroundBitmap.height * scale).toInt(), true
            )

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don"t want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren"t
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap()
            }
        }

        private fun initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                mBackgroundBitmap.width,
                mBackgroundBitmap.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(mGrayBackgroundBitmap)
            val grayPaint = Paint()
            val colorMatrix = ColorMatrix()
            colorMatrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(colorMatrix)
            grayPaint.colorFilter = filter
            canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, grayPaint)
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
                //WatchFaceService.TAP_TYPE_TAP ->
                // The user has completed the tap gesture.
                //Can user open up xml or pages
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
            drawAnimation(canvas, bounds)
        }

        private fun drawAnimation(canvas: Canvas, bounds: Rect) {
            val scale = 1.5
            val dst = Rect(
                (bounds.left / scale).toInt(),
                (bounds.top / scale).toInt(),
                (bounds.right / scale).toInt(),
                (bounds.bottom / scale).toInt())

            canvas.drawBitmap(
                BitmapFactory.decodeResource(applicationContext.resources, R.drawable.rainbow1),
                bounds,
                dst,
                null
            )
        }

        private fun drawBackground(canvas: Canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else if (mAmbient) {
                canvas.drawBitmap(mGrayBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, mBackgroundPaint)
            }
        }

        private fun drawWatchFace(canvas: Canvas) {

            /*
             * Draw ticks. Usually you will want to bake this directly into the photo, but in
             * cases where you want to allow users to select their own photos, this dynamically
             * creates them on top of the photo.
             */
            val innerTickRadius = mCenterX - 10
            val outerTickRadius = mCenterX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
//                canvas.drawLine(
                //                  mCenterX + innerX, mCenterY + innerY,
                //                mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint
                //          )
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            val seconds =
                mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f
            val secondsRotation = seconds * 6f

            val minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f

            val hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = mCalendar.get(Calendar.HOUR) * 30 + hourHandOffset

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save()

            canvas.rotate(hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                mCenterX,
                mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                mCenterX,
                mCenterY - sHourHandLength,
                mHourPaint
            )

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                mCenterX,
                mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                mCenterX,
                mCenterY - sMinuteHandLength,
                mMinutePaint
            )

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY)
                canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - mSecondHandLength,
                    mSecondPaint
                )

            }
            canvas.drawCircle(
                mCenterX,
                mCenterY,
                CENTER_GAP_AND_CIRCLE_RADIUS,
                mTickAndCirclePaint
            )


            /* Restore the canvas" original orientation. */
            canvas.restore()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren"t visible. */
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
