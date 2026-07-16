package dev.whitefire.noedap.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.whitefire.noedap.data.repository.UserPreferencesRepository
import dev.whitefire.noedap.domain.model.WorkTimeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _config = MutableStateFlow<WorkTimeConfig?>(null)
    val config: StateFlow<WorkTimeConfig?> = _config.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            preferencesRepository.workTimeConfigFlow.collect { config ->
                _config.value = config
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(config: WorkTimeConfig) {
        viewModelScope.launch {
            preferencesRepository.setWorkTimeConfig(config)
            _config.value = config
            _saveSuccess.value = true
        }
    }

    fun setWeeklyTarget(target: Float) {
        _config.value?.let { current ->
            _config.value = current.copy(weeklyTargetHours = target)
        }
    }

    fun setCoreTime(dayOfWeek: java.time.DayOfWeek, start: LocalTime, end: LocalTime) {
        _config.value?.let { current ->
            val newCoreTimes = current.coreTimes.toMutableMap()
            newCoreTimes[dayOfWeek] = WorkTimeConfig.CoreTime(start, end)
            _config.value = current.copy(coreTimes = newCoreTimes)
        }
    }

    fun setBreakRule(afterHours: Float, durationHours: Float) {
        _config.value?.let { current ->
            _config.value = current.copy(
                breakRules = listOf(WorkTimeConfig.BreakRule(afterHours, durationHours))
            )
        }
    }

    fun resetToDefaults() {
        _config.value = WorkTimeConfig()
    }
}

class SettingsViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
