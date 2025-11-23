package com.example.pharmagotchi.utils

import android.content.Context
import com.example.pharmagotchi.PreferencesManager
import com.example.pharmagotchi.database.PharmagotchiDatabase
import com.example.pharmagotchi.models.PetEmotion
import com.example.pharmagotchi.models.PetStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PetStatusManager(private val context: Context) {

    private val database = PharmagotchiDatabase.getDatabase(context)
    private val prefsManager = PreferencesManager(context)

    suspend fun determineStatus(): PetStatus = withContext(Dispatchers.IO) {
        // 1. Check Health Status (In Pain)
        val (healthStatus, _) = prefsManager.getHealthStatus()
        if (healthStatus == "WARNING" || healthStatus == "CRITICAL") {
            return@withContext PetStatus(
                PetEmotion.IN_PAIN,
                "I'm not feeling well because your recent health metrics are concerning."
            )
        }

        // 2. Check Medications (Sad)
        val medications = database.medicationDao().getAllMedicationsSync()
        val currentTime = System.currentTimeMillis()
        val missedMedications = medications.filter { med ->
            val intervalMillis = TimeUnit.HOURS.toMillis(med.intervalHours.toLong())
            // Grace period of 50% of interval
            val gracePeriod = intervalMillis / 2
            val timeSinceLastTaken = currentTime - med.lastTaken
            
            // Only count as missed if it's been taken at least once (lastTaken > 0)
            // OR if it's never been taken but created a long time ago? 
            // For simplicity, assume lastTaken > 0 means active tracking.
            // If lastTaken is 0, we can check if the medication was added > interval ago?
            // Let's stick to lastTaken > 0 for now to avoid false positives on new meds.
            med.lastTaken > 0 && timeSinceLastTaken > (intervalMillis + gracePeriod)
        }

        if (missedMedications.isNotEmpty()) {
            val medNames = missedMedications.joinToString(", ") { it.name }
            return@withContext PetStatus(
                PetEmotion.SAD,
                "I'm sad because you missed your dose of: $medNames."
            )
        }

        // 3. Check Data Tracking (Confused)
        // If no data logged for any visible graph in > 2 days
        val visibleGraphs = database.graphDao().getVisibleGraphs().first()
        if (visibleGraphs.isNotEmpty()) {
            var hasRecentData = false
            for (graph in visibleGraphs) {
                val latestData = database.graphDao().getRecentDataPoints(graph.id, 1)
                if (latestData.isNotEmpty()) {
                    val timeSinceData = currentTime - latestData.first().timestamp
                    if (timeSinceData < TimeUnit.DAYS.toMillis(2)) {
                        hasRecentData = true
                        break
                    }
                }
            }

            if (!hasRecentData) {
                return@withContext PetStatus(
                    PetEmotion.CONFUSED,
                    "I'm confused because you haven't logged any health data in over 2 days."
                )
            }
        }

        // 4. Default (Happy)
        return@withContext PetStatus(
            PetEmotion.HAPPY,
            "I'm happy because you're taking good care of yourself!"
        )
    }
}
