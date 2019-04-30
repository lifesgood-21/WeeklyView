package ru.lifesgood.weeklyview

import java.util.*

object DateFormat {

    private val calendarInstance = Calendar.getInstance()
    private val calendarCompare = Calendar.getInstance()

    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        calendarInstance.timeInMillis = timestamp1
        calendarCompare.timeInMillis = timestamp2
        val date1 = calendarInstance.get(Calendar.DATE)
        val date2 = calendarCompare.get(Calendar.DATE)
        val month1 = calendarInstance.get(Calendar.MONTH)
        val month2 = calendarCompare.get(Calendar.MONTH)
        val year1 = calendarInstance.get(Calendar.YEAR)
        val year2 = calendarCompare.get(Calendar.YEAR)
        return date1 == date2 && month1 == month2 && year1 == year2
    }
}