package com.example.pharmagotchi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "health_contacts")
data class HealthContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val role: String // e.g., Pharmacist, Doctor, Caregiver
)
