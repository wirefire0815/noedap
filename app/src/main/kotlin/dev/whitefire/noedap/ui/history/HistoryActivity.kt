package dev.whitefire.noedap.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dev.whitefire.noedap.NoedapApplication
import dev.whitefire.noedap.databinding.ActivityHistoryBinding
import dev.whitefire.noedap.util.formatHours
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as NoedapApplication).workDayRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()

        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = WorkDayAdapter(
            onDeleteClick = { workDay ->
                viewModel.deleteWorkDay(workDay)
            }
        )
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.workDays.collect { workDays ->
                        (binding.recyclerView.adapter as? WorkDayAdapter)?.submitList(workDays)
                        updateStats(workDays)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateStats(workDays: List<dev.whitefire.noedap.domain.model.WorkDay>) {
        val stats = viewModel.getWeekStats(workDays)
        binding.tvTotalHours.text = stats.totalHours.formatHours()
        binding.tvDaysWorked.text = "${stats.daysWorked} days"
    }
}

class WorkDayAdapter(
    private val onDeleteClick: (dev.whitefire.noedap.domain.model.WorkDay) -> Unit
) : androidx.recyclerview.widget.ListAdapter<dev.whitefire.noedap.domain.model.WorkDay, WorkDayViewHolder>(WorkDayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkDayViewHolder {
        val binding = dev.whitefire.noedap.databinding.ItemWorkDayBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WorkDayViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WorkDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class WorkDayViewHolder(
    private val binding: dev.whitefire.noedap.databinding.ItemWorkDayBinding,
    private val onDeleteClick: (dev.whitefire.noedap.domain.model.WorkDay) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    fun bind(workDay: dev.whitefire.noedap.domain.model.WorkDay) {
        binding.tvDate.text = workDay.getDateDisplay()
        binding.tvDuration.text = workDay.getDurationString()
        binding.tvNotes.text = workDay.notes.ifEmpty { "No notes" }

        binding.btnDelete.setOnClickListener { onDeleteClick(workDay) }
    }
}

class WorkDayDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<dev.whitefire.noedap.domain.model.WorkDay>() {
    override fun areItemsTheSame(oldItem: dev.whitefire.noedap.domain.model.WorkDay, newItem: dev.whitefire.noedap.domain.model.WorkDay): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: dev.whitefire.noedap.domain.model.WorkDay, newItem: dev.whitefire.noedap.domain.model.WorkDay): Boolean {
        return oldItem == newItem
    }
}
