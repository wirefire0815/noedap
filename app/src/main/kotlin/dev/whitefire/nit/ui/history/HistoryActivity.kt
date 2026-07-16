package dev.whitefire.nit.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import dev.whitefire.nit.NitApplication
import dev.whitefire.nit.R
import dev.whitefire.nit.domain.model.WorkDay
import dev.whitefire.nit.ui.history.HistoryViewModel.SimpleWeekStats
import dev.whitefire.nit.util.formatHours
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvTotalHours: TextView
    private lateinit var tvDaysWorked: TextView

    private val viewModel: HistoryViewModel by viewModels {
        HistoryViewModelFactory((application as NitApplication).workDayRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        tvTotalHours = findViewById(R.id.tvTotalHours)
        tvDaysWorked = findViewById(R.id.tvDaysWorked)

        setupRecyclerView()
        setupObservers()

        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = WorkDayAdapter(
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
                        (recyclerView.adapter as? WorkDayAdapter)?.submitList(workDays)
                        updateStats(workDays)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun updateStats(workDays: List<WorkDay>) {
        val stats = viewModel.getWeekStats(workDays)
        tvTotalHours.text = stats.totalHours.formatHours()
        tvDaysWorked.text = "${stats.daysWorked} days"
    }
}

class WorkDayAdapter(
    private val onDeleteClick: (WorkDay) -> Unit
) : androidx.recyclerview.widget.ListAdapter<WorkDay, WorkDayViewHolder>(WorkDayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkDayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_work_day, parent, false)
        return WorkDayViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: WorkDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class WorkDayViewHolder(
    itemView: View,
    private val onDeleteClick: (WorkDay) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
    private val tvNotes: TextView = itemView.findViewById(R.id.tvNotes)
    private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

    fun bind(workDay: WorkDay) {
        tvDate.text = workDay.getDateDisplay()
        tvDuration.text = workDay.getDurationString()
        tvNotes.text = workDay.notes.ifEmpty { "No notes" }

        btnDelete.setOnClickListener { onDeleteClick(workDay) }
    }
}

class WorkDayDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<WorkDay>() {
    override fun areItemsTheSame(oldItem: WorkDay, newItem: WorkDay): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: WorkDay, newItem: WorkDay): Boolean {
        return oldItem == newItem
    }
}
