package dev.whitefire.nit.domain.model

/**
 * Statistics for a work week
 */
data class WeekStats(
    val totalHours: Float,
    val remainingHours: Float,
    val progressPercentage: Float,
    val targetMet: Boolean,
    val todayHours: Float,
    val daysWorked: Int,
    val totalDays: Int
)
