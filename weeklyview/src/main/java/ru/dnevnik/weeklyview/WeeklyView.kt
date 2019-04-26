package ru.dnevnik.weeklyview

import android.animation.Animator
import android.content.Context
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.CycleInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.weekly_view.view.*
import kotlinx.android.synthetic.main.weekly_view_day.view.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit

class WeeklyView: FrameLayout {

    private lateinit var root: RelativeLayout
    private var calendar: Calendar = Calendar.getInstance()
    private var currentMillis: Long = 0
    private var todayMillis: Long = 0
    private var leftBound = 0L
    private var rightBound = 0L
    private val dayHolders = arrayOfNulls<DayHolder>(7)
    private var dateChangeListener: OnDateChangeListener? = null

    private val dateClick = OnClickListener { v ->
        currentMillis = v.tag as Long
        setupDays(false)
        dateChangeListener?.onDateChanged(currentMillis)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()
        removeAllViewsInLayout()
        root = LayoutInflater.from(context).inflate(R.layout.weekly_view, this, false) as RelativeLayout
        resetCalendar()
        setupPrevNextButtons(root)
        initDayHolders(root)
        addView(root)
        setupDays(true)
    }

    fun setCurrentMillis(millis: Long, notifyAboutChanges: Boolean = true ){
        currentMillis = millis
        calendar.timeInMillis = millis
        val right = findRightDateBound(currentMillis)
        val left = findLeftDateBound(currentMillis)
        setDateBounds(left, right)
        setupDays(false)
        if (notifyAboutChanges)
            dateChangeListener?.onDateChanged(currentMillis)
    }

    private fun findLeftDateBound(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.clearTime()
        while (calendar.get(Calendar.MONTH) != Calendar.AUGUST)
            calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return calendar.timeInMillis
    }

    private fun findRightDateBound(millis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.clearTime()
        while (calendar.get(Calendar.MONTH) != Calendar.JULY)
            calendar.add(Calendar.MONTH, 1)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        return calendar.timeInMillis
    }

    fun getCurrentMillis(): Long{
        return currentMillis
    }

    fun setTroubleIndicators(indicatorsEnabledDelegate: IndicatorsEnabledDelegate) {
        dayHolders.forEach { dayHolder ->
            if (dayHolder?.date != null){
                val hasIndicator = indicatorsEnabledDelegate.hasIndicator(dayHolder.date)
                val indicatorRes = indicatorsEnabledDelegate.indicatorRes(dayHolder.date)
                dayHolder.setHasMarker(hasIndicator, indicatorRes)
            }
        }
    }

    fun setDateChangeListener(listener: OnDateChangeListener) {
        dateChangeListener = listener
    }

    private fun setupPrevNextButtons(root: RelativeLayout) {
        val weekClick = OnClickListener { v ->
            var translationX = width
            when (v.id) {
                R.id.previousWeek -> {
                    currentMillis -= TimeUnit.DAYS.toMillis(7)
                    calendar.add(Calendar.DAY_OF_WEEK, -7)
                }
                else -> {
                    translationX *= -1
                    currentMillis += TimeUnit.DAYS.toMillis(7)
                    calendar.add(Calendar.DAY_OF_WEEK, +7)
                }
            }
            this@WeeklyView.root
                    .animate()
                    .translationX(translationX.toFloat())
                    .setDuration(200)
                    .setListener(WeeklyViewReadyToUpdateListener(translationX * -1))
            clearIndicators()
        }
        root.nextWeek.setOnClickListener(weekClick)
        root.previousWeek.setOnClickListener(weekClick)
    }

    private fun clearIndicators() {
        dayHolders.forEach {
            it?.setHasMarker(false, null)
        }
    }

    private fun initDayHolders(root: View) {
        dayHolders[0] = DayHolder(root.monday)
        dayHolders[1] = DayHolder(root.tuesday)
        dayHolders[2] = DayHolder(root.wednesday)
        dayHolders[3] = DayHolder(root.thursday)
        dayHolders[4] = DayHolder(root.friday)
        dayHolders[5] = DayHolder(root.saturday)
        dayHolders[6] = DayHolder(root.sunday)
        dayHolders[0]?.itemView?.day?.setText(R.string.weekday_monday_short)
        dayHolders[1]?.itemView?.day?.setText(R.string.weekday_tuesday_short)
        dayHolders[2]?.itemView?.day?.setText(R.string.weekday_wednesday_short)
        dayHolders[3]?.itemView?.day?.setText(R.string.weekday_thursday_short)
        dayHolders[4]?.itemView?.day?.setText(R.string.weekday_friday_short)
        dayHolders[5]?.itemView?.day?.setText(R.string.weekday_saturday_short)
        dayHolders[6]?.itemView?.day?.setText(R.string.weekday_sunday_short)
        for (holder in dayHolders) {
            holder?.itemView?.setOnClickListener(dateClick)
        }
    }

    private fun resetCalendar() {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.firstDayOfWeek = Calendar.MONDAY
        currentMillis = calendar.timeInMillis
        todayMillis = currentMillis
    }

    private fun setupDays(notifyWeeklyListener: Boolean) {
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val start = calendar.timeInMillis
        setDate(0)
        for (i in 1..6) {
            calendar.add(Calendar.DATE, 1)
            setDate(i)
        }
        if (notifyWeeklyListener)
            dateChangeListener?.onWeekChanged(start, calendar.timeInMillis, currentMillis)

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        previousWeek.visibility = when (calendar.timeInMillis >= leftBound){
            true -> View.VISIBLE
            else -> View.GONE
        }
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        nextWeek.visibility = when(calendar.timeInMillis <= rightBound){
            true -> View.VISIBLE
            else -> View.GONE
        }
    }

    private fun setDate(index: Int) {
        val isSunday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        val isSelected = DateFormat.isSameDay(calendar.timeInMillis, currentMillis)
        val dayHolder = dayHolders[index]
        with(dayHolder!!){
            val context = itemView.context
            date = calendar.timeInMillis
            itemView.date?.text = calendar.get(Calendar.DATE).toString()
            context?.let { val color = ContextCompat.getColor(context, when (isSelected) {
                    true -> R.color.white
                        else -> {when(isSunday){
                            true -> R.color.gray
                            else -> R.color.black
                        }
                    }
                }
            )
                itemView.date?.setTextColor(color)
                itemView.day?.setTextColor(color)
            }
            var background = R.drawable.selected_day_background
            if (DateFormat.isSameDay(calendar.timeInMillis, System.currentTimeMillis())){
                setAsCurrentDay()
                background = R.drawable.selected_same_day_background
                if (isSelected){
                    context?.let { itemView.date?.setTextColor(ContextCompat.getColor(it, R.color.white)) }
                    context?.let { itemView.day?.setTextColor(ContextCompat.getColor(it, R.color.white)) }
                }
            }
            if (isSelected) {
                itemView.setBackgroundResource(background)
                itemView.animate()
                        ?.scaleX(0.95f)
                        ?.scaleY(0.95f)?.interpolator = CycleInterpolator(1f)
            } else {
                itemView.background = null
            }
            itemView.tag = calendar.timeInMillis
        }
    }

    private fun setDateBounds(min: Long, max:Long){
        if( min < max ){
            leftBound = min
            rightBound = max
        } else{
            throw Exception("WeeklyView.setDateBounds ERROR: min date can't be grater than max date")
        }
    }

    private class DayHolder(val itemView: View) {

        var date: Long = 0

        init {
            itemView.background = null
        }

        fun setHasMarker(hasTrouble: Boolean, @DrawableRes res: Int?) {
             itemView.dot.setBackgroundResource(res?:0)
             itemView.dot.visibility = when (hasTrouble){
                 true -> View.VISIBLE
                 else -> View.INVISIBLE
             }
        }

        fun setAsCurrentDay(){
            with(itemView){
                day.setTextColor(ContextCompat.getColor(itemView.context, R.color.tangerine_two))
                date.setTextColor(ContextCompat.getColor(itemView.context, R.color.tangerine_two))
            }
        }
    }

    private inner class WeeklyViewReadyToUpdateListener internal constructor(private val fromX: Int) : Animator.AnimatorListener {

        override fun onAnimationStart(animation: Animator?) {}

        override fun onAnimationEnd(animation: Animator) {
            root.translationX = fromX.toFloat()
            setupDays(true)
            dateChangeListener?.onDateChanged(currentMillis)
            root.animate().setDuration(100).translationX(0f).setListener(null)
        }

        override fun onAnimationCancel(animation: Animator) {}

        override fun onAnimationRepeat(animation: Animator) {}
    }

    interface OnDateChangeListener {

        fun onDateChanged(millis: Long)

        fun onWeekChanged(start: Long, end: Long, current: Long)

    }

    interface IndicatorsEnabledDelegate{

        fun hasIndicator(date: Long): Boolean

        fun indicatorRes(date: Long): Int?
    }
}
