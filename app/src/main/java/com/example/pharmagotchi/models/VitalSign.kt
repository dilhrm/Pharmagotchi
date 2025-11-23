package com.example.pharmagotchi.models

data class VitalSign(
    val name: String,
    val unit: String,
    val category: String, // e.g., "cardiovascular", "metabolic", "respiratory"
    val normalRange: String? = null,
    val relatedConditions: List<String> = emptyList(),
    val relatedMedications: List<String> = emptyList()
)
