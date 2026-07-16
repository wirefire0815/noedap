package dev.whitefire.noedap.util

import android.app.TimePickerDialog
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import dev.whitefire.noedap.data.local.WorkDayEntity
import dev.whitefire.noedap.domain.model.WorkDay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Extension functions for various utility operations
 */

// ========== Time/Date Formatting ==========

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
private val DATE_SHORT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

fun LocalTime.formatTime(): String = this.format(TIME_FORMATTER)

fun LocalDate.formatDate(): String = this.format(DATE_FORMATTER)

fun LocalDate.formatShortDate(): String = this.format(DATE_SHORT_FORMATTER)

fun LocalDate.isToday(): Boolean = this == LocalDate.now()

fun LocalDate.isYesterday(): Boolean = this == LocalDate.now().minusDays(1)

fun LocalDate.isTomorrow(): Boolean = this == LocalDate.now().plusDays(1)

// ========== Float Formatting ==========

fun Float.formatHours(): String {
    val hours = this.toInt()
    val minutes = ((this - hours) * 60).toInt()
    return String.format("%02d:%02d", hours, minutes)
}

fun Float.formatHoursWithDecimal(): String {
    return String.format("%.2f", this)
}

// ========== Model <-> Entity Mapping ==========

fun WorkDay.toEntity(): WorkDayEntity {
    return WorkDayEntity(
        id = id,
        date = date,
        startTime = startTime,
        endTime = endTime,
        breakMinutes = breakMinutes,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun WorkDayEntity.toModel(): WorkDay {
    return WorkDay(
        id = id,
        date = date,
        startTime = startTime,
        endTime = endTime,
        breakMinutes = breakMinutes,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ========== Time Picker Dialog ==========

fun EditText.showTimePicker(
    context: Context,
    initialTime: LocalTime = LocalTime.of(9, 30),
    onTimeSelected: (LocalTime) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, initialTime.hour)
    calendar.set(Calendar.MINUTE, initialTime.minute)
    
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimeSelected(LocalTime.of(hourOfDay, minute)) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

fun android.widget.Button.showTimePicker(
    context: Context,
    initialTime: LocalTime = LocalTime.of(9, 30),
    onTimeSelected: (LocalTime) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, initialTime.hour)
    calendar.set(Calendar.MINUTE, initialTime.minute)
    
    TimePickerDialog(
        context,
        { _, hourOfDay, minute -> onTimeSelected(LocalTime.of(hourOfDay, minute)) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    ).show()
}

// ========== Toast Extensions ==========

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// ========== Validation ==========

fun LocalTime?.isValid(): Boolean = this != null

fun Int.isValidBreakMinutes(): Boolean = this >= 0 && this < 1440 // Max 24 hours

// ========== Duration Helpers ==========

fun Int.minutesToHoursMinutes(): Pair<Int, Int> {
    val hours = this / 60
    val minutes = this % 60
    return Pair(hours, minutes)
}

fun Int.minutesToDisplayString(): String {
    val (hours, minutes) = this.minutesToHoursMinutes()
    return String.format("%02d:%02d", hours, minutes)
}

// ========== Week Calculation ==========

fun LocalDate.getWeekStart(): LocalDate {
    var date = this
    while (date.dayOfWeek.value != 1) { // Monday
        date = date.minusDays(1)
    }
    return date
}

fun LocalDate.getWeekEnd(): LocalDate {
    var date = this
    while (date.dayOfWeek.value != 7) { // Sunday
        date = date.plusDays(1)
    }
    return date
}

fun LocalDate.getIsoWeekNumber(): Int {
    val weekFields = java.time.temporal.WeekFields.ISO
    return this.get(weekFields.weekOfWeekBasedYear())
}
