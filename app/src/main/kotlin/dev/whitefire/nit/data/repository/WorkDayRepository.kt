package dev.whitefire.nit.data.repository

import dev.whitefire.nit.data.local.WorkDayDao
import dev.whitefire.nit.data.local.WorkDayEntity
import dev.whitefire.nit.domain.model.WeekStats
import dev.whitefire.nit.domain.model.WorkDay
import dev.whitefire.nit.domain.model.WorkWeek
import dev.whitefire.nit.util.toEntity
import dev.whitefire.nit.util.toModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Repository for managing work day data
 */
class WorkDayRepository private constructor(
    private val workDayDao: WorkDayDao
) {
    
    /**
     * Get all work days as a flow
     */
    fun getAllWorkDaysFlow(): Flow<List<WorkDay>> {
        return workDayDao.getAllFlow().map { entities ->
            entities.map { it.toModel() }
        }
    }
    
    /**
     * Get work days for a specific week
     */
    fun getWorkWeekFlow(week: WorkWeek): Flow<WorkWeek> {
        return workDayDao.getByWeekFlow(week.startDate, week.endDate).map { entities ->
            val workDays = entities.map { it.toModel() }
            week.copy(workDays = workDays)
        }
    }
    
    /**
     * Get work day for a specific date
     */
    suspend fun getWorkDay(date: LocalDate): WorkDay? {
        return workDayDao.getByDate(date)?.toModel()
    }
    
    /**
     * Get current week with all work days
     */
    suspend fun getCurrentWeek(): WorkWeek {
        val week = WorkWeek.currentWeek()
        val workDays = workDayDao.getByWeek(week.startDate, week.endDate)
            .map { it.toModel() }
        return week.copy(workDays = workDays)
    }
    
    /**
     * Get work week for a specific date
     */
    suspend fun getWorkWeekForDate(date: LocalDate): WorkWeek {
        val week = WorkWeek.fromDate(date)
        val workDays = workDayDao.getByWeek(week.startDate, week.endDate)
            .map { it.toModel() }
        return week.copy(workDays = workDays)
    }
    
    /**
     * Get recent work days
     */
    suspend fun getRecentWorkDays(limit: Int = 20): List<WorkDay> {
        return workDayDao.getRecent(limit).map { it.toModel() }
    }
    
    /**
     * Save a work day
     */
    suspend fun saveWorkDay(workDay: WorkDay) {
        workDayDao.upsert(workDay.toEntity())
    }
    
    /**
     * Delete a work day by date
     */
    suspend fun deleteWorkDay(date: LocalDate) {
        workDayDao.deleteByDate(date)
    }
    
    /**
     * Delete a work day by ID
     */
    suspend fun deleteWorkDayById(id: String) {
        workDayDao.deleteById(id)
    }
    
    /**
     * Get total hours worked in a date range
     */
    suspend fun getTotalHoursInRange(start: LocalDate, end: LocalDate): Float {
        val workDays = workDayDao.getByDateRange(start, end)
        return workDays.sumOf { it.toModel().effectiveHours.toDouble() }.toFloat()
    }
    
    /**
     * Get statistics for the current week
     */
    suspend fun getCurrentWeekStats(): WeekStats {
        val week = getCurrentWeek()
        val today = LocalDate.now()
        val todayWorkDay = week.workDays.firstOrNull { it.date == today }
        
        return WeekStats(
            totalHours = week.totalNetHours,
            remainingHours = week.remainingHours,
            progressPercentage = week.progressPercentage,
            targetMet = week.isTargetMet,
            todayHours = todayWorkDay?.effectiveHours ?: 0f,
            daysWorked = week.workDays.count { it.isComplete },
            totalDays = week.workDays.size
        )
    }
    
    /**
     * Check if a day has been recorded
     */
    suspend fun hasWorkDay(date: LocalDate): Boolean {
        return workDayDao.exists(date) > 0
    }
    
    companion object {
        @Volatile
        private var instance: WorkDayRepository? = null
        
        fun getInstance(workDayDao: WorkDayDao): WorkDayRepository {
            return instance ?: synchronized(this) {
                instance ?: WorkDayRepository(workDayDao).also { instance = it }
            }
        }
    }
}

// Extension functions for DAO to support Flow
fun WorkDayDao.getAllFlow(): Flow<List<WorkDayEntity>> {
    return flow { emit(getAll()) }
}

fun WorkDayDao.getByWeekFlow(start: LocalDate, end: LocalDate): Flow<List<WorkDayEntity>> {
    return flow { emit(getByWeek(start, end)) }
}
