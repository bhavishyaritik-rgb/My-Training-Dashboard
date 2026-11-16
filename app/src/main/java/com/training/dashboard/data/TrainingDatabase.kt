package com.training.dashboard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DayPlan::class, ExerciseItem::class, WorkoutLog::class], version = 1, exportSchema = false)
abstract class TrainingDatabase : RoomDatabase() {

    abstract fun trainingDao(): TrainingDao

    companion object {
        @Volatile
        private var INSTANCE: TrainingDatabase? = null

        fun getInstance(context: Context): TrainingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrainingDatabase::class.java,
                    "training_dashboard.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
