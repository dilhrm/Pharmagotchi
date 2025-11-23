package com.example.pharmagotchi.database

import androidx.room.*
import com.example.pharmagotchi.models.HealthContact
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthContactDao {
    @Query("SELECT * FROM health_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<HealthContact>>

    @Query("SELECT * FROM health_contacts")
    suspend fun getAllContactsSync(): List<HealthContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: HealthContact): Long

    @Delete
    suspend fun deleteContact(contact: HealthContact)
}
