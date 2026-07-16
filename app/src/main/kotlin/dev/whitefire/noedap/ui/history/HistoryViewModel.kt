package dev.whitefire.noedap.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.whitefire.noedap.data.repository.WorkDayRepository
import dev.whitefire.noedap.domain.model.WorkDay
import dev.whitefire.noedap.domain.model.WorkWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryViewModel(
    private val workDayRepository: WorkDayRepository
) : ViewModel() {

    private val _workDays = MutableStateFlow<List<WorkDay>>(emptyList())
    val workDays: StateFlow<List<WorkDay>> = _workDays.asStateFlow()

    private val _selectedWeek = MutableStateFlow<WorkWeek?>(null)
    val selectedWeek: StateFlow<WorkWeek?> = _selectedWeek.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadWorkDays()
    }

    private fun loadWorkDays() {
        viewModelScope.launch {
            _isLoading.value = true
            val days = workDayRepository.getRecentWorkDays(50)
            _workDays.value = days.sortedByDescending { it.date }
            _isLoading.value = false
        }
    }

    fun loadWeek(year: Int, week: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            val workWeek = WorkWeek.fromYearWeek(year, week)
            workWeek?.let {
                val days = workDayRepository.getWorkWeekForDate(it.startDate)
                _selectedWeek.value = days
            }
            _isLoading.value = false
        }
    }

    fun deleteWorkDay(workDay: WorkDay) {
        viewModelScope.launch {
            workDayRepository.deleteWorkDayById(workDay.id)
            loadWorkDays()
        }
    }

    fun getWeekStats(workDays: List<WorkDay>): WeekStats {
        val totalHours = workDays.sumOf { it.effectiveHours }
        val daysWorked = workDays.count { it.isComplete }
        val totalDays = workDays.size
        return WeekStats(totalHours, daysWorked, totalDays)
    }

    data class WeekStats(
        val totalHours: Float,
        val daysWorked: Int,
        val totalDays: Int
    )
}

class HistoryViewModelFactory(
    private val workDayRepository: WorkDayRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(workDayRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
