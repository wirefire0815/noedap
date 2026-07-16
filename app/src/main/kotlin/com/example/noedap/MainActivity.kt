package com.example.noedap

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}

// Data classes and utilities for working time tracking
object WorkTimeCalculator {
    // Core working hours per week (38.5 hours)
    const val WEEKLY_TARGET_HOURS = 38.5f
    
    // Kernzeiten (core times)
    val MON_THU_CORE_START = LocalTime.of(9, 30)
    val MON_THU_CORE_END = LocalTime.of(16, 0)
    val FRI_CORE_START = LocalTime.of(9, 30)
    val FRI_CORE_END = LocalTime.of(12, 30)
    
    // Break rules: 30 min break after 6 hours of work
    const val BREAK_AFTER_HOURS = 6f
    const val BREAK_DURATION_HOURS = 0.5f
    
    data class WorkDay(
        val date: LocalDate,
        val startTime: LocalTime? = null,
        val endTime: LocalTime? = null,
        val breakMinutes: Int = 0
    ) {
        fun duration(): Duration? {
            if (startTime == null || endTime == null) return null
            val duration = Duration.between(startTime, endTime)
            return if (duration.isNegative) null else duration
        }
        
        fun netWorkDuration(): Duration? {
            val dur = duration() ?: return null
            return dur.minusMinutes(breakMinutes.toLong())
        }
        
        fun effectiveWorkHours(): Float {
            val dur = netWorkDuration() ?: return 0f
            return dur.toMinutes().toFloat() / 60f
        }
    }
    
    data class WorkWeek(
        val days: List<WorkDay> = emptyList()
    ) {
        fun totalHours(): Float {
            return days.sumOf { it.effectiveWorkHours() }
        }
        
        fun remainingHours(): Float {
            return WEEKLY_TARGET_HOURS - totalHours()
        }
        
        fun isComplete(): Boolean {
            return totalHours() >= WEEKLY_TARGET_HOURS
        }
    }
    
    /**
     * Calculate required break based on work duration
     */
    fun calculateBreakMinutes(workDuration: Duration): Int {
        val hours = workDuration.toMinutes().toFloat() / 60f
        if (hours >= BREAK_AFTER_HOURS) {
            return (BREAK_DURATION_HOURS * 60).toInt()
        }
        return 0
    }
    
    /**
     * Check if a time is within Kernzeit for a given day
     */
    fun isInKernzeit(date: LocalDate, time: LocalTime): Boolean {
        val dayOfWeek = date.dayOfWeek
        return when {
            dayOfWeek.value >= 1 && dayOfWeek.value <= 4 -> { // Mon-Thu
                !time.isBefore(MON_THU_CORE_START) && !time.isAfter(MON_THU_CORE_END)
            }
            dayOfWeek.value == 5 -> { // Fri
                !time.isBefore(FRI_CORE_START) && !time.isAfter(FRI_CORE_END)
            }
            else -> false // Weekend
        }
    }
}
