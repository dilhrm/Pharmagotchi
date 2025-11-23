package com.example.pharmagotchi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val intervalHours: Int = 24, // Default to once daily
    val lastTaken: Long = 0
)
