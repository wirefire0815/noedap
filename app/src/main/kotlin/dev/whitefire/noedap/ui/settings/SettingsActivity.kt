package dev.whitefire.noedap.ui.settings

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.whitefire.noedap.NoedapApplication
import dev.whitefire.noedap.databinding.ActivitySettingsBinding
import dev.whitefire.noedap.util.formatTime
import dev.whitefire.noedap.util.showTimePicker
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((application as NoedapApplication).preferencesRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        setupObservers()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupViews() {
        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnReset.setOnClickListener { resetToDefaults() }

        binding.etWeeklyTarget.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateWeeklyTarget()
        }

        binding.etBreakAfter.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateBreakRule()
        }

        binding.etBreakDuration.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) updateBreakRule()
        }

        binding.btnMonStart.setOnClickListener {
            showTimePicker(binding.btnMonStart, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.start ?: LocalTime.of(9, 30)) { time ->
                viewModel.setCoreTime(DayOfWeek.MONDAY, time, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.end ?: LocalTime.of(16, 0))
            }
        }

        binding.btnMonEnd.setOnClickListener {
            showTimePicker(binding.btnMonEnd, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.end ?: LocalTime.of(16, 0)) { time ->
                viewModel.setCoreTime(DayOfWeek.MONDAY, viewModel.config.value?.coreTimes?.get(DayOfWeek.MONDAY)?.start ?: LocalTime.of(9, 30), time)
            }
        }

        binding.btnFriStart.setOnClickListener {
            showTimePicker(binding.btnFriStart, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.start ?: LocalTime.of(9, 30)) { time ->
                viewModel.setCoreTime(DayOfWeek.FRIDAY, time, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.end ?: LocalTime.of(12, 30))
            }
        }

        binding.btnFriEnd.setOnClickListener {
            showTimePicker(binding.btnFriEnd, viewModel.config.value?.coreTimes?.get(DayOfWeek.FRIDAY)?.end ?: LocalTime.of(12, 30)) { time ->
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
        binding.etWeeklyTarget.setText(config.weeklyTargetHours.toString())
        binding.etBreakAfter.setText(config.breakRules.firstOrNull()?.afterHours?.toString() ?: "6")
        binding.etBreakDuration.setText(config.breakRules.firstOrNull()?.durationHours?.toString() ?: "0.5")

        binding.btnMonStart.text = config.coreTimes[DayOfWeek.MONDAY]?.start?.formatTime() ?: "09:30"
        binding.btnMonEnd.text = config.coreTimes[DayOfWeek.MONDAY]?.end?.formatTime() ?: "16:00"
        binding.btnFriStart.text = config.coreTimes[DayOfWeek.FRIDAY]?.start?.formatTime() ?: "09:30"
        binding.btnFriEnd.text = config.coreTimes[DayOfWeek.FRIDAY]?.end?.formatTime() ?: "12:30"
    }

    private fun updateWeeklyTarget() {
        binding.etWeeklyTarget.text?.toString()?.toFloatOrNull()?.let { target ->
            viewModel.setWeeklyTarget(target)
        }
    }

    private fun updateBreakRule() {
        val after = binding.etBreakAfter.text?.toString()?.toFloatOrNull() ?: 6f
        val duration = binding.etBreakDuration.text?.toString()?.toFloatOrNull() ?: 0.5f
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
