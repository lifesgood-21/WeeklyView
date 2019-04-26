package ru.dnevnik.weeklyview

import java.util.*

fun Calendar.clearTime(){
    this.set(Calendar.HOUR,0)
    this.set(Calendar.HOUR_OF_DAY,0)
    this.set(Calendar.MINUTE,0)
    this.set(Calendar.SECOND,0)
    this.set(Calendar.MILLISECOND,0)
}
