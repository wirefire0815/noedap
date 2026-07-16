package dev.whitefire.nit.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Represents a work week with its days and statistics
 */
data class WorkWeek(
    val startDate: LocalDate,
    val workDays: List<WorkDay> = emptyList(),
    val config: WorkTimeConfig = DEFAULT_WORK_CONFIG
) {
    val endDate: LocalDate
        get() = startDate.plusDays(6) // Monday + 6 days = Sunday
    
    val weekNumber: Int
        get() {
            val weekFields = WeekFields.of(Locale.getDefault())
            return startDate.get(weekFields.weekOfWeekBasedYear())
        }
    
    val year: Int
        get() = startDate.year
    
    /**
     * Get work days for a specific day of week
     */
    fun getWorkDays(dayOfWeek: DayOfWeek): List<WorkDay> {
        return workDays.filter { it.dayOfWeek == dayOfWeek }
    }
    
    /**
     * Get work day for a specific date
     */
    fun getWorkDay(date: LocalDate): WorkDay? {
        return workDays.firstOrNull { it.date == date }
    }
    
    /**
     * Total gross hours worked this week
     */
    val totalGrossHours: Float
        get() = workDays.sumOf { day ->
            (day.grossDuration?.toMinutes()?.toFloat()?.div(60f) ?: 0f).toDouble()
        }.toFloat()
    
    /**
     * Total net hours worked this week (with breaks)
     */
    val totalNetHours: Float
        get() = workDays.sumOf { it.effectiveHours.toDouble() }.toFloat()
    
    /**
     * Remaining hours to reach target
     */
    val remainingHours: Float
        get() = config.weeklyTargetHours - totalNetHours
    
    /**
     * Check if week target is met
     */
    val isTargetMet: Boolean
        get() = totalNetHours >= config.weeklyTargetHours
    
    /**
     * Get progress percentage (0-100)
     */
    val progressPercentage: Float
        get() = (totalNetHours / config.weeklyTargetHours * 100f).coerceAtMost(100f)
    
    /**
     * Check if week is complete (all required days worked)
     */
    val isComplete: Boolean
        get() = isTargetMet && workDays.all { day ->
            day.startTime != null && day.endTime != null
        }
    
    /**
     * Get display string for the week
     */
    fun getDisplayRange(): String {
        val start = startDate
        val end = endDate
        return if (start.year == end.year) {
            "${start.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth} - ${end.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${end.dayOfMonth}, ${start.year}"
        } else {
            "${start.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth}, ${start.year} - ${end.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())} ${end.dayOfMonth}, ${end.year}"
        }
    }
    
    /**
     * Get work days sorted by date
     */
    fun getSortedWorkDays(): List<WorkDay> {
        return workDays.sortedBy { it.date }
    }
    
    /**
     * Add or update a work day
     */
    fun withWorkDay(workDay: WorkDay): WorkWeek {
        val existingIndex = workDays.indexOfFirst { it.date == workDay.date }
        val newDays = workDays.toMutableList()
        
        if (existingIndex >= 0) {
            newDays[existingIndex] = workDay
        } else {
            newDays.add(workDay)
        }
        
        return copy(workDays = newDays)
    }
    
    /**
     * Remove a work day
     */
    fun withoutWorkDay(date: LocalDate): WorkWeek {
        return copy(workDays = workDays.filter { it.date != date })
    }
    
    companion object {
        /**
         * Create a WorkWeek for the current week
         */
        fun currentWeek(config: WorkTimeConfig = DEFAULT_WORK_CONFIG): WorkWeek {
            val today = LocalDate.now()
            val weekFields = WeekFields.of(Locale.getDefault())
            val weekNumber = today.get(weekFields.weekOfWeekBasedYear())
            // Find the Monday of the current week
            val startDate = today.minusDays(today.dayOfWeek.value.toLong() - DayOfWeek.MONDAY.value.toLong())
            return WorkWeek(startDate, emptyList(), config)
        }
        
        /**
         * Create a WorkWeek from a date
         */
        fun fromDate(date: LocalDate, config: WorkTimeConfig = DEFAULT_WORK_CONFIG): WorkWeek {
            val weekFields = WeekFields.of(Locale.getDefault())
            // Find the Monday of the week containing this date
            val startDate = date.minusDays(date.dayOfWeek.value.toLong() - DayOfWeek.MONDAY.value.toLong())
            return WorkWeek(startDate, emptyList(), config)
        }
        
        /**
         * Create a WorkWeek from year and week number
         */
        fun fromYearWeek(year: Int, week: Int, config: WorkTimeConfig = DEFAULT_WORK_CONFIG): WorkWeek? {
            return try {
                val weekFields = WeekFields.of(Locale.getDefault())
                // Create a date for the first day of the year
                val firstDay = LocalDate.of(year, 1, 1)
                // Find the first Monday of the year
                val firstMonday = firstDay.plusDays(((DayOfWeek.MONDAY.value - firstDay.dayOfWeek.value + 7) % 7).toLong())
                // Add (week-1) weeks to get to the start of the desired week
                val startDate = firstMonday.plusWeeks((week - 1).toLong())
                WorkWeek(startDate, emptyList(), config)
            } catch (e: Exception) {
                null
            }
        }
    }
}
