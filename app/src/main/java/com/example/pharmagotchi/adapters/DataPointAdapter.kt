package com.example.pharmagotchi.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.databinding.ItemDataPointBinding
import com.example.pharmagotchi.models.DataPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class DataPointAdapter(
    private val dataPoints: List<DataPoint>,
    private val unit: String
) : RecyclerView.Adapter<DataPointAdapter.DataPointViewHolder>() {

    class DataPointViewHolder(val binding: ItemDataPointBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataPointViewHolder {
        val binding = ItemDataPointBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DataPointViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataPointViewHolder, position: Int) {
        val dataPoint = dataPoints[position]
        holder.binding.dataPointValue.text = "${dataPoint.value} $unit"
        holder.binding.dataPointTime.text = getRelativeTime(dataPoint.timestamp)
    }

    override fun getItemCount() = dataPoints.size

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = abs(now - timestamp)

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days day${if (days > 1) "s" else ""} ago"
            hours > 0 -> "$hours hour${if (hours > 1) "s" else ""} ago"
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} ago"
            else -> "Just now"
        }
    }
}
