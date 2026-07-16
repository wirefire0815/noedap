package dev.whitefire.nit.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.appbar.MaterialToolbar
import dev.whitefire.nit.NitApplication
import dev.whitefire.nit.R
import dev.whitefire.nit.domain.model.WorkTimeConfig
import dev.whitefire.nit.util.formatTime
import dev.whitefire.nit.util.showTimePicker
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var etWeeklyTarget: EditText
    private lateinit var etBreakAfter: EditText
    private lateinit var etBreakDuration: EditText
    private lateinit var btnMonStart: Button
    private lateinit var btnMonEnd: Button
    private lateinit var btnFriStart: Button
    private lateinit var btnFriEnd: Button

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((application as NitApplication).preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        toolbar = findViewById(R.id.toolbar)
        btnSave = findViewById(R.id.btnSave)
        btnReset = findViewById(R.id.btnReset)
        etWeeklyTarget = findViewById(R.id.etWeeklyTarget)
        etBreakAfter = findViewById(R.id.etBreakAfter)
        etBreakDuration = findViewById(R.id.etBreakDuration)
        btnMonStart = findViewById(R.id.btnMonStart)
        btnMonEnd = findViewById(R.id.btnMonEnd)
        btnFriStart = findViewById(R.id.btnFriStart)
        btnFriEnd = findViewById(R.id.btnFriEnd)

        setupViews()
        setupObservers()

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        btnSave.setOnClickListener { saveSettings() }
        btnReset.setOnClickListener { resetToDefaults() }

        etWeeklyTarget.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateWeeklyTarget()
        }

        etBreakAfter.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateBreakRule()
        }

        etBreakDuration.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateBreakRule()
        }

        btnMonStart.setOnClickListener {
            btnMonStart.showTimePicker(this, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.start ?: LocalTime.of(9, 30)) { time ->
                viewModel.setCoreTime(DayOfWeek.MONDAY, time, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.end ?: LocalTime.of(16, 0))
            }
        }

        btnMonEnd.setOnClickListener {
            btnMonEnd.showTimePicker(this, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.end ?: LocalTime.of(16, 0)) { time ->
                viewModel.setCoreTime(DayOfWeek.MONDAY, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.start ?: LocalTime.of(9, 30), time)
            }
        }

        btnFriStart.setOnClickListener {
            btnFriStart.showTimePicker(this, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.start ?: LocalTime.of(9, 30)) { time ->
                viewModel.setCoreTime(DayOfWeek.FRIDAY, time, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.end ?: LocalTime.of(12, 30))
            }
        }

        btnFriEnd.setOnClickListener {
            btnFriEnd.showTimePicker(this, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.end ?: LocalTime.of(12, 30)) { time ->
                viewModel.setCoreTime(DayOfWeek.FRIDAY, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.start ?: LocalTime.of(9, 30), time)
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.config.collect { config ->
                        config?.let { updateUi(it) }
                    }
                }
                launch {
                    viewModel.saveSuccess.collect { success ->
                        if (success) showToast("Settings saved")
                    }
                }
            }
        }
    }

    private fun updateUi(config: WorkTimeConfig) {
        etWeeklyTarget.setText(config.weeklyTargetHours.toString())
        etBreakAfter.setText(config.breakRules.firstOrNull()?.afterHours?.toString() ?: "6")
        etBreakDuration.setText(config.breakRules.firstOrNull()?.durationHours?.toString() ?: "0.5")

        btnMonStart.text = config.coreTimes[DayOfWeek.MONDAY]?.start?.formatTime() ?: "09:30"
        btnMonEnd.text = config.coreTimes[DayOfWeek.MONDAY]?.end?.formatTime() ?: "16:00"
        btnFriStart.text = config.coreTimes[DayOfWeek.FRIDAY]?.start?.formatTime() ?: "09:30"
        btnFriEnd.text = config.coreTimes[DayOfWeek.FRIDAY]?.end?.formatTime() ?: "12:30"
    }

    private fun updateWeeklyTarget() {
        etWeeklyTarget.text?.toString()?.toFloatOrNull()?.let { target ->
            viewModel.setWeeklyTarget(target)
        }
    }

    private fun updateBreakRule() {
        val after = etBreakAfter.text?.toString()?.toFloatOrNull() ?: 6f
        val duration = etBreakDuration.text?.toString()?.toFloatOrNull() ?: 0.5f
        viewModel.setBreakRule(after, duration)
    }

    private fun saveSettings() {
        viewModel.config.value?.let { config ->
            viewModel.saveConfig(config)
        }
    }

    private fun resetToDefaults() {
        viewModel.resetToDefaults()
        viewModel.config.value?.let { config ->
            viewModel.saveConfig(config)
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}
