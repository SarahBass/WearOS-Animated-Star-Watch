package com.academy.testwatch3

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
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
private const val INTERACTIVE_UPDATE_RATE_MS = 600

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

            initializeBackground()
            initializeWatchFace()
        }

        private fun getHolidayBackground(): Bitmap {
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
            val fullDateSpaces: String = sdf5.format(d)
            val easterArray = arrayOf(
                "April 9 2023",
                "March 31 2024",
                "April 20 2025",
                "April 5 2026",
                "March 28 2027",
                "April 16 2028",
                "April 1 2029",
                "April 21 2030",
                "April 13 2031",
                "March 28 2032"
            )

            val backgroundBitmap: Bitmap =
                if (monthOfYear == "October") {
                    if (dayOfMonth == "31" || dayOfMonth == "30" || dayOfMonth == "1") {
                        BitmapFactory.decodeResource(resources, R.drawable.october1)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.october2)
                    }
                } else if (monthOfYear == "November") {
                    if (dayOfTheWeek == "Mon" || dayOfTheWeek == "Wed" || dayOfTheWeek == "Fri") {
                        BitmapFactory.decodeResource(resources, R.drawable.november1)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.november2)
                    }
                } else if (monthOfYear == "December") {
                    //Christmas & Christmas Eve
                    if (dayOfMonth == "25" || dayOfMonth == "24") {
                        BitmapFactory.decodeResource(resources, R.drawable.december1)
                    }
                    //https://www.calendardate.com/hanukkah_2030.htm has dates up to 2030 for Hanukah or use HebrewCalendar (YEAR, 2, 25)
                    else if ((Integer.parseInt(year4digits) == 2022 && Integer.parseInt(dayOfMonth) in 18..23) ||
                        (Integer.parseInt(year4digits) == 2023 && Integer.parseInt(dayOfMonth) in 7..15) ||
                        (Integer.parseInt(year4digits) == 2024 && Integer.parseInt(dayOfMonth) in 26..30) ||
                        (Integer.parseInt(year4digits) == 2025 && Integer.parseInt(dayOfMonth) in 14..22) ||
                        (Integer.parseInt(year4digits) == 2026 && Integer.parseInt(dayOfMonth) in 4..12) ||
                        (Integer.parseInt(year4digits) == 2027 && Integer.parseInt(dayOfMonth) in 26..30) ||
                        (Integer.parseInt(year4digits) == 2028 && Integer.parseInt(dayOfMonth) in 12..20) ||
                        (Integer.parseInt(year4digits) == 2029 && Integer.parseInt(dayOfMonth) in 1..9) ||
                        (Integer.parseInt(year4digits) == 2030 && Integer.parseInt(dayOfMonth) in 20..23)
                    ) {
                        BitmapFactory.decodeResource(resources, R.drawable.jewishholiday)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.december2)
                    }}
                    else if (monthOfYear == "January") {
                    if (Integer.parseInt(dayOfMonth) in 1..15) {
                        BitmapFactory.decodeResource(resources, R.drawable.icerainbow)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.tuesday)}
                    }else if (monthOfYear == "February") {
                    if (Integer.parseInt(dayOfMonth) in 1..15) {
                        BitmapFactory.decodeResource(resources, R.drawable.feb14)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.springflower)
                    }
                } else if (monthOfYear == "March") {
                    if (Integer.parseInt(dayOfMonth) in 1..18) {
                        BitmapFactory.decodeResource(resources, R.drawable.march17)
                    } else if (easterArray.contains(fullDateSpaces)) {
                        BitmapFactory.decodeResource(resources, R.drawable.easter)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.springflower)
                    }
                } else if (monthOfYear == "April") {
                    if (easterArray.contains(fullDateSpaces)) {
                        BitmapFactory.decodeResource(resources, R.drawable.easter)
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.springflower)
                    }
                } else if (monthOfYear == "July" || monthOfYear == "August") {
                    BitmapFactory.decodeResource(resources, R.drawable.summerbeach)
                } else {
                    BitmapFactory.decodeResource(
                        resources,
                        when (dayOfTheWeek) {
                            "Mon" -> R.drawable.monday
                            "Tue" -> R.drawable.tuesday
                            "Wed" -> R.drawable.wednesday
                            "Thu" -> R.drawable.thursday
                            "Fri" -> R.drawable.friday
                            "Sat" -> R.drawable.saturday
                            "Sun" -> R.drawable.sunday
                            else -> R.drawable.icerainbow
                        }
                    )
                }

            return backgroundBitmap
        }


   private fun getAnimationCase(): String {


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
            val fullDateSpaces: String = sdf5.format(d)
            val easterArray = arrayOf(
                "April 9 2023",
                "March 31 2024",
                "April 20 2025",
                "April 5 2026",
                "March 28 2027",
                "April 16 2028",
                "April 1 2029",
                "April 21 2030",
                "April 13 2031",
                "March 28 2032"
            )

            val caseString =
                if (monthOfYear == "October") {
                    if (dayOfMonth == "31" || dayOfMonth == "30" || dayOfMonth == "1") {"Halloween"
                    } else {"October"}
                } else if (monthOfYear == "November") {
                    if (dayOfTheWeek == "Mon" || dayOfTheWeek == "Wed" || dayOfTheWeek == "Fri") {
                        "Thanksgiving"
                    } else {"Fall"}
                } else if (monthOfYear == "December") {
                    //Christmas & Christmas Eve
                    if (dayOfMonth == "25" || dayOfMonth == "24") {"Christmas"
                    }
                    //https://www.calendardate.com/hanukkah_2030.htm has dates up to 2030 for Hanukah or use HebrewCalendar (YEAR, 2, 25)
                    else if ((Integer.parseInt(year4digits) == 2022 && Integer.parseInt(dayOfMonth) in 18..23) ||
                        (Integer.parseInt(year4digits) == 2023 && Integer.parseInt(dayOfMonth) in 7..15) ||
                        (Integer.parseInt(year4digits) == 2024 && Integer.parseInt(dayOfMonth) in 26..30) ||
                        (Integer.parseInt(year4digits) == 2025 && Integer.parseInt(dayOfMonth) in 14..22) ||
                        (Integer.parseInt(year4digits) == 2026 && Integer.parseInt(dayOfMonth) in 4..12) ||
                        (Integer.parseInt(year4digits) == 2027 && Integer.parseInt(dayOfMonth) in 26..30) ||
                        (Integer.parseInt(year4digits) == 2028 && Integer.parseInt(dayOfMonth) in 12..20) ||
                        (Integer.parseInt(year4digits) == 2029 && Integer.parseInt(dayOfMonth) in 1..9) ||
                        (Integer.parseInt(year4digits) == 2030 && Integer.parseInt(dayOfMonth) in 20..23)
                    ) {
                        "Jewish"
                    } else {
                        "Winter"
                    }
                }else if (monthOfYear == "January") {
                    if (Integer.parseInt(dayOfMonth) in 1..15) {
                        "IceRainbow"
                    } else {
                        "IceRainbow"}}
                else if (monthOfYear == "February") {
                    if (Integer.parseInt(dayOfMonth) in 1..15) {
                        "Valentine"
                    } else {
                        "Spring"
                    }
                } else if (monthOfYear == "March") {
                    if (Integer.parseInt(dayOfMonth) in 1..18) {
                       "Irish"
                    } else if (easterArray.contains(fullDateSpaces)) {
                        "Easter"
                    } else {
                       "Spring"
                    }
                } else if (monthOfYear == "April") {
                    if (easterArray.contains(fullDateSpaces)) {
                        "Easter"
                    } else {
                       "Spring"
                    }
                } else if (monthOfYear == "July" || monthOfYear == "August") {
                    "Summer"
                } else {
                        when (dayOfTheWeek) {
                            "Mon" -> "Monday"
                            "Tue" -> "Tuesday"
                            "Wed" -> "Wednesday"
                            "Thu" -> "Thursday"
                            "Fri" -> "Friday"
                            "Sat" -> "Saturday"
                            "Sun" -> "Sunday"
                            else -> "RainbowIce"
                        }

                }

            return caseString
        }




        private fun initializeBackground() {
            mBackgroundPaint = Paint().apply {
                color = Color.BLACK
            }

            mBackgroundBitmap = getHolidayBackground()

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
            //canvas.drawBitmap(mBackgroundBitmap, 0f, 0f, grayPaint)
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
                (bounds.bottom / scale).toInt()
            )

            val frameTime = INTERACTIVE_UPDATE_RATE_MS

            val starsCount = 2
            val timeTimeSwitch = 20000



            val drawable = when (getAnimationCase()) {
                "IceRainbow" -> when ((mCalendar.timeInMillis % (28 * frameTime)) / frameTime) {
                    0L -> R.drawable.rainbow1
                    1L -> R.drawable.rainbow2
                    2L -> R.drawable.rainbow1
                    3L -> R.drawable.rainbow2
                    4L -> R.drawable.rainbow3
                    5L -> R.drawable.rainbow4
                    6L -> R.drawable.rainbow3
                    7L -> R.drawable.rainbow4
                    8L -> R.drawable.rainbow5
                    9L -> R.drawable.rainbow6
                    10L -> R.drawable.rainbow5
                    11L -> R.drawable.rainbow6
                    12L -> R.drawable.rainbow1
                    13L -> R.drawable.rainbow2
                    14L -> R.drawable.rainbow3
                    15L -> R.drawable.rainbow2
                    16L -> R.drawable.rainbow1
                    17L -> R.drawable.rainbow2
                    18L -> R.drawable.rainbow3
                    19L -> R.drawable.rainbow4
                    20L -> R.drawable.rainbow5
                    21L -> R.drawable.rainbow6
                    22L -> R.drawable.snowman0
                    23L -> R.drawable.snowman1
                    24L -> R.drawable.snowman2
                    25L -> R.drawable.snowman0
                    26L -> R.drawable.snowman1
                    27L -> R.drawable.snowman2
                    else -> R.drawable.rainbow1
                }

                "Winter" -> when ((mCalendar.timeInMillis % (40 * frameTime)) / frameTime) {
                    0L -> R.drawable.reindeer0
                    1L -> R.drawable.reindeer1
                    2L -> R.drawable.reindeer0
                    3L -> R.drawable.reindeer1
                    4L -> R.drawable.reindeer0
                    5L -> R.drawable.reindeer1
                    6L -> R.drawable.snowman0
                    7L -> R.drawable.snowman1
                    8L -> R.drawable.snowman2
                    9L -> R.drawable.snowman0
                    10L -> R.drawable.snowman1
                    11L -> R.drawable.snowman2
                    12L -> R.drawable.mint6
                    13L -> R.drawable.mint5
                    14L -> R.drawable.mint6
                    15L -> R.drawable.mint5
                    16L -> R.drawable.mint0
                    17L -> R.drawable.mint1
                    18L -> R.drawable.mint2
                    19L -> R.drawable.mint3
                    20L -> R.drawable.mint4
                    21L -> R.drawable.mint5
                    22L -> R.drawable.mint6
                    23L -> R.drawable.mint5
                    24L -> R.drawable.mint6
                    25L -> R.drawable.peppermint0
                    26L -> R.drawable.peppermint1
                    27L -> R.drawable.peppermint2
                    28L -> R.drawable.peppermint0
                    29L -> R.drawable.peppermint1
                    30L -> R.drawable.peppermint2
                    31L -> R.drawable.peppermint0
                    32L -> R.drawable.peppermint1
                    33L -> R.drawable.peppermint2
                    34L -> R.drawable.reindeer0
                    35L -> R.drawable.reindeer1
                    36L -> R.drawable.reindeer0
                    37L -> R.drawable.reindeer0
                    38L -> R.drawable.reindeer1
                    39L -> R.drawable.reindeer0
                    else -> R.drawable.peppermint0
                }
                "Christmas" -> when ((mCalendar.timeInMillis % (40 * frameTime)) / frameTime) {
                0L -> R.drawable.reindeer0
                1L -> R.drawable.reindeer1
                2L -> R.drawable.reindeer0
                3L -> R.drawable.reindeer1
                4L -> R.drawable.reindeer0
                5L -> R.drawable.reindeer1
                6L -> R.drawable.cookiesanta0
                7L -> R.drawable.cookiesanta1
                8L -> R.drawable.cookiesanta0
                9L -> R.drawable.cookiesanta1
                11L -> R.drawable.cookiesanta0
                12L -> R.drawable.cookiesanta1
                13L -> R.drawable.cookiesanta0
                14L -> R.drawable.cookiesanta1
                15L -> R.drawable.cookiesanta0
                16L -> R.drawable.cookiesanta1
                17L -> R.drawable.ginger0
                18L -> R.drawable.ginger1
                19L -> R.drawable.ginger0
                20L -> R.drawable.ginger1
                21L -> R.drawable.ginger0
                22L -> R.drawable.ginger1
                23L -> R.drawable.ginger0
                24L -> R.drawable.ginger1
                25L -> R.drawable.peppermint0
                26L -> R.drawable.peppermint1
                27L -> R.drawable.peppermint2
                28L -> R.drawable.peppermint0
                29L -> R.drawable.peppermint1
                30L -> R.drawable.peppermint2
                31L -> R.drawable.peppermint0
                32L -> R.drawable.peppermint1
                33L -> R.drawable.peppermint2
                34L -> R.drawable.reindeer0
                35L -> R.drawable.reindeer1
                36L -> R.drawable.reindeer0
                37L -> R.drawable.reindeer0
                38L -> R.drawable.reindeer1
                39L -> R.drawable.reindeer0
                else -> R.drawable.peppermint0
            }


            "Spring" -> when ((mCalendar.timeInMillis % (18 * frameTime)) / frameTime) {
                    0L -> R.drawable.seed0
                    1L -> R.drawable.seed1
                    2L -> R.drawable.seed2
                    3L -> R.drawable.seed0
                    4L -> R.drawable.seed1
                    5L -> R.drawable.seed2
                    6L -> R.drawable.seed0
                    7L -> R.drawable.seedjump1
                    8L -> R.drawable.seed0
                    9L -> R.drawable.seedjump1
                    10L -> R.drawable.seed0
                    11L -> R.drawable.seedwave1
                    12L -> R.drawable.seedwave2
                    13L -> R.drawable.seedwave3
                    14L -> R.drawable.seed0
                    15L -> R.drawable.seedwave1
                    16L -> R.drawable.seedwave2
                    17L -> R.drawable.seedwave3
                    else -> R.drawable.seed0
                }

                "Valentine" -> when ((mCalendar.timeInMillis % (15 * frameTime)) / frameTime) {
                    0L -> R.drawable.heart0
                    1L -> R.drawable.heart1
                    2L -> R.drawable.heart0
                    3L -> R.drawable.heart1
                    4L -> R.drawable.heart0
                    5L -> R.drawable.heartsleep0
                    6L -> R.drawable.heartsleep1
                    7L -> R.drawable.heartsleep0
                    8L -> R.drawable.heartsleep1
                    9L -> R.drawable.heartsleep0
                    10L -> R.drawable.heartsleep1
                    11L -> R.drawable.heart0
                    12L -> R.drawable.heartkiss
                    13L -> R.drawable.heart0
                    14L -> R.drawable.heartkiss
                    else -> R.drawable.heart0
                }
                "Easter" -> when ((mCalendar.timeInMillis % (8 * frameTime)) / frameTime) {
                    0L -> R.drawable.bunnyblue0
                    1L -> R.drawable.bunnyblue1
                    2L -> R.drawable.bunnyblue0
                    3L -> R.drawable.bunnyblue1
                    4L -> R.drawable.bunnybluepeep1
                    5L -> R.drawable.bunnybluepeep2
                    6L -> R.drawable.bunnybluepeep1
                    7L -> R.drawable.bunnybluepeep2
                    else -> R.drawable.bunnyblue0
                }

                "Birthday" -> when ((mCalendar.timeInMillis % (3 * frameTime)) / frameTime) {
                    0L -> R.drawable.birthday0
                    1L -> R.drawable.birthday1
                    2L -> R.drawable.birthday2
                    else -> R.drawable.birthday2
                }

                "Thanksgiving" -> when ((mCalendar.timeInMillis % (2 * frameTime)) / frameTime) {
                    0L -> R.drawable.turkey0
                    1L -> R.drawable.turkey1
                    else -> R.drawable.turkey0
                }

                "Halloween" -> when ((mCalendar.timeInMillis % (20 * frameTime)) / frameTime) {
                    0L -> R.drawable.witch0
                    1L -> R.drawable.witch1
                    2L -> R.drawable.witch0
                    3L -> R.drawable.witch1
                    4L -> R.drawable.witch0
                    5L -> R.drawable.witch1
                    6L -> R.drawable.witch0
                    7L -> R.drawable.witch1
                    8L -> R.drawable.pumpkin0
                    9L -> R.drawable.pumpkin1
                    10L -> R.drawable.pumpkin2
                    11L -> R.drawable.pumpkin0
                    12L -> R.drawable.pumpkin1
                    13L -> R.drawable.pumpkin2
                    14L -> R.drawable.pumpkin0
                    15L -> R.drawable.pumpkin1
                    16L -> R.drawable.pumpkin2
                    17L -> R.drawable.pumpkin0
                    18L -> R.drawable.pumpkin1
                    19L -> R.drawable.pumpkin2

                    else -> R.drawable.witch0
                }


                "October" -> when ((mCalendar.timeInMillis % (36 * frameTime)) / frameTime) {
                    0L -> R.drawable.bat1
                    1L -> R.drawable.bat2
                    2L -> R.drawable.bat1
                    3L -> R.drawable.bat2
                    4L -> R.drawable.bat1
                    5L -> R.drawable.batpumpkin0
                    6L -> R.drawable.batpumpkin1
                    7L -> R.drawable.batpumpkin0
                    8L -> R.drawable.batpumpkin1
                    9L -> R.drawable.bat1
                    10L -> R.drawable.bat2
                    11L -> R.drawable.bat1
                    12L -> R.drawable.bat2
                    13L -> R.drawable.witchcookie0
                    14L -> R.drawable.witchcookie1
                    15L -> R.drawable.witchcookie2
                    16L -> R.drawable.witchcookie1
                    17L -> R.drawable.witchcookie0
                    18L -> R.drawable.candle0
                    19L -> R.drawable.candle2
                    20L -> R.drawable.candle0
                    21L -> R.drawable.candle2
                    22L -> R.drawable.candle3
                    23L -> R.drawable.candle4
                    24L -> R.drawable.candle3
                    25L -> R.drawable.candle4
                    26L -> R.drawable.candle4
                    27L -> R.drawable.candle1
                    28L -> R.drawable.candle4
                    29L -> R.drawable.candle1
                    30L -> R.drawable.candle0
                    31L -> R.drawable.candle1
                    32L -> R.drawable.candle2
                    33L -> R.drawable.candle3
                    34L -> R.drawable.candle4
                    35L -> R.drawable.candle1
                    else -> R.drawable.bat1}


                "Jewish" -> when ((mCalendar.timeInMillis % (25 * frameTime)) / frameTime) {
                    0L -> R.drawable.jewstar0
                    1L -> R.drawable.jewstar1
                    2L -> R.drawable.jewstar2
                    3L -> R.drawable.jewstar1
                    4L -> R.drawable.jewstar0
                    5L -> R.drawable.jewstar0
                    6L -> R.drawable.jewstar1
                    7L -> R.drawable.jewstar2
                    8L -> R.drawable.jewstar1
                    9L -> R.drawable.jewstar2
                    10L -> R.drawable.jewstar0
                    11L -> R.drawable.jewishcandle0
                    12L -> R.drawable.jewishcandle1
                    13L -> R.drawable.jewishcandle0
                    14L -> R.drawable.jewishcandle1
                    15L -> R.drawable.jewishcandle0
                    16L -> R.drawable.jewishcandle1
                    17L -> R.drawable.candle3
                    18L -> R.drawable.candle4
                    19L -> R.drawable.candle3
                    20L -> R.drawable.candle4
                    21L -> R.drawable.candle4
                    22L -> R.drawable.candle1
                    23L -> R.drawable.candle4
                    24L -> R.drawable.candle1
                    else -> R.drawable.jewstar0
                }


                 "Summer" -> when ((mCalendar.timeInMillis % (24 * frameTime)) / frameTime) {
                    0L -> R.drawable.starfish1
                    1L -> R.drawable.starfish2
                    2L -> R.drawable.starfish1
                    3L -> R.drawable.starfish2
                    4L -> R.drawable.starfish1
                    5L -> R.drawable.starfish2
                    6L -> R.drawable.starfishcoconut0
                    7L -> R.drawable.starfishcoconut1
                    8L -> R.drawable.starfishcoconut0
                    9L -> R.drawable.starfishcoconut1
                    10L -> R.drawable.starfishcoconut0
                    11L -> R.drawable.starfishcoconut1
                     12L -> R.drawable.rainbow1
                     13L -> R.drawable.rainbow2
                     14L -> R.drawable.rainbow1
                     15L -> R.drawable.rainbow2
                     16L -> R.drawable.rainbow3
                     17L -> R.drawable.rainbow4
                     18L -> R.drawable.rainbow3
                     19L -> R.drawable.rainbow4
                     20L -> R.drawable.rainbow5
                     21L -> R.drawable.rainbow6
                     22L -> R.drawable.rainbow5
                     23L -> R.drawable.rainbow6
                    else -> R.drawable.starfish1
                }

                "Irish" -> when ((mCalendar.timeInMillis % (30 * frameTime)) / frameTime) {
                    0L -> R.drawable.green0
                    1L -> R.drawable.green1
                    2L -> R.drawable.green0
                    3L -> R.drawable.green1
                    4L -> R.drawable.green0
                    5L -> R.drawable.green1
                    6L -> R.drawable.lucky21
                    7L -> R.drawable.lucky22
                    8L -> R.drawable.lucky23
                    9L -> R.drawable.lucky24
                    10L -> R.drawable.lucky25
                    11L -> R.drawable.lucky26
                    12L -> R.drawable.lucky27
                    13L -> R.drawable.lucky29
                    14L -> R.drawable.lucky30
                    15L -> R.drawable.lucky31
                    16L -> R.drawable.lucky32
                    17L -> R.drawable.lucky33
                    18L -> R.drawable.lucky34
                    19L -> R.drawable.lucky35
                    20L -> R.drawable.lucky36
                    21L -> R.drawable.lucky37
                    22L -> R.drawable.green0
                    23L -> R.drawable.green1
                    24L -> R.drawable.green0
                    25L -> R.drawable.green1
                    26L -> R.drawable.green0
                    27L -> R.drawable.green1
                    28L -> R.drawable.green0
                    29L -> R.drawable.green1
                    else -> R.drawable.green0
                }

                "Fall" -> when ((mCalendar.timeInMillis % (16 * frameTime)) / frameTime) {
                    0L -> R.drawable.cow0
                    1L -> R.drawable.cow1
                    2L -> R.drawable.cow0
                    3L -> R.drawable.cow1
                    4L -> R.drawable.cow0
                    5L -> R.drawable.cow1
                    6L -> R.drawable.cow0
                    7L -> R.drawable.cow1
                    8L -> R.drawable.cow0
                    9L -> R.drawable.turkey0
                    10L -> R.drawable.turkey1
                    11L -> R.drawable.turkey0
                    12L -> R.drawable.turkey1
                    13L -> R.drawable.turkey0
                    15L -> R.drawable.turkey1
                    else -> R.drawable.turkey1
                }

                else -> when ((mCalendar.timeInMillis % (22 * frameTime)) / frameTime) {
                    0L -> R.drawable.rainbow1
                    1L -> R.drawable.rainbow2
                    2L -> R.drawable.rainbow1
                    3L -> R.drawable.rainbow2
                    4L -> R.drawable.rainbow3
                    5L -> R.drawable.rainbow4
                    6L -> R.drawable.rainbow3
                    7L -> R.drawable.rainbow4
                    8L -> R.drawable.rainbow5
                    9L -> R.drawable.rainbow6
                    10L -> R.drawable.rainbow5
                    11L -> R.drawable.rainbow6
                    12L -> R.drawable.rainbow1
                    13L -> R.drawable.rainbow2
                    14L -> R.drawable.rainbow3
                    15L -> R.drawable.rainbow2
                    16L -> R.drawable.rainbow1
                    17L -> R.drawable.rainbow2
                    18L -> R.drawable.rainbow3
                    19L -> R.drawable.rainbow4
                    20L -> R.drawable.rainbow5
                    21L -> R.drawable.rainbow6
                    else -> R.drawable.rainbow1
                }
            }

            canvas.drawBitmap(
                BitmapFactory.decodeResource(applicationContext.resources, drawable),
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
                mBackgroundBitmap = Bitmap.createScaledBitmap(
                    getHolidayBackground(),
                    mBackgroundBitmap.width,
                    mBackgroundBitmap.height, true
                )

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
