package com.example.pharmagotchi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "graphs")
data class GraphMetadata(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val vitalSignName: String,
    val unit: String,
    val isVisible: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "data_points")
data class DataPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val graphId: Long,
    val value: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
