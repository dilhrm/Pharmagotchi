package com.example.pharmagotchi.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.pharmagotchi.adapters.GraphWithData
import com.example.pharmagotchi.database.PharmagotchiDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val graphDao = PharmagotchiDatabase.getDatabase(application).graphDao()

    val graphsWithData: Flow<List<GraphWithData>> = combine(
        graphDao.getVisibleGraphs(),
        graphDao.getAllDataPoints()
    ) { graphs, allDataPoints ->
        graphs.map { graph ->
            val graphData = allDataPoints
                .filter { it.graphId == graph.id }
                .sortedByDescending { it.timestamp }
                .take(5)
            GraphWithData(graph, graphData)
        }
    }
}