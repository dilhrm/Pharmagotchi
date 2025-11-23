package com.example.pharmagotchi.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pharmagotchi.databinding.ItemGraphBlockBinding
import com.example.pharmagotchi.models.DataPoint
import com.example.pharmagotchi.models.GraphMetadata
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Color

data class GraphWithData(
    val metadata: GraphMetadata,
    val recentDataPoints: List<DataPoint>
)

class GraphBlockAdapter(
    private val graphs: List<GraphWithData>,
    private val onAddDataClick: (GraphMetadata) -> Unit,
    private val onDeleteClick: (GraphMetadata) -> Unit
) : RecyclerView.Adapter<GraphBlockAdapter.GraphBlockViewHolder>() {

    private val expandedStates = mutableMapOf<Long, Boolean>()

    class GraphBlockViewHolder(val binding: ItemGraphBlockBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphBlockViewHolder {
        val binding = ItemGraphBlockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GraphBlockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GraphBlockViewHolder, position: Int) {
        val graphWithData = graphs[position]
        val graph = graphWithData.metadata
        val dataPoints = graphWithData.recentDataPoints
        val isExpanded = expandedStates[graph.id] ?: false

        // Set graph name
        holder.binding.graphName.text = graph.name

        // Set latest value
        if (dataPoints.isNotEmpty()) {
            val latestValue = dataPoints.first()
            holder.binding.graphLatestValue.text = "${latestValue.value} ${graph.unit}"
        } else {
            holder.binding.graphLatestValue.text = "No data yet"
        }

        // Set expanded state
        holder.binding.graphExpandedContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.binding.divider.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Rotate expand icon
        holder.binding.expandIcon.rotation = if (isExpanded) 180f else 0f

        // Setup recent data list and chart
        if (isExpanded && dataPoints.isNotEmpty()) {
            holder.binding.recentDataList.layoutManager = LinearLayoutManager(holder.binding.root.context)
            holder.binding.recentDataList.adapter = DataPointAdapter(dataPoints, graph.unit)
            holder.binding.graphPlaceholder.visibility = View.GONE
            holder.binding.chart.visibility = View.VISIBLE
            
            setupChart(holder.binding.chart, dataPoints, graph)
        } else if (isExpanded) {
            holder.binding.graphPlaceholder.visibility = View.VISIBLE
            holder.binding.chart.visibility = View.GONE
            holder.binding.graphPlaceholder.text = "No data yet\nTap 'Add Data' to start tracking"
        }

        // Click listener for header
        holder.binding.graphHeader.setOnClickListener {
            expandedStates[graph.id] = !isExpanded
            notifyItemChanged(position)
        }

        // Button click listeners
        holder.binding.addDataButton.setOnClickListener {
            onAddDataClick(graph)
        }

        holder.binding.deleteGraphButton.setOnClickListener {
            onDeleteClick(graph)
        }
    }

    private fun setupChart(
        chart: com.github.mikephil.charting.charts.LineChart,
        dataPoints: List<DataPoint>,
        graph: GraphMetadata
    ) {
        // Sort data points by timestamp ascending for the chart
        val sortedPoints = dataPoints.sortedBy { it.timestamp }
        
        if (sortedPoints.isEmpty()) return

        // Fix precision loss by using relative time
        val referenceTimestamp = sortedPoints.first().timestamp

        val entries = sortedPoints.map { point ->
            // X is milliseconds since first point
            Entry((point.timestamp - referenceTimestamp).toFloat(), point.value.toFloat())
        }

        val dataSet = LineDataSet(entries, graph.name).apply {
            color = Color.parseColor("#19A8AD") // Main blue
            setCircleColor(Color.parseColor("#19A8AD"))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = Color.parseColor("#19A8AD")
            fillAlpha = 50
        }

        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        // Add reference back to get actual time
                        val originalTimestamp = value.toLong() + referenceTimestamp
                        return dateFormat.format(Date(originalTimestamp))
                    }
                }
            }
            
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
            }
            
            animateX(1000)
            invalidate()
        }
    }

    override fun getItemCount() = graphs.size
}
