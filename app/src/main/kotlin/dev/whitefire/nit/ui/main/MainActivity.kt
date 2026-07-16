package dev.whitefire.nit.ui.main

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.whitefire.nit.NitApplication
import dev.whitefire.nit.R
import dev.whitefire.nit.data.repository.UserPreferencesRepository
import dev.whitefire.nit.data.repository.WorkDayRepository
import dev.whitefire.nit.domain.model.WeekStats
import dev.whitefire.nit.domain.model.WorkTimeConfig
import dev.whitefire.nit.domain.model.WorkWeek
import dev.whitefire.nit.ui.history.HistoryActivity
import dev.whitefire.nit.ui.settings.SettingsActivity
import dev.whitefire.nit.util.formatHours
import dev.whitefire.nit.util.formatShortDate
import dev.whitefire.nit.util.formatTime
import dev.whitefire.nit.util.minutesToDisplayString
import dev.whitefire.nit.util.showTimePicker
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            (application as NitApplication).workDayRepository,
            (application as NitApplication).preferencesRepository
        )
    }

    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
    private lateinit var etNotes: EditText
    private lateinit var btnCalculate: Button
    private lateinit var btnDelete: Button
    private lateinit var btnDeleteEntry: Button
    private lateinit var btnDatePrev: Button
    private lateinit var btnDateNext: Button
    private lateinit var btnToday: Button
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var tvDate: TextView
    private lateinit var tvDurationValue: TextView
    private lateinit var tvBreakValue: TextView
    private lateinit var tvTodayWorkedValue: TextView
    private lateinit var tvWeekWorkedValue: TextView
    private lateinit var tvRemainingValue: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProgressText: TextView
    private lateinit var tvSuggestedDaily: TextView
    private lateinit var tvLeaveSuggestion: TextView
    private lateinit var tvMonHours: TextView
    private lateinit var tvTueHours: TextView
    private lateinit var tvWedHours: TextView
    private lateinit var tvThuHours: TextView
    private lateinit var tvFriHours: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etNotes = findViewById(R.id.etNotes)
        btnCalculate = findViewById(R.id.btnCalculate)
        btnDelete = findViewById(R.id.btnDelete)
        btnDeleteEntry = findViewById(R.id.btnDeleteEntry)
        btnDatePrev = findViewById(R.id.btnDatePrev)
        btnDateNext = findViewById(R.id.btnDateNext)
        btnToday = findViewById(R.id.btnToday)
        bottomNav = findViewById(R.id.bottomNav)
        tvDate = findViewById(R.id.tvDate)
        tvDurationValue = findViewById(R.id.tvDurationValue)
        tvBreakValue = findViewById(R.id.tvBreakValue)
        tvTodayWorkedValue = findViewById(R.id.tvTodayWorkedValue)
        tvWeekWorkedValue = findViewById(R.id.tvWeekWorkedValue)
        tvRemainingValue = findViewById(R.id.tvRemainingValue)
        progressBar = findViewById(R.id.progressBar)
        tvProgressText = findViewById(R.id.tvProgressText)
        tvSuggestedDaily = findViewById(R.id.tvSuggestedDaily)
        tvLeaveSuggestion = findViewById(R.id.tvLeaveSuggestion)
        tvMonHours = findViewById(R.id.tvMonHours)
        tvTueHours = findViewById(R.id.tvTueHours)
        tvWedHours = findViewById(R.id.tvWedHours)
        tvThuHours = findViewById(R.id.tvThuHours)
        tvFriHours = findViewById(R.id.tvFriHours)

        setupViews()
        setupObservers()
    }

    private fun setupViews() {
        etStartTime.setOnClickListener {
            etStartTime.showTimePicker(this, viewModel.startTime.value ?: LocalTime.of(9, 30)) { time ->
                viewModel.setStartTime(time)
                updateCalculations()
            }
        }

        etEndTime.setOnClickListener {
            etEndTime.showTimePicker(this, viewModel.endTime.value ?: LocalTime.of(16, 0)) { time ->
                viewModel.setEndTime(time)
                updateCalculations()
            }
        }

        btnCalculate.setOnClickListener {
            viewModel.saveWorkDay()
            showToast("Saved")
        }

        btnDelete.setOnClickListener {
            viewModel.setStartTime(null)
            viewModel.setEndTime(null)
            viewModel.setBreakMinutes(0)
            etNotes.setText("")
            showToast("Cleared")
        }

        btnDeleteEntry.setOnClickListener {
            viewModel.deleteWorkDay()
            showToast("Deleted")
        }

        btnDatePrev.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value.minusDays(1))
        }

        btnDateNext.setOnClickListener {
            viewModel.setDate(viewModel.currentDate.value.plusDays(1))
        }

        btnToday.setOnClickListener {
            viewModel.setDate(LocalDate.now())
        }

        etNotes.addTextChangedListener { text ->
            viewModel.setNotes(text.toString())
        }

        bottomNav.setOnItemSelectedListener { item ->
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
                        tvDate.text = date.formatShortDate()
                    }
                }
                launch {
                    viewModel.startTime.collect { time ->
                        etStartTime.setText(time?.formatTime() ?: "")
                        updateCalculations()
                    }
                }
                launch {
                    viewModel.endTime.collect { time ->
                        etEndTime.setText(time?.formatTime() ?: "")
                        updateCalculations()
                    }
                }
                launch {
                    viewModel.breakMinutes.collect { minutes ->
                        tvBreakValue.text = minutes.minutesToDisplayString()
                    }
                }
                launch {
                    viewModel.notes.collect { notes ->
                        if (etNotes.text.toString() != notes) {
                            etNotes.setText(notes)
                        }
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
                launch {
                    viewModel.currentWeek.collect {
                        updateLeaveSuggestion(viewModel.workTimeConfig.value)
                    }
                }
            }
        }
    }

    private fun updateCalculations() {
        tvDurationValue.text = viewModel.getCurrentDurationString()
        updateLeaveSuggestion(viewModel.workTimeConfig.value)
    }

    private fun updateStats(stats: WeekStats) {
        tvTodayWorkedValue.text = stats.todayHours.formatHours()

        val config = viewModel.workTimeConfig.value ?: return
        tvWeekWorkedValue.text = "${stats.totalHours.formatHours()} / ${config.weeklyTargetHours.formatHours()}"
        tvRemainingValue.text = stats.remainingHours.formatHours()
        progressBar.progress = stats.progressPercentage.toInt()
        tvProgressText.text = "${stats.progressPercentage.toInt()}%"

        tvSuggestedDaily.text = "Suggested daily: ${viewModel.getSuggestedDailyHours().formatHours()}"
    }

    private fun updateWeekDistribution(week: WorkWeek) {
        val days = week.getSortedWorkDays()

        tvMonHours.text = days.firstOrNull { it.date.dayOfWeek.value == 1 }?.effectiveHours?.formatHours() ?: "00:00"
        tvTueHours.text = days.firstOrNull { it.date.dayOfWeek.value == 2 }?.effectiveHours?.formatHours() ?: "00:00"
        tvWedHours.text = days.firstOrNull { it.date.dayOfWeek.value == 3 }?.effectiveHours?.formatHours() ?: "00:00"
        tvThuHours.text = days.firstOrNull { it.date.dayOfWeek.value == 4 }?.effectiveHours?.formatHours() ?: "00:00"
        tvFriHours.text = days.firstOrNull { it.date.dayOfWeek.value == 5 }?.effectiveHours?.formatHours() ?: "00:00"
    }

    private fun updateLeaveSuggestion(config: WorkTimeConfig?) {
        config ?: return

        val leaveTime = viewModel.getSuggestedLeaveTime()
        if (leaveTime != null) {
            tvLeaveSuggestion.text = "Can leave at: ${leaveTime.formatTime()}"
        } else {
            tvLeaveSuggestion.text = "Weekly target met!"
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
