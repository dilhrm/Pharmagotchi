package com.example.pharmagotchi.database

import androidx.room.*
import com.example.pharmagotchi.models.DataPoint
import com.example.pharmagotchi.models.GraphMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface GraphDao {
    @Query("SELECT * FROM graphs ORDER BY createdAt DESC")
    fun getAllGraphs(): Flow<List<GraphMetadata>>

    @Query("SELECT * FROM graphs WHERE isVisible = 1 ORDER BY createdAt DESC")
    fun getVisibleGraphs(): Flow<List<GraphMetadata>>

    @Query("SELECT * FROM graphs WHERE isVisible = 1")
    suspend fun getVisibleGraphsSync(): List<GraphMetadata>

    @Query("SELECT * FROM graphs WHERE id = :graphId")
    suspend fun getGraphById(graphId: Long): GraphMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGraph(graph: GraphMetadata): Long

    @Update
    suspend fun updateGraph(graph: GraphMetadata)

    @Delete
    suspend fun deleteGraph(graph: GraphMetadata)

    @Query("SELECT * FROM data_points WHERE graphId = :graphId ORDER BY timestamp DESC")
    fun getDataPointsForGraph(graphId: Long): Flow<List<DataPoint>>

    @Query("SELECT * FROM data_points ORDER BY timestamp DESC")
    fun getAllDataPoints(): Flow<List<DataPoint>>

    @Query("SELECT * FROM data_points WHERE graphId = :graphId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentDataPoints(graphId: Long, limit: Int): List<DataPoint>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDataPoint(dataPoint: DataPoint): Long

    @Delete
    suspend fun deleteDataPoint(dataPoint: DataPoint)

    @Query("DELETE FROM data_points WHERE graphId = :graphId")
    suspend fun deleteAllDataPointsForGraph(graphId: Long)
}
