package com.academy.testwatch3

import org.junit.Assert.assertEquals
import org.junit.runner.RunWith
import org.junit.Test
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

//Date(int year, int month, int date, int hrs, int min)
@RunWith(MockitoJUnitRunner::class)
class MyWatchFaceTest {
    @Test
    fun test_all() {
        assertEquals(MyWatchFaceUtils().getHolidayBackgroundDrawable(Date(122, 4, 5, 5, 5)), R.drawable.cincodemayo)
        assertEquals(MyWatchFaceUtils().getHolidayBackgroundDrawable(Date(122, 11, 25, 5, 5)), R.drawable.december1)
        assertEquals(MyWatchFaceUtils().getHolidayBackgroundDrawable(Date(126, 3, 5, 5, 5)), R.drawable.easter)

        assertEquals(MyFullMoonFaceUtils().getMoonDrawable(Date(126, 4, 5, 5, 5)), R.drawable.wan)
        assertEquals(MyFullMoonFaceUtils().getMoonDrawable(Date(122, 4, 5, 5, 5)), R.drawable.wax)
        assertEquals(MyFullMoonFaceUtils().getMoonDrawable(Date(123, 4, 5, 5, 5)), R.drawable.full)
        assertEquals(MyFullMoonFaceUtils().getMoonDrawable(Date(126, 4, 16, 5, 5)), R.drawable.newmoontiny)
    }
}
