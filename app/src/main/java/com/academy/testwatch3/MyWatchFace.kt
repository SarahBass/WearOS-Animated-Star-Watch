package com.academy.testwatch3
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import kotlin.math.floor
import kotlin.math.roundToInt


/**
 * Updates rate in milliseconds for interactive mode. We update once a second to advance the
 * second hand.
 */
private const val INTERACTIVE_UPDATE_RATE_MS = 600

/**
 * Handler message id for updating the time periodically in interactive mode.
 */
private const val MSG_UPDATE_TIME = 0
private const val HOUR_STROKE_WIDTH =  12f
private const val MINUTE_STROKE_WIDTH = 10f
private const val SECOND_TICK_STROKE_WIDTH = 5f
private const val CENTER_GAP_AND_CIRCLE_RADIUS = 4f
private const val SHADOW_RADIUS = 7f

private var heartRate: Float = 0f
private var stepCount: Float = 0f

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



class MyWatchFace : CanvasWatchFaceService(), SensorEventListener {



    override fun onCreate() {
        Log.i("tag", "onStart Service ${this.application.applicationInfo}")

        val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL, 5_000_000);

        val mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        mSensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL, 5_000_000);

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


        fun getHolidayBackground(): Bitmap {
            val d = Date()

            val backgroundBitmap: Bitmap =
                BitmapFactory.decodeResource(resources, MyWatchFaceUtils().getHolidayBackgroundDrawable(d))

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

       val lunarArray = arrayOf(
           "February 1 2022",
           "January 22 2023",
           "February 10 2024",
           "January 29 2025",
           "February 17 2026",
           "February 7 2027",
           "January 26 2028",
           "February 13 2029",
           "February 2 2030"
       )
       val sdf6 = SimpleDateFormat("MMMM d")
       val birthdaySpaces: String = sdf6.format(d)
       val birthdayArray = arrayOf(
           "December 27",
           "January 24",
           "July 11"
       )

            val caseString =
                if (birthdayArray.contains(birthdaySpaces) ){
                    "Birthday"
                }
       else if (monthOfYear == "October") {
                    if (dayOfMonth == "31" || dayOfMonth == "30" || dayOfMonth == "1") {"Halloween"
                    } else {"October"}
                } else if (monthOfYear == "November") {
                    if (dayOfTheWeekLong == "Wednesday" || dayOfTheWeekLong == "Thursday" || dayOfTheWeekLong == "Fri") {
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
                    } else if (dayOfMonth == "31"){"New Year"}
                    else {
                        "Winter"
                    }
                }else if (monthOfYear == "January") {
                    if (lunarArray.contains(fullDateSpaces) || dayOfMonth == "1") {
                        "New Year"
                    } else if (Integer.parseInt(dayOfMonth) in 2..15) {
                        "IceRainbow"
                    } else {
                        "IceRainbow"}}
                else if (monthOfYear == "February") {
                    if (lunarArray.contains(fullDateSpaces)) {
                        "New Year"
                    } else if (Integer.parseInt(dayOfMonth) in 1..15) {
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
                }else if (monthOfYear == "May") {
                    if (dayOfMonth == "5"){
                    "Cinco de Mayo"
                    } else {
                        "Mother"
                    }
                } else if (monthOfYear == "July" || monthOfYear == "August") {
                    "Summer"
                }else if (monthOfYear == "September") {
                    "School"
                }
                else {
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
        private fun changeWandColor() {
        /* Extracts colors from background image to improve watchface style. */
        Palette.from(mBackgroundBitmap).generate {
            it?.let {
                mWatchHandHighlightColor = it.getVibrantColor(Color.WHITE)
                mWatchHandColor = it.getLightVibrantColor(Color.WHITE)
                mWatchHandShadowColor = it.getDarkMutedColor(Color.BLACK)
                updateWatchHandStyle()
            }
        }}

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
                mMinutePaint.strokeWidth = MINUTE_STROKE_WIDTH
                mHourPaint.strokeWidth = HOUR_STROKE_WIDTH
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
            mCenterX = width / 2f
            mCenterY = height / 2f
            mSecondHandLength = (mCenterX * 0.6).toFloat()
            sMinuteHandLength =  (mCenterX * 0.6).toFloat()
            sHourHandLength =  (mCenterX * 0.6).toFloat()

            /* Scale loaded background image (more efficient) if surface dimensions change. */
            val scale = width.toFloat() / mBackgroundBitmap.width.toFloat()

            mBackgroundBitmap = Bitmap.createScaledBitmap(
                mBackgroundBitmap,
                (mBackgroundBitmap.width * scale).toInt(),
                (mBackgroundBitmap.height * scale).toInt(), true
            )

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
                WatchFaceService.TAP_TYPE_TAP -> {


                }
                 //The user has completed the tap gesture.
                //Can user open up xml or pages
            }
            invalidate()
        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            mCalendar.timeInMillis = now
            updateWatchHandStyle()
            drawBackground(canvas)
            drawWatchFace(canvas)
            drawAnimation(canvas, bounds)
            drawStepsFace(canvas)
            drawDaysFace(canvas)
            drawMonthsFace(canvas)
            drawMonth(canvas, bounds)
            drawDay(canvas, bounds)
            drawMoon(canvas, bounds)
            drawMinTen(canvas, bounds)
            drawHours(canvas, bounds)
            drawMin(canvas, bounds)
            drawAMPM(canvas, bounds)
            drawDates(canvas, bounds)
            drawDatesTen(canvas, bounds)
            drawHeartRates(canvas, bounds)
            drawHeartRatesTens(canvas, bounds)
            drawHeartRatesHundreds(canvas, bounds)
            initGrayBackgroundBitmap()
            changeWandColor()
        }

        private fun drawAnimation(canvas: Canvas, bounds: Rect) {
            val frameTime = INTERACTIVE_UPDATE_RATE_MS
            var drawable = when (getAnimationCase()) {
                "IceRainbow" -> when ((mCalendar.timeInMillis % (43 * frameTime)) / frameTime) {
                    0L -> R.drawable.rainbow1
                    1L -> R.drawable.rainbow2
                    2L -> R.drawable.rainbow1
                    3L -> R.drawable.rainbow2
                    4L -> R.drawable.rainbow1
                    5L -> R.drawable.rainbow2
                    6L -> R.drawable.rainbow3
                    7L -> R.drawable.rainbow4
                    8L -> R.drawable.rainbow5
                    9L -> R.drawable.rainbow6
                    10L -> R.drawable.rainbow1
                    11L -> R.drawable.rainbow2
                    12L -> R.drawable.rainbow3
                    13L -> R.drawable.rainbow4
                    14L -> R.drawable.rainbow5
                    15L -> R.drawable.rainbow6
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
                    28L -> R.drawable.snowman0
                    29L -> R.drawable.snowman1
                    30L -> R.drawable.snowman2
                    31L -> R.drawable.snowman0
                    32L -> R.drawable.snowman1
                    33L -> R.drawable.snowman2
                    34L -> R.drawable.snowman0
                    35L -> R.drawable.whitebright0
                    36L -> R.drawable.whitebright1
                    37L -> R.drawable.whitebright0
                    38L -> R.drawable.whitebright1
                    39L -> R.drawable.whitebright0
                    40L -> R.drawable.whitebright1
                    41L -> R.drawable.whitebright0
                    42L -> R.drawable.whitebright1
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
                    26L -> R.drawable.peppermint4
                    27L -> R.drawable.peppermint0
                    28L -> R.drawable.peppermint4
                    29L -> R.drawable.peppermint0
                    30L -> R.drawable.peppermint4
                    31L -> R.drawable.peppermint0
                    32L -> R.drawable.peppermint4
                    33L -> R.drawable.peppermint0
                    34L -> R.drawable.reindeer0
                    35L -> R.drawable.reindeer1
                    36L -> R.drawable.reindeer0
                    37L -> R.drawable.reindeer0
                    38L -> R.drawable.reindeer1
                    39L -> R.drawable.reindeer0
                    else -> R.drawable.peppermint0
                }
                "Christmas" -> when ((mCalendar.timeInMillis % (46 * frameTime)) / frameTime) {
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
                10L -> R.drawable.cookiesanta0
                11L -> R.drawable.cookiesanta1
                12L -> R.drawable.cookiesanta0
                13L -> R.drawable.cookiesanta1
                14L -> R.drawable.cookiesanta0
                15L -> R.drawable.cookiesanta1
                16L -> R.drawable.cookiesanta0
                17L -> R.drawable.ginger0
                18L -> R.drawable.ginger1
                19L -> R.drawable.ginger0
                20L -> R.drawable.ginger1
                21L -> R.drawable.ginger0
                22L -> R.drawable.ginger1
                23L -> R.drawable.ginger0
                24L -> R.drawable.ginger1
                25L -> R.drawable.peppermint0
                26L -> R.drawable.peppermint4
                27L -> R.drawable.peppermint0
                28L -> R.drawable.peppermint4
                29L -> R.drawable.peppermint0
                30L -> R.drawable.peppermint4
                31L -> R.drawable.peppermint0
                32L -> R.drawable.peppermint4
                33L -> R.drawable.peppermint0
                34L -> R.drawable.reindeer0
                35L -> R.drawable.reindeer1
                36L -> R.drawable.reindeer0
                37L -> R.drawable.reindeer0
                38L -> R.drawable.reindeer1
                39L -> R.drawable.reindeer0
                    40L -> R.drawable.reindeer0
                    41L -> R.drawable.reindeer1
                    42L -> R.drawable.reindeer0
                    43L -> R.drawable.reindeer0
                    44L -> R.drawable.reindeer1
                    45L -> R.drawable.reindeer0
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

                "Mother" -> when ((mCalendar.timeInMillis % (30 * frameTime)) / frameTime) {
                    0L -> R.drawable.motherdaystar0
                    1L -> R.drawable.motherdaystar1
                    2L -> R.drawable.motherdaystar0
                    3L -> R.drawable.motherdaystar1
                    4L -> R.drawable.motherdaystar0
                    5L -> R.drawable.motherdaystar1
                    6L -> R.drawable.motherdaystar0
                    7L -> R.drawable.motherdaystar1
                    8L -> R.drawable.motherdaystar0
                    9L -> R.drawable.motherdaystar1
                    10L -> R.drawable.seed0
                    11L -> R.drawable.seed1
                    12L -> R.drawable.seed2
                    13L -> R.drawable.seed0
                    14L -> R.drawable.seed1
                    15L -> R.drawable.seed2
                    16L -> R.drawable.seed0
                    17L -> R.drawable.seedjump1
                    18L -> R.drawable.bee0
                    19L -> R.drawable.bee1
                    20L -> R.drawable.bee0
                    21L -> R.drawable.bee1
                    22L -> R.drawable.bee0
                    23L -> R.drawable.bee1
                    24L -> R.drawable.bee0
                    25L -> R.drawable.bee1
                    26L -> R.drawable.bee0
                    27L -> R.drawable.bee1
                    28L -> R.drawable.bee0
                    29L -> R.drawable.bee1
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

                "New Year" -> when ((mCalendar.timeInMillis % (2 * frameTime)) / frameTime) {
                    0L -> R.drawable.darkergold1
                    1L -> R.drawable.darkergold2
                    else -> R.drawable.darkergold1
                }

                "School" -> when ((mCalendar.timeInMillis % (24 * frameTime)) / frameTime) {
                    0L -> R.drawable.schoolstar0
                    1L -> R.drawable.schoolstar1
                    2L -> R.drawable.schoolstar0
                    3L -> R.drawable.schoolstar1
                    4L -> R.drawable.schoolstar0
                    5L -> R.drawable.schoolstar1
                    6L -> R.drawable.schoolstar2
                    7L -> R.drawable.schoolstar3
                    8L -> R.drawable.schoolstar2
                    9L -> R.drawable.schoolstar3
                    10L -> R.drawable.schoolstar2
                    11L -> R.drawable.schoolstar3
                    12L -> R.drawable.schoolstar4
                    13L -> R.drawable.schoolstar5
                    14L -> R.drawable.schoolstar4
                    15L -> R.drawable.schoolstar5
                    16L -> R.drawable.schoolstar4
                    17L -> R.drawable.schoolstar5
                    18L -> R.drawable.darkrainbow0
                    19L -> R.drawable.darkrainbow1
                    20L -> R.drawable.darkrainbow0
                    21L -> R.drawable.darkrainbow1
                    22L -> R.drawable.darkrainbow0
                    23L -> R.drawable.darkrainbow1
                    else -> R.drawable.darkergold1
                }

                "Cinco de Mayo" -> when ((mCalendar.timeInMillis % (2 * frameTime)) / frameTime) {
                    0L -> R.drawable.cinco0
                    1L -> R.drawable.cinco1
                    else -> R.drawable.cinco0
                }

                "Birthday" -> when ((mCalendar.timeInMillis % (26 * frameTime)) / frameTime) {
                    0L -> R.drawable.eatcake0
                    1L -> R.drawable.eatcake2
                    2L -> R.drawable.eatcake0
                    3L -> R.drawable.eatcake2
                    4L -> R.drawable.eatcake0
                    5L -> R.drawable.eatcake2
                    6L -> R.drawable.candle5
                    7L -> R.drawable.candle1
                    8L -> R.drawable.candle5
                    9L -> R.drawable.candle1
                    10L -> R.drawable.candle5
                    11L -> R.drawable.candle1
                    12L -> R.drawable.candle5
                    13L -> R.drawable.candle1
                    14L -> R.drawable.candle5
                    15L -> R.drawable.candle1
                    16L -> R.drawable.heart0
                    17L -> R.drawable.heartkiss
                    18L -> R.drawable.heart0
                    19L -> R.drawable.heartkiss
                    20L -> R.drawable.heart0
                    21L -> R.drawable.heartkiss
                    22L -> R.drawable.heart0
                    23L -> R.drawable.heartkiss
                    24L -> R.drawable.eatcake0
                    25L -> R.drawable.eatcake2
                    else -> R.drawable.candle1
                }

                "Thanksgiving" -> when ((mCalendar.timeInMillis % (24 * frameTime)) / frameTime) {
                    0L -> R.drawable.turkey0
                    1L -> R.drawable.turkey1
                    2L -> R.drawable.turkey0
                    3L -> R.drawable.turkey1
                    4L -> R.drawable.turkey0
                    5L -> R.drawable.turkey1
                    6L -> R.drawable.turkey0
                    7L -> R.drawable.turkey1
                    8L -> R.drawable.turkey0
                    9L -> R.drawable.turkey1
                    10L -> R.drawable.turkey0
                    11L -> R.drawable.turkey1
                    12L -> R.drawable.squirrel0
                    13L -> R.drawable.squirrel1
                    14L -> R.drawable.squirrel0
                    15L -> R.drawable.squirrel1
                    16L -> R.drawable.squirrel0
                    17L -> R.drawable.squirrel1
                    18L -> R.drawable.squirrel0
                    19L -> R.drawable.squirrel1
                    20L -> R.drawable.squirrel0
                    21L -> R.drawable.squirrel1
                    22L -> R.drawable.squirrel0
                    23L -> R.drawable.squirrel1
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


                "October" -> when ((mCalendar.timeInMillis % (49 * frameTime)) / frameTime) {
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
                    18L -> R.drawable.witchcookie0
                    19L -> R.drawable.witchcookie1
                    20L -> R.drawable.witchcookie2
                    21L -> R.drawable.witchcookie1
                    22L -> R.drawable.witchcookie0
                    23L -> R.drawable.candle0
                    24L -> R.drawable.candle2
                    25L -> R.drawable.candle0
                    26L -> R.drawable.candle2
                    27L -> R.drawable.candle5
                    28L -> R.drawable.candle1
                    29L -> R.drawable.candle5
                    30L -> R.drawable.candle1
                    31L -> R.drawable.candle5
                    32L -> R.drawable.candle1
                    33L -> R.drawable.bat1
                    34L -> R.drawable.bat2
                    35L -> R.drawable.bat1
                    36L -> R.drawable.bat2
                    37L -> R.drawable.bat1
                    38L -> R.drawable.bat2
                    39L -> R.drawable.bat1
                    40L -> R.drawable.batpumpkin1
                    41L -> R.drawable.batpumpkin0
                    42L -> R.drawable.batpumpkin1
                    43L -> R.drawable.batpumpkin0
                    44L -> R.drawable.batpumpkin1
                    45L -> R.drawable.batpumpkin0
                    46L -> R.drawable.batpumpkin1
                    47L -> R.drawable.bat1
                    48L -> R.drawable.bat2
                    else -> R.drawable.bat1}


                "Jewish" -> when ((mCalendar.timeInMillis % (25 * frameTime)) / frameTime) {
                    0L -> R.drawable.jewstar0
                    1L -> R.drawable.jewstar2
                    2L -> R.drawable.jewstar0
                    3L -> R.drawable.jewstar2
                    4L -> R.drawable.jewstar0
                    5L -> R.drawable.jewstar2
                    6L -> R.drawable.jewstar0
                    7L -> R.drawable.jewstar1
                    8L -> R.drawable.jewstar2
                    9L -> R.drawable.jewishcandle0
                    10L -> R.drawable.jewishcandle1
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
                      6L -> R.drawable.green0
                       7L -> R.drawable.green1
                       8L -> R.drawable.green0
                       9L -> R.drawable.green1
                       10L -> R.drawable.darkergold1
                       11L -> R.drawable.darkergold2
                       12L -> R.drawable.darkergold1
                       13L -> R.drawable.darkergold2
                       14L -> R.drawable.darkergold1
                       15L -> R.drawable.darkergold2
                       16L -> R.drawable.darkergold1
                       17L -> R.drawable.darkergold2
                       18L -> R.drawable.darkergold1
                       19L -> R.drawable.darkergold2
                       20L -> R.drawable.darkergold1
                       21L -> R.drawable.darkergold2
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

                "Fall" -> when ((mCalendar.timeInMillis % (34 * frameTime)) / frameTime) {
                    0L -> R.drawable.cow0
                    1L -> R.drawable.cow1
                    2L -> R.drawable.cow0
                    3L -> R.drawable.cow1
                    4L -> R.drawable.cow0
                    5L -> R.drawable.cow1
                    6L -> R.drawable.cow0
                    7L -> R.drawable.cow1
                    8L -> R.drawable.cow0
                    9L -> R.drawable.gnome0
                    10L -> R.drawable.gnome1
                    11L -> R.drawable.gnome0
                    12L -> R.drawable.gnome1
                    13L -> R.drawable.gnome0
                    14L -> R.drawable.gnome1
                    15L -> R.drawable.gnome0
                    16L -> R.drawable.gnome1
                    17L -> R.drawable.gnome0
                    18L -> R.drawable.gnome1
                    19L -> R.drawable.gnome0
                    20L -> R.drawable.gnome1
                    21L -> R.drawable.gnome0
                    22L -> R.drawable.bee0
                    23L -> R.drawable.bee1
                    24L -> R.drawable.bee0
                    25L -> R.drawable.bee1
                    26L -> R.drawable.bee0
                    27L -> R.drawable.bee1
                    28L -> R.drawable.bee0
                    29L -> R.drawable.bee1
                    30L -> R.drawable.bee0
                    31L -> R.drawable.bee1
                    32L -> R.drawable.bee0
                    33L -> R.drawable.bee1
                    else -> R.drawable.turkey1
                }

                else -> when ((mCalendar.timeInMillis % (22 * frameTime)) / frameTime) {
                    0L -> R.drawable.rainbow1
                    1L -> R.drawable.rainbow2
                    2L -> R.drawable.rainbow1
                    3L -> R.drawable.rainbow2
                    4L -> R.drawable.rainbow1
                    5L -> R.drawable.rainbow2
                    6L -> R.drawable.rainbow3
                    7L -> R.drawable.rainbow4
                    8L -> R.drawable.rainbow5
                    9L -> R.drawable.rainbow6
                    10L -> R.drawable.whitebright0
                    11L -> R.drawable.whitebright1
                    12L -> R.drawable.whitebright0
                    13L -> R.drawable.whitebright1
                    14L -> R.drawable.whitebright0
                    15L -> R.drawable.whitebright1
                    16L -> R.drawable.whitebright0
                    17L -> R.drawable.rainbow1
                    18L -> R.drawable.rainbow2
                    19L -> R.drawable.rainbow3
                    20L -> R.drawable.rainbow4
                    21L -> R.drawable.rainbow5
                    else -> R.drawable.rainbow1

                }
            }

            if (mAmbient) {
                drawable = R.drawable.digitalwatchstar
            }

            val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

            val src = Rect(0, 0, bitmap.height, bitmap.width)
            val dst = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

            canvas.drawBitmap(
                bitmap,
                src,
                dst,
                null
            )
        }

        private fun drawMonth(canvas: Canvas, bounds: Rect) {
            val sdf2 = SimpleDateFormat("MMM")
            val d = Date()
            val month: String = sdf2.format(d)
            var drawable : Int = when (month){
                        "Jan" ->R.drawable.jan
                "Feb" ->R.drawable.feb
                "Mar" ->R.drawable.mar
                "Apr" ->R.drawable.apr
                "May" ->R.drawable.may
                "Jun" ->R.drawable.jun
                "Jul" ->R.drawable.jul
                "Aug" ->R.drawable.aug
                "Sep" ->R.drawable.sep
                "Oct" ->R.drawable.oct
                "Nov" ->R.drawable.nov
                "Dec" ->R.drawable.dec

               else -> R.drawable.jan}

if (mAmbient) {
    val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

    val src = Rect(0, 0, bitmap.height, bitmap.width)
    val dst = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

    canvas.drawBitmap(
        bitmap,
        src,
        dst,
        null
    )
}else{}
            }


        private fun drawDay(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("EEE")
            val d = Date()
            val dayOfTheWeek: String = sdf.format(d)
            var drawable : Int = when (dayOfTheWeek){
                "Mon" ->R.drawable.mon
                "Tues" ->R.drawable.tue
                "Wed" ->R.drawable.wed
                "Thu" ->R.drawable.thu
                "Fri" ->R.drawable.fri
                "Sat" ->R.drawable.sat
                "Sun" ->R.drawable.sun

                else -> R.drawable.sun}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }


        private fun drawMin(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("m")
            val d = Date()
            val minutes: String = sdf.format(d)
            var drawable : Int = when (Integer.parseInt(minutes)%10){
                0 ->R.drawable.minuteones0
                1->R.drawable.minuteones1
                2 ->R.drawable.minuteones2
                3 ->R.drawable.minuteones3
                4 ->R.drawable.minuteones4
                5-> R.drawable.minuteones5
                6 ->R.drawable.minuteones6
                7 ->R.drawable.minuteones7
                8 ->R.drawable.minuteones8
                9 ->R.drawable.minuteones9

                else -> R.drawable.minuteones0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawMinTen(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("m")
            val d = Date()
            val minutes: String = sdf.format(d)

            var drawable : Int = when ((floor(( (Integer.parseInt(minutes)/10)).toDouble()).toInt())){
                0 ->R.drawable.minuteones0
                1->R.drawable.minuteones1
                2 ->R.drawable.minuteones2
                3 ->R.drawable.minuteones3
                4 ->R.drawable.minuteones4
                5->R.drawable.minuteones5
                6 ->R.drawable.minuteones6
                7 ->R.drawable.minuteones7
                8 ->R.drawable.minuteones8
                9 ->R.drawable.minuteones9


                else -> R.drawable.minuteones0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top, bounds.right -30  , bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawDates(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("d")
            val d = Date()
            val day: String = sdf.format(d)

            var drawable : Int = when( Integer.parseInt(day)%10){
                0 ->R.drawable.day0
                1 ->R.drawable.day1
                2 ->R.drawable.day2
                3 ->R.drawable.day3
                4 ->R.drawable.day4
                5 ->R.drawable.day5
                6 ->R.drawable.day6
                7 ->R.drawable.day7
                8 ->R.drawable.day8
                9 ->R.drawable.day9
                else -> R.drawable.day0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top, bounds.right   , bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawDatesTen(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("d")
            val d = Date()
            val day: String = sdf.format(d)

            var drawable : Int = when ((floor(( (Integer.parseInt(day)/10)).toDouble()).toInt())){
                0 ->R.drawable.day0
                1 ->R.drawable.day1
                2 ->R.drawable.day2
                3 ->R.drawable.day3
                4 ->R.drawable.day4
                5 ->R.drawable.day5
                6 ->R.drawable.day6
                7 ->R.drawable.day7
                8 ->R.drawable.day8
                9 ->R.drawable.day9
                else -> R.drawable.day0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left -15 , bounds.top, bounds.right -15   , bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawHeartRates(canvas: Canvas, bounds: Rect) {
            val yourHeart = heartRate.roundToInt()

            var drawable : Int = when (yourHeart % 10){
                0 ->R.drawable.hearratet0
                1 ->R.drawable.heartrate1
                2 ->R.drawable.heartrate2
                3 ->R.drawable.heartrate3
                4 ->R.drawable.heartrate4
                5 ->R.drawable.heartrate5
                6 ->R.drawable.heartrate6
                7 ->R.drawable.heartrate7
                8 ->R.drawable.heartrate8
                9 ->R.drawable.heartrate9
                else -> R.drawable.hearratet0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top , bounds.right   , bounds.bottom )

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }
        private fun drawHeartRatesTens(canvas: Canvas, bounds: Rect) {

            val yourHeart = heartRate.roundToInt()

            var drawable : Int = when ((floor(((yourHeart % 100) / 10).toDouble()).toInt())){
                0 ->R.drawable.hearratet0
                1 ->R.drawable.heartrate1
                2 ->R.drawable.heartrate2
                3 ->R.drawable.heartrate3
                4 ->R.drawable.heartrate4
                5 ->R.drawable.heartrate5
                6 ->R.drawable.heartrate6
                7 ->R.drawable.heartrate7
                8 ->R.drawable.heartrate8
                9 ->R.drawable.heartrate9
                else -> R.drawable.hearratet0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top , bounds.right -20  , bounds.bottom )

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawHeartRatesHundreds(canvas: Canvas, bounds: Rect) {

            val yourHeart = heartRate.roundToInt()

            var drawable : Int = when (floor((yourHeart / 100).toDouble()).toInt()){
                0 ->R.drawable.hearratet0
                1 ->R.drawable.heartrate1
                2 ->R.drawable.heartrate2
                3 ->R.drawable.heartrate3
                4 ->R.drawable.heartrate4
                5 ->R.drawable.heartrate5
                6 ->R.drawable.heartrate6
                7 ->R.drawable.heartrate7
                8 ->R.drawable.heartrate8
                9 ->R.drawable.heartrate9
                else -> R.drawable.hearratet0}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top , bounds.right -37  , bounds.bottom )

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawAMPM(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("a")
            val d = Date()
            val amPM: String = sdf.format(d)

            var drawable : Int = when (amPM){
                "AM" ->R.drawable.am
                "PM"->R.drawable.pm
                else -> R.drawable.pm}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left  , bounds.top, bounds.right   , bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }


        private fun drawHours(canvas: Canvas, bounds: Rect) {
            val sdf = SimpleDateFormat("h")
            val d = Date()
            val hours: String = sdf.format(d)

            var drawable : Int = when (hours){
                "1"->R.drawable.hour1
                "2" ->R.drawable.hour2
                "3" ->R.drawable.hour3
                "4" ->R.drawable.hour4
                "5"->R.drawable.hour5
                "6" ->R.drawable.hour6
                "7" ->R.drawable.hour7
                "8" ->R.drawable.hour8
                "9" ->R.drawable.hour9
                "10" ->R.drawable.hour10
                "11" ->R.drawable.hour11
                "12" ->R.drawable.hour12
                else -> R.drawable.hour12}

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left , bounds.top, bounds.right, bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }

        private fun drawMoon(canvas: Canvas, bounds: Rect) {

            val drawable : Int = MyFullMoonFaceUtils().getMoonDrawable(Date())

            if (mAmbient) {
                val bitmap = BitmapFactory.decodeResource(applicationContext.resources, drawable)

                val src = Rect(0, 0, bitmap.height, bitmap.width)
                val dst = Rect(bounds.left, bounds.top, bounds.right, bounds.bottom)

                canvas.drawBitmap(
                    bitmap,
                    src,
                    dst,
                    null
                )
            }else{}
        }


        private fun drawBackground(canvas: Canvas) {

            if (mAmbient) {
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

            val seconds =
                mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f
            val secondsRotation = seconds * 6f

            val minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f

            val hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = mCalendar.get(Calendar.HOUR) * 30 + hourHandOffset

            canvas.save()

            if (!mAmbient) {canvas.rotate(hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                mCenterX,
                mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                mCenterX,
                mCenterY - sHourHandLength,
                mHourPaint
            )}
            if (!mAmbient) {
            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY)
            canvas.drawLine(
                mCenterX,
                mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                mCenterX,
                mCenterY - sMinuteHandLength,
                mMinutePaint
            )}

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

            canvas.restore()
        }

        private fun drawStepsFace(canvas: Canvas) {
            if(mAmbient){
                val steps: Int = (stepCount / 100).roundToInt()
                val innerTickRadius = mCenterX - 54
                val outerTickRadius = mCenterX -58
                for (tickIndex in 0..steps) {
                    val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 105).toFloat()
                    val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                    val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                    val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                    val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                    canvas.drawLine(
                                  mCenterX + innerX, mCenterY + innerY,
                                mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint
                          )
            }}else{}
        }

        private fun drawDaysFace(canvas: Canvas) {
            val sdf3 = SimpleDateFormat("d")
            val d = Date()
            val dayOfMonth: String = sdf3.format(d)

            if(mAmbient){

                val days: Int = (dayOfMonth.toInt()) -1
                val innerTickRadius = mCenterX -20
                val outerTickRadius = mCenterX -25
                for (tickIndex in 0..days) {
                    val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 31).toFloat()
                    val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                    val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                    val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                    val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                    canvas.drawLine(
                        mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint
                    )
                }}else{}
        }

        private fun drawMonthsFace(canvas: Canvas) {
            val sdf2 = SimpleDateFormat("M")
            val d = Date()
            val monthOfYear: String = sdf2.format(d)

            if(mAmbient){

                val months: Int = (monthOfYear.toInt()) -1
                val innerTickRadius = mCenterX - 0
                val outerTickRadius = mCenterX -5
                for (tickIndex in 0..months) {
                    val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                    val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                    val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                    val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                    val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                    canvas.drawLine(
                        mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mTickAndCirclePaint
                    )
                }}else{}
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

    override fun onSensorChanged(event: SensorEvent?) {
        val mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        if (event?.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)) {
            event?.values?.get(0)?.let {
                heartRate = it
            }
        }

        if (event?.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)) {
            event?.values?.get(0)?.let {
                stepCount = it
            }
        }
    }

    override fun onAccuracyChanged(event: Sensor?, p1: Int) {
    }
}
