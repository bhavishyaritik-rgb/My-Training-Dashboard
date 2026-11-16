package com.training.dashboard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "day_plans")
data class DayPlan(
    @PrimaryKey val dayOfWeek: Int,
    val title: String,
    val isRestDay: Boolean = false,
    val personalNote: String = ""
)

@Serializable
@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = DayPlan::class,
        parentColumns = ["dayOfWeek"],
        childColumns = ["dayOfWeek"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("dayOfWeek")]
)
data class ExerciseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val name: String,
    val sets: Int = 3,
    val repsOrDuration: String = "10",
    val durationSeconds: Int = 0,
    val restSeconds: Int = 45,
    val isCompletedToday: Boolean = false,
    val sortOrder: Int = 0
)

@Serializable
@Entity(tableName = "workout_logs")
data class WorkoutLog(
    @PrimaryKey val dateIso: String,
    val dayOfWeek: Int,
    val workoutTitle: String,
    val completedCount: Int,
    val totalCount: Int,
    val isRestOrRecovery: Boolean = false,
    val notes: String = ""
)

@Dao
interface TrainingDao {
    @Query("SELECT * FROM day_plans ORDER BY dayOfWeek ASC")
    fun observeAllDayPlans(): Flow<List<DayPlan>>

    @Query("SELECT * FROM day_plans WHERE dayOfWeek = :day")
    fun observeDayPlan(day: Int): Flow<DayPlan?>

    @Query("SELECT * FROM exercises WHERE dayOfWeek = :day ORDER BY sortOrder ASC")
    fun observeExercisesForDay(day: Int): Flow<List<ExerciseItem>>

    @Query("SELECT * FROM exercises WHERE dayOfWeek = :day ORDER BY sortOrder ASC")
    suspend fun getExercisesForDaySync(day: Int): List<ExerciseItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDayPlan(plan: DayPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseItem>)

    @Update
    suspend fun updateExercise(exercise: ExerciseItem)

    @Query("UPDATE exercises SET isCompletedToday = 0")
    suspend fun resetAllDailyCheckboxes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkoutLog(log: WorkoutLog)

    @Query("SELECT * FROM workout_logs ORDER BY dateIso DESC LIMIT 60")
    fun observeRecentLogs(): Flow<List<WorkoutLog>>
}
