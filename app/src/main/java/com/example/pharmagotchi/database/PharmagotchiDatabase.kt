package com.example.pharmagotchi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pharmagotchi.models.DataPoint
import com.example.pharmagotchi.models.GraphMetadata
import com.example.pharmagotchi.models.Medication

import com.example.pharmagotchi.models.HealthContact

@Database(
    entities = [GraphMetadata::class, DataPoint::class, Medication::class, HealthContact::class],
    version = 4,
    exportSchema = false
)
abstract class PharmagotchiDatabase : RoomDatabase() {
    abstract fun graphDao(): GraphDao
    abstract fun medicationDao(): MedicationDao
    abstract fun healthContactDao(): HealthContactDao

    companion object {
        @Volatile
        private var INSTANCE: PharmagotchiDatabase? = null

        fun getDatabase(context: Context): PharmagotchiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PharmagotchiDatabase::class.java,
                    "pharmagotchi_database"
                )
                .fallbackToDestructiveMigration() // For simplicity in development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
