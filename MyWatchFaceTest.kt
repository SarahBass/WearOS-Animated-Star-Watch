package com.academy.testwatch3

import org.junit.runner.RunWith
import org.junit.Test
import org.mockito.junit.MockitoJUnitRunner
import java.util.*

@RunWith(MockitoJUnitRunner::class)
class MyWatchFaceTest {
    @Test
    fun test1() {
        assert(MyWatchFaceUtils().getHolidayBackgroundDrawable(Date()) == R.drawable.cincodemayo)
    }
}
