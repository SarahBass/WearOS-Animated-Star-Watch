package com.academy.testwatch3

import org.junit.runner.RunWith
import org.junit.Test
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
//Date(int year, int month, int date, int hrs, int min)
@RunWith(MockitoJUnitRunner::class)
class MyWatchFaceTest {
    @Test
    fun test1() {
        assert(MyWatchFaceUtils().getHolidayBackgroundDrawable(Date(2022, 5, 5, 5, 5)) == R.drawable.cincodemayo)
    }
}
//Expected Output:
//May 5th 2023: full
//May 5 2022 : wax
// May 5th, 2026 : wan
/*
class moonPhaseTest {
   @Test

   fun test1() {
       assert(MyWanMoonFaceUtils().drawMoon(Date(2026, 5, 5, 5, 5))) == R.drawable.wan)
   }
   fun test2() {
       assert(MyWaxMoonFaceUtils().drawMoon(Date(2022, 5, 5, 5, 5))) == R.drawable.wax)
   }
   fun test3() {
       assert(MyFullMoonFaceUtils().drawMoon(Date(2023, 5, 5, 5, 5))) == R.drawable.fullpng)
   }
}
*/
