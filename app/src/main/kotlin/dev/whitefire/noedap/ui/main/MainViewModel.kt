package dev.whitefire.noedap.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.whitefire.noedap.data.repository.UserPreferencesRepository
import dev.whitefire.noedap.data.repository.WorkDayRepository
import dev.whitefire.noedap.domain.model.WorkDay
import dev.whitefire.noedap.domain.model.WorkTimeConfig
import dev.whitefire.noedap.domain.model.WorkWeek
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * ViewModel for the main screen
 */
class MainViewModel(
    private val workDayRepository: WorkDayRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    
    // State for the current date being edited
    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate = _currentDate.asStateFlow()
    
    // State for start and end times
    private val _startTime = MutableStateFlow<LocalTime?>(null)
    val startTime = _startTime.asStateFlow()
    
    private val _endTime = MutableStateFlow<LocalTime?>(null)
    val endTime = _endTime.asStateFlow()
    
    // State for break minutes
    private val _breakMinutes = MutableStateFlow(0)
    val breakMinutes = _breakMinutes.asStateFlow()
    
    // State for notes
    private val _notes = MutableStateFlow("")
    val notes = _notes.asStateFlow()
    
    // State for auto-calculate break
    private val _autoCalculateBreak = MutableStateFlow(true)
    val autoCalculateBreak = _autoCalculateBreak.asStateFlow()
    
    // Current week state
    private val _currentWeek = MutableStateFlow<WorkWeek?>(null)
    val currentWeek: StateFlow<WorkWeek?> = _currentWeek.asStateFlow()
    
    // Work time configuration
    private val _workTimeConfig = MutableStateFlow<WorkTimeConfig?>(null)
    val workTimeConfig: StateFlow<WorkTimeConfig?> = _workTimeConfig.asStateFlow()
    
    // UI state
    sealed class UiState {
        object Loading : UiState()
        object Error : UiState()
        object Success : UiState()
    }
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // Stats
    private val _stats = MutableStateFlow<WorkDayRepository.WeekStats?>(null)
    val stats: StateFlow<WorkDayRepository.WeekStats?> = _stats.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            try {
                // Load work time config
                preferencesRepository.workTimeConfigFlow
                    .collect { config ->
                        _workTimeConfig.value = config
                        _autoCalculateBreak.value = preferencesRepository.autoCalculateBreakFlow.first()
                    }
                
                // Load current week
                val week = workDayRepository.getCurrentWeek()
                _currentWeek.value = week
                _stats.value = workDayRepository.getCurrentWeekStats()
                
                // Load today's work day if it exists
                val todayWorkDay = workDayRepository.getWorkDay(LocalDate.now())
                todayWorkDay?.let { workDay ->
                    _startTime.value = workDay.startTime
                    _endTime.value = workDay.endTime
                    _breakMinutes.value = workDay.breakMinutes
                    _notes.value = workDay.notes
                }
                
                _uiState.value = UiState.Success
            } catch (e: Exception) {
                _uiState.value = UiState.Error
            }
        }
    }
    
    private fun refreshWeekStats() {
        viewModelScope.launch {
            val week = workDayRepository.getCurrentWeek()
            _currentWeek.value = week
            _stats.value = workDayRepository.getCurrentWeekStats()
        }
    }
    
    /**
     * Get stats from current week (including preview inputs)
     */
    fun getCurrentWeekStatsPreview(): WeekStats {
        val week = _currentWeek.value ?: return WeekStats(0f, 0f, 0f, false, 0f, 0, 0)
        val config = _workTimeConfig.value ?: return WeekStats(0f, 0f, 0f, false, 0f, 0, 0)
        
        val today = LocalDate.now()
        val todayWorkDay = week.workDays.firstOrNull { it.date == today }
        val todayHours = todayWorkDay?.effectiveHours ?: 0f
        
        val totalHours = week.totalNetHours
        val remaining = config.weeklyTargetHours - totalHours
        val progress = (totalHours / config.weeklyTargetHours * 100f).coerceAtMost(100f)
        val targetMet = totalHours >= config.weeklyTargetHours
        val daysWorked = week.workDays.count { it.isComplete }
        
        return WeekStats(
            totalHours = totalHours,
            remainingHours = remaining,
            progressPercentage = progress,
            targetMet = targetMet,
            todayHours = todayHours,
            daysWorked = daysWorked,
            totalDays = week.workDays.size
        )
    }
    
    fun setDate(date: LocalDate) {
        _currentDate.value = date
        viewModelScope.launch {
            val workDay = workDayRepository.getWorkDay(date)
            workDay?.let { day ->
                _startTime.value = day.startTime
                _endTime.value = day.endTime
                _breakMinutes.value = day.breakMinutes
                _notes.value = day.notes
            } ?: run {
                _startTime.value = null
                _endTime.value = null
                _breakMinutes.value = 0
                _notes.value = ""
            }
            
            // Load the week for this date
            val week = workDayRepository.getWorkWeekForDate(date)
            _currentWeek.value = week
            _stats.value = workDayRepository.getCurrentWeekStats()
        }
    }
    
    fun setStartTime(time: LocalTime?) {
        _startTime.value = time
        updateCurrentWeekWithInputs(_currentWeek.value ?: return)
    }
    
    fun setEndTime(time: LocalTime?) {
        _endTime.value = time
        // Auto-calculate break if enabled
        if (_autoCalculateBreak.value) {
            calculateBreak()
        } else {
            updateCurrentWeekWithInputs(_currentWeek.value ?: return)
        }
    }
    
    fun setBreakMinutes(minutes: Int) {
        _breakMinutes.value = minutes.coerceAtLeast(0)
        _currentWeek.value?.let { updateCurrentWeekWithInputs(it) }
    }
    
    fun setNotes(notes: String) {
        _notes.value = notes
        _currentWeek.value?.let { updateCurrentWeekWithInputs(it) }
    }
    
    private fun calculateBreak() {
        val start = _startTime.value
        val end = _endTime.value
        val config = _workTimeConfig.value ?: return
        
        if (start != null && end != null) {
            val duration = java.time.Duration.between(start, end)
            if (duration.isNegative) return
            
            val hours = duration.toMinutes().toFloat() / 60f
            var totalBreak = 0
            
            for (rule in config.breakRules) {
                if (hours >= rule.afterHours) {
                    totalBreak += (rule.durationHours * 60).toInt()
                }
            }
            
            _breakMinutes.value = totalBreak
            _currentWeek.value?.let { updateCurrentWeekWithInputs(it) }
        }
    }
    
    private fun updateCurrentWeekWithInputs(week: WorkWeek) {
        val currentDate = _currentDate.value
        val start = _startTime.value
        val end = _endTime.value
        val breakMin = _breakMinutes.value
        val notes = _notes.value
        
        if (start != null && end != null && currentDate != null) {
            val newDay = WorkDay(
                date = currentDate,
                startTime = start,
                endTime = end,
                breakMinutes = breakMin,
                notes = notes
            )
            _currentWeek.value = week.withWorkDay(newDay)
        }
    }
    
    fun setAutoCalculateBreak(enabled: Boolean) {
        _autoCalculateBreak.value = enabled
        viewModelScope.launch {
            preferencesRepository.setAutoCalculateBreak(enabled)
        }
        if (enabled) {
            calculateBreak()
        }
    }
    
    /**
     * Save the current work day
     */
    fun saveWorkDay() {
        viewModelScope.launch {
            val date = _currentDate.value
            val start = _startTime.value
            val end = _endTime.value
            val breakMin = _breakMinutes.value
            val notes = _notes.value
            
            val workDay = WorkDay(
                date = date,
                startTime = start,
                endTime = end,
                breakMinutes = breakMin,
                notes = notes
            )
            
            workDayRepository.saveWorkDay(workDay)
            refreshWeekStats()
        }
    }
    
    /**
     * Delete work day for current date
     */
    fun deleteWorkDay() {
        viewModelScope.launch {
            val date = _currentDate.value
            workDayRepository.deleteWorkDay(date)
            
            // Reset current day inputs
            _startTime.value = null
            _endTime.value = null
            _breakMinutes.value = 0
            _notes.value = ""
            
            refreshWeekStats()
        }
    }
    
    /**
     * Calculate remaining time needed for today to meet daily target
     */
    fun getTodayRemainingHours(): Float {
        val config = _workTimeConfig.value ?: return 0f
        val target = config.weeklyTargetHours / 5f // Rough daily target
        val current = _startTime.value?.let { start ->
            _endTime.value?.let { end ->
                val duration = java.time.Duration.between(start, end)
                if (duration.isNegative) 0f
                else (duration.toMinutes() - _breakMinutes.value).toFloat() / 60f
            } ?: 0f
        } ?: 0f
        
        return (target - current).coerceAtLeast(0f)
    }
    
    /**
     * Check if required breaks are satisfied
     */
    fun isBreakSufficient(): Boolean {
        val config = _workTimeConfig.value ?: return true
        val start = _startTime.value
        val end = _endTime.value
        
        if (start == null || end == null) return true
        
        val duration = java.time.Duration.between(start, end)
        if (duration.isNegative) return true
        
        val hours = duration.toMinutes().toFloat() / 60f
        
        for (rule in config.breakRules) {
            if (hours >= rule.afterHours) {
                val requiredBreak = (rule.durationHours * 60).toInt()
                if (_breakMinutes.value < requiredBreak) {
                    return false
                }
            }
        }
        
        return true
    }
    
    /**
     * Get formatted duration string for current inputs
     */
    fun getCurrentDurationString(): String {
        val start = _startTime.value
        val end = _endTime.value
        
        if (start == null || end == null) return "00:00"
        
        val duration = java.time.Duration.between(start, end)
        if (duration.isNegative) return "00:00"
        
        val grossHours = duration.toHours()
        val grossMinutes = duration.toMinutesPart()
        
        val netDuration = duration.minusMinutes(_breakMinutes.value.toLong())
        val netHours = netDuration.toHours()
        val netMinutes = netDuration.toMinutesPart()
        
        return if (_breakMinutes.value > 0) {
            String.format("%02d:%02d (net: %02d:%02d)", 
                grossHours, grossMinutes, netHours, netMinutes)
        } else {
            String.format("%02d:%02d", grossHours, grossMinutes)
        }
    }
    
    /**
     * Calculate when you can leave today to meet weekly target
     * Returns LocalTime or null if target already met
     */
    fun getSuggestedLeaveTime(): LocalTime? {
        val config = _workTimeConfig.value ?: return null
        val start = _startTime.value ?: return null
        val currentDate = _currentDate.value
        
        // Get preview stats from current week
        val previewStats = getCurrentWeekStatsPreview()
        val remainingHours = previewStats.remainingHours
        
        if (remainingHours <= 0) return null // Target already met
        
        // Calculate current today hours from inputs
        val currentTodayHours = _startTime.value?.let { s ->
            _endTime.value?.let { e ->
                val dur = java.time.Duration.between(s, e)
                if (!dur.isNegative) (dur.toMinutes() - _breakMinutes.value).toFloat() / 60f
                else 0f
            } ?: 0f
        } ?: 0f
        
        // Calculate hours needed from current today hours
        val hoursNeededFromNow = remainingHours - currentTodayHours
        if (hoursNeededFromNow <= 0) return null
        
        // Calculate leave time: start + (currentTodayHours + hoursNeededFromNow) + break
        val totalHours = currentTodayHours + hoursNeededFromNow
        val minutesNeeded = (totalHours * 60).toInt()
        val leaveTime = start.plusMinutes(minutesNeeded.toLong())
        
        // Add break if needed
        val breakRule = config.breakRules.firstOrNull()
        breakRule?.let { rule ->
            if (totalHours >= rule.afterHours) {
                return leaveTime.plusMinutes((rule.durationHours * 60).toLong())
            }
        }
        
        return leaveTime
    }
    
    /**
     * Get suggested hours for remaining days to meet target
     */
    fun getSuggestedDailyHours(): Float {
        val config = _workTimeConfig.value ?: return 0f
        val stats = _stats.value ?: return 0f
        val currentDate = _currentDate.value
        
        if (stats.remainingHours <= 0) return 0f
        
        // Count remaining work days in week (Mon-Fri)
        val week = _currentWeek.value ?: return 0f
        val remainingDays = (1..5).count { dayOfWeek ->
            val date = week.startDate.plusDays(dayOfWeek.toLong() - 1)
            date >= currentDate && date.dayOfWeek.value in 1..5
        }
        
        if (remainingDays <= 0) return 0f
        
        return stats.remainingHours / remainingDays
    }
    
    /**
     * Get distribution of hours across week
     */
    fun getWeekDistribution(): List<DayDistribution> {
        val week = _currentWeek.value ?: return emptyList()
        val stats = _stats.value ?: return emptyList()
        
        val days = mutableListOf<DayDistribution>()
        val startDate = week.startDate
        
        for (i in 0..6) {
            val date = startDate.plusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek
            
            // Skip weekends by default
            if (dayOfWeek.value > 5) continue
            
            val workDay = week.workDays.firstOrNull { it.date == date }
            val hours = workDay?.effectiveHours ?: 0f
            val isPast = date.isBefore(LocalDate.now())
            
            days.add(DayDistribution(date, dayOfWeek, hours, isPast))
        }
        
        return days
    }
    
    data class DayDistribution(
        val date: LocalDate,
        val dayOfWeek: java.time.DayOfWeek,
        val hours: Float,
        val isPast: Boolean
    )
}
