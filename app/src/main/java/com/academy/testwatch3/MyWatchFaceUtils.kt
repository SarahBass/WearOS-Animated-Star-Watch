package com.academy.testwatch3

import androidx.annotation.VisibleForTesting
import java.util.*

class MyWatchFaceUtils {
    fun getHolidayBackgroundDrawable(date: Date): Int {
        return R.drawable.cincodemayo
    }
}
class MyWanMoonFaceUtils {
    fun drawMoon(date: Date): Int {
        return R.drawable.wan
    }
}

class MyWaxMoonFaceUtils {
    fun drawMoon(date: Date): Int {
        return R.drawable.wax
    }
}

class MyFullMoonFaceUtils {
    fun drawMoon(date: Date): Int {
        return R.drawable.fullpng
    }
}

class MyNewMoonFaceUtils {
    fun drawMoon(date: Date): Int {
        return R.drawable.newmoontiny
    }
}
