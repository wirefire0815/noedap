package dev.whitefire.noedap.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.whitefire.noedap.NoedapApplication
import dev.whitefire.noedap.databinding.ActivityMainBinding
import dev.whitefire.noedap.ui.history.HistoryActivity
import dev.whitefire.noedap.ui.settings.SettingsActivity
import dev.whitefire.noedap.util.formatHours
import dev.whitefire.noedap.util.formatShortDate
import dev.whitefire.noedap.util.showTimePicker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as NoedapApplication).workDayRepository,
            (application as NoedapApplication).preferencesRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        binding.etStartTime.setOnClickListener {
            it.showTimePicker(this, viewModel.startTime.value ?: LocalTime.of(9, 30)) { time ->
                viewModel.setStartTime(time)
                updateCalculations()
            }
        }

        binding.etEndTime.setOnClickListener {
            it.showTimePicker(this, viewModel.endTime.value ?: LocalTime.of(16, 0)) { time ->
                viewModel.setEndTime(time)
                updateCalculations()
            }
        }

        binding.btnCalculate.setOnClickListener {
            viewModel.saveWorkDay(binding.etNotes.text.toString())
            showToast("Saved")
        }

        binding.btnDelete.setOnClickListener {
            viewModel.setStartTime(null)
            viewModel.setEndTime(null)
            viewModel.setBreakMinutes(0)
            binding.etNotes.setText("")
            showToast("Cleared")
        }

        binding.btnDeleteEntry.setOnClickListener {
            viewModel.deleteWorkDay()
            showToast("Deleted")
        }

        binding.btnDatePrev.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value.minusDays(1))
        }

        binding.btnDateNext.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value.plusDays(1))
        }

        binding.btnToday.setOnClickListener {
            viewModel.setDate(LocalDate.now())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_main -> true
                R.id.nav_history -> {
                    startActivity(android.content.Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentDate.collect { date ->
                        binding.tvDate.text = date.formatShortDate()
                    }
                }
                launch {
                    viewModel.startTime.collect { time ->
                        binding.etStartTime.setText(time?.formatTime() ?: "")
                        updateCalculations()
                    }
                }
                launch {
                    viewModel.endTime.collect { time ->
                        binding.etEndTime.setText(time?.formatTime() ?: "")
                        updateCalculations()
                    }
                }
                launch {
                    viewModel.breakMinutes.collect { minutes ->
                        binding.tvBreakValue.text = minutes.minutesToDisplayString()
                    }
                }
                launch {
                    viewModel.currentWeek.collect { week ->
                        week?.let { updateWeekDistribution(it) }
                    }
                }
                launch {
                    viewModel.stats.collect { stats ->
                        stats?.let { updateStats(it) }
                    }
                }
                launch {
                    viewModel.workTimeConfig.collect { config ->
                        config?.let { updateLeaveSuggestion(it) }
                    }
                }
            }
        }
    }

    private fun updateCalculations() {
        binding.tvDurationValue.text = viewModel.getCurrentDurationString()
        updateLeaveSuggestion(viewModel.workTimeConfig.value)
    }

    private fun updateStats(stats: WorkDayRepository.WeekStats) {
        binding.tvTodayWorkedValue.text = stats.todayHours.formatHours()
        
        val config = viewModel.workTimeConfig.value ?: return
        binding.tvWeekWorkedValue.text = "${stats.totalHours.formatHours()} / ${config.weeklyTargetHours.formatHours()}"
        binding.tvRemainingValue.text = stats.remainingHours.formatHours()
        binding.progressBar.progress = stats.progressPercentage.toInt()
        binding.tvProgressText.text = "${stats.progressPercentage.toInt()}%"
        
        binding.tvSuggestedDaily.text = "Suggested daily: ${viewModel.getSuggestedDailyHours().formatHours()}"
    }

    private fun updateWeekDistribution(week: WorkWeek) {
        val days = week.getSortedWorkDays()
        
        binding.tvMonHours.text = days.firstOrNull { it.date.dayOfWeek.value == 1 }?.effectiveHours?.formatHours() ?: "00:00"
        binding.tvTueHours.text = days.firstOrNull { it.date.dayOfWeek.value == 2 }?.effectiveHours?.formatHours() ?: "00:00"
        binding.tvWedHours.text = days.firstOrNull { it.date.dayOfWeek.value == 3 }?.effectiveHours?.formatHours() ?: "00:00"
        binding.tvThuHours.text = days.firstOrNull { it.date.dayOfWeek.value == 4 }?.effectiveHours?.formatHours() ?: "00:00"
        binding.tvFriHours.text = days.firstOrNull { it.date.dayOfWeek.value == 5 }?.effectiveHours?.formatHours() ?: "00:00"
    }

    private fun updateLeaveSuggestion(config: WorkTimeConfig?) {
        config ?: return
        
        val leaveTime = viewModel.getSuggestedLeaveTime()
        if (leaveTime != null) {
            binding.tvLeaveSuggestion.text = "Can leave at: ${leaveTime.formatTime()}"
        } else {
            binding.tvLeaveSuggestion.text = "Weekly target met!"
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}

class MainViewModelFactory(
    private val workDayRepository: WorkDayRepository,
    private val preferencesRepository: UserPreferencesRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(workDayRepository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
