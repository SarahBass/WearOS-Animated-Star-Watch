package com.academy.testwatch3

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class MyWatchFaceUtils {
    fun getHolidayBackgroundDrawable(d: Date): Int {
        val sdf = SimpleDateFormat("EEE")
        val sdf1 = SimpleDateFormat("EEEE")
        val sdf2 = SimpleDateFormat("MMMM")
        val sdf3 = SimpleDateFormat("d")
        val sdf4 = SimpleDateFormat("yyyy")
        val sdf5 = SimpleDateFormat("MMMM d yyyy")

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
        //Chinese New Year Starts on New Moon
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


        val birthdayArray =
        arrayOf(
            "December 27",
            "January 24",
            "July 11")

        val sdf6 = SimpleDateFormat("MMMM d")
        val birthdaySpaces: String = sdf6.format(d)

        val drawable: Int =
            if (birthdayArray.contains(birthdaySpaces)) {
                R.drawable.birthday
            } else if (monthOfYear == "October") {
                if (dayOfMonth == "31" || dayOfMonth == "30" || dayOfMonth == "1") {
                    R.drawable.october1
                } else {
                    R.drawable.october2
                }
            } else if (monthOfYear == "September") {
                if (dayOfTheWeek == "Mon" || dayOfTheWeek == "Wed") {
                    R.drawable.school0
                } else {
                    R.drawable.school1
                }
            } else if (monthOfYear == "November") {
                if (dayOfTheWeek == "Mon" || dayOfTheWeek == "Wed" || dayOfTheWeek == "Fri") {
                    R.drawable.november2
                } else {
                    R.drawable.november1
                }
            } else if (monthOfYear == "December") {
                //Christmas & Christmas Eve
                if (dayOfMonth == "25" || dayOfMonth == "24") {
                    R.drawable.december1
                }
                //https://www.calendardate.com/hanukkah_2030.htm has dates up to 2030 for Hanukah or use HebrewCalendar (YEAR, 2, 25)
                else if ((Integer.parseInt(year4digits) == 2022 && Integer.parseInt(
                        dayOfMonth
                    ) in 18..23) ||
                    (Integer.parseInt(year4digits) == 2023 && Integer.parseInt(dayOfMonth) in 7..15) ||
                    (Integer.parseInt(year4digits) == 2024 && Integer.parseInt(dayOfMonth) in 26..30) ||
                    (Integer.parseInt(year4digits) == 2025 && Integer.parseInt(dayOfMonth) in 14..22) ||
                    (Integer.parseInt(year4digits) == 2026 && Integer.parseInt(dayOfMonth) in 4..12) ||
                    (Integer.parseInt(year4digits) == 2027 && Integer.parseInt(dayOfMonth) in 26..30) ||
                    (Integer.parseInt(year4digits) == 2028 && Integer.parseInt(dayOfMonth) in 12..20) ||
                    (Integer.parseInt(year4digits) == 2029 && Integer.parseInt(dayOfMonth) in 1..9) ||
                    (Integer.parseInt(year4digits) == 2030 && Integer.parseInt(dayOfMonth) in 20..23)
                ) {
                    R.drawable.jewishholiday
                } else if (dayOfMonth == "31") {
                    R.drawable.newyear
                } else {
                    R.drawable.december2
                }
            } else if (monthOfYear == "January") {
                if (dayOfMonth == "1") {
                    R.drawable.newyear
                } else if (lunarArray.contains(fullDateSpaces)) {
                    R.drawable.chinese
                } else if (Integer.parseInt(dayOfMonth) in 2..15) {
                    R.drawable.icerainbow
                } else {
                    R.drawable.tuesday
                }
            } else if (monthOfYear == "February") {
                if (lunarArray.contains(fullDateSpaces)) {
                    R.drawable.chinese
                } else if (Integer.parseInt(dayOfMonth) in 1..15) {
                    R.drawable.feb14
                } else {
                    R.drawable.springflower
                }
            } else if (monthOfYear == "March") {
                if (Integer.parseInt(dayOfMonth) in 1..18) {
                    R.drawable.march17
                } else if (easterArray.contains(fullDateSpaces)) {
                    R.drawable.easter
                } else {
                    R.drawable.springflower
                }
            } else if (monthOfYear == "April") {
                if (easterArray.contains(fullDateSpaces)) {
                    R.drawable.easter
                } else {
                    R.drawable.springflower
                }
            } else if (monthOfYear == "May") {
                if (dayOfMonth == "5") {
                    R.drawable.cincodemayo
                } else {
                    R.drawable.motherday
                }
            } else if (monthOfYear == "July" || monthOfYear == "August") {
                R.drawable.summerbeach
            } else {
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
            }

        return drawable
    }
}

class MyFullMoonFaceUtils {
    private fun getFullMoonDate(d: Date): String {
        val sdf0 = SimpleDateFormat("yyyy MMMM")
        val yearMonth: String = sdf0.format(d)
        val fullMoonDate = when(yearMonth){
            "2022 January" -> "17"
            "2022 February" -> "16"
            "2022 March" -> "18"
            "2022 April" -> "16"
            "2022 May" -> "16"
            "2022 June" -> "14"
            "2022 July" -> "13"
            "2022 August" -> "11"
            "2022 September" -> "10"
            "2022 October" -> "9"
            "2022 November" -> "8"
            "2022 December" -> "7"
            "2023 January" -> "6"
            "2023 February" -> "5"
            "2023 March" -> "7"
            "2023 April" -> "5"
            "2023 May" -> "5"
            "2023 June" -> "3"
            "2023 July" -> "3"
            "2023 August" -> "1"
            "2023 September" -> "29"
            "2023 October" -> "28"
            "2023 November" -> "27"
            "2023 December" -> "26"
            "2024 January" -> "25"
            "2024 February" -> "24"
            "2024 March" -> "25"
            "2024 April" -> "23"
            "2024 May" -> "23"
            "2024 June" -> "21"
            "2024 July" -> "21"
            "2024 August" -> "19"
            "2024 September" -> "17"
            "2024 October" -> "17"
            "2024 November" -> "15"
            "2024 December" -> "15"
            "2025 January" -> "13"
            "2025 February" -> "12"
            "2025 March" -> "13"
            "2025 April" -> "12"
            "2025 May" -> "12"
            "2025 June" -> "11"
            "2025 July" -> "10"
            "2025 August" -> "9"
            "2025 September" -> "7"
            "2025 October" -> "6"
            "2025 November" -> "5"
            "2025 December" -> "4"
            "2026 January" -> "3"
            "2026 February" -> "1"
            "2026 March" -> "3"
            "2026 April" -> "1"
            "2026 May" -> "1"
            "2026 June" -> "29"
            "2026 July" -> "29"
            "2026 August" -> "27"
            "2026 September" -> "26"
            "2026 October" -> "25"
            "2026 November" -> "24"
            "2026 December" -> "23"
            "2027 January" -> "22"
            "2027 February" -> "20"
            "2027 March" -> "22"
            "2027 April" -> "20"
            "2027 May" -> "20"
            "2027 June" -> "18"
            "2027 July" -> "18"
            "2027 August" -> "17"
            "2027 September" -> "15"
            "2027 October" -> "15"
            "2027 November" -> "13"
            "2027 December" -> "13"
            "2028 January" -> "11"
            "2028 February" -> "10"
            "2028 March" -> "10"
            "2028 April" -> "9"
            "2028 May" -> "8"
            "2028 June" -> "6"
            "2028 July" -> "6"
            "2028 August" -> "5"
            "2028 September" -> "3"
            "2028 October" -> "3"
            "2028 November" -> "2"
            "2028 December" -> "1"
            else -> "1"
        }
        return fullMoonDate
    }

    private fun getNewMoonDate(d: Date): String {

        val sdf0 = SimpleDateFormat("yyyy MMMM")
        val yearMonth: String = sdf0.format(d)
        val newMoonDate = when(yearMonth){
            "2022 January" -> 2
            "2022 February" -> 1 //It's Actually Jan31
            "2022 March" -> 2 //March31
            "2022 April" -> 30
            "2022 May" -> 30
            "2022 June" -> 28
            "2022 July" -> 28
            "2022 August" -> 27
            "2022 September" -> 25
            "2022 October" -> 25
            "2022 November" -> 23
            "2022 December" -> 23

            "2023 January" -> 21
            "2023 February" -> 19
            "2023 March" -> 21
            "2023 April" -> 19
            "2023 May" -> 19
            "2023 June" -> 17
            "2023 July" -> 17
            "2023 August" -> 16
            "2023 September" -> 14
            "2023 October" -> 14
            "2023 November" -> 13
            "2023 December" -> 12

            "2024 January" -> 11
            "2024 February" -> 9
            "2024 March" -> 10
            "2024 April" -> 8
            "2024 May" -> 7
            "2024 June" -> 6
            "2024 July" -> 5
            "2024 August" -> 4
            "2024 September" -> 2
            "2024 October" -> 2
            "2024 November" -> 1 //Nov 30
            "2024 December" -> 30

            "2025 January" -> 29
            "2025 February" -> 27
            "2025 March" -> 29
            "2025 April" -> 27
            "2025 May" -> 26
            "2025 June" -> 25
            "2025 July" -> 24
            "2025 August" -> 22
            "2025 September" -> 21
            "2025 October" -> 21
            "2025 November" -> 19
            "2025 December" -> 19

            "2026 January" -> 18
            "2026 February" -> 17
            "2026 March" -> 18
            "2026 April" -> 17
            "2026 May" -> 16
            "2026 June" -> 14
            "2026 July" -> 14
            "2026 August" -> 12
            "2026 September" -> 10
            "2026 October" -> 10
            "2026 November" -> 8
            "2026 December" -> 8

            "2027 January" -> 7
            "2027 February" -> 6
            "2027 March" -> 8
            "2027 April" -> 6
            "2027 May" -> 6
            "2027 June" -> 4
            "2027 July" -> 3
            "2027 August" -> 2
            "2027 September" -> 29
            "2027 October" -> 29
            "2027 November" -> 27
            "2027 December" -> 27

            "2028 January" -> 26
            "2028 February" -> 25
            "2028 March" -> 25
            "2028 April" -> 24
            "2028 May" -> 24
            "2028 June" -> 22
            "2028 July" -> 21
            "2028 August" -> 20
            "2028 September" -> 18
            "2028 October" -> 17
            "2028 November" -> 16
            "2028 December" -> 15
            else -> 1
        }
        return newMoonDate.toString()
    }

    fun getMoonString(d: Date): String {
        val sdf1 = SimpleDateFormat("d")
        val dayOfMonth: String = sdf1.format(d)

        //val LUNAR_MONTH = 29.530588853;
        val newMoondifference = abs((Integer.parseInt(dayOfMonth)) - (Integer.parseInt(getNewMoonDate(d))))
        val fullMoondifference = abs((Integer.parseInt(dayOfMonth)) - (Integer.parseInt(getFullMoonDate(d))))
        //val moonPercent : Double = newMoondifference / LUNAR_MONTH

        val moonString : String =
            if (fullMoondifference == 0 ){"Full Moon"}
            else if (newMoondifference == 0 ){"New Moon"}
            else if (Integer.parseInt(dayOfMonth) > (Integer.parseInt(getFullMoonDate(d)))) {"Waning"}
            else if (Integer.parseInt(dayOfMonth) < (Integer.parseInt(getFullMoonDate(d)))) {"Waxing"}
            else{"Plain Moon"}

        return moonString
    }

    fun getMoonDrawable(d: Date): Int {

        val moonString = MyFullMoonFaceUtils().getMoonString(d)

        val drawable : Int = when (moonString) {
            "Full Moon" ->R.drawable.full
            "Waxing" ->R.drawable.wax
            "Waning" ->R.drawable.wan
            "New Moon" ->R.drawable.newmoontiny
            else -> R.drawable.wax
        }

        return drawable

    }
}
