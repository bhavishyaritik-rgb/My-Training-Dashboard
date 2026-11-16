package com.training.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.training.dashboard.data.TrainingDatabase
import com.training.dashboard.domain.WorkoutPlanParser
import com.training.dashboard.ui.screens.ActiveWorkoutScreen
import com.training.dashboard.ui.screens.DashboardScreen
import com.training.dashboard.ui.theme.MyTrainingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = TrainingDatabase.getInstance(applicationContext)

        lifecycleScope.launch(Dispatchers.IO) {
            val existing = db.trainingDao().observeAllDayPlans().first()
            if (existing.isEmpty()) {
                val samplePlan = """
                    Monday — Push Strength
                    [ ] Push-ups — 3 × 12
                    [ ] Incline push-ups — 2 × 10
                    [ ] Pike holds — 3 × 20 sec
                    [ ] Shoulder taps — 2 × 15 each side

                    Tuesday — Pull & Core
                    [ ] Resistance row — 3 × 12
                    [ ] Plank hold — 3 × 45 sec
                    [ ] Superman arches — 3 × 10
                """.trimIndent()

                val parsed = WorkoutPlanParser.parsePlanText(samplePlan)
                parsed.forEach { (day, exercises) ->
                    db.trainingDao().upsertDayPlan(day)
                    db.trainingDao().insertExercises(exercises)
                }
            }
        }

        setContent {
            MyTrainingTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf("dashboard") }
                    val todayDow = LocalDate.now().dayOfWeek.value

                    val dayPlanState by db.trainingDao().observeDayPlan(todayDow).collectAsState(initial = null)
                    val exercisesState by db.trainingDao().observeExercisesForDay(todayDow).collectAsState(initial = emptyList())
                    val recentLogs by db.trainingDao().observeRecentLogs().collectAsState(initial = emptyList())

                    val workoutTitle = dayPlanState?.title ?: "Today's Training"
                    val weeklyRatio = if (recentLogs.isNotEmpty()) {
                        recentLogs.take(7).sumOf { it.completedCount.toDouble() / it.totalCount.coerceAtLeast(1) }.toFloat() / 7f
                    } else 0f

                    when (currentScreen) {
                        "dashboard" -> {
                            DashboardScreen(
                                workoutTitle = workoutTitle,
                                exercises = exercisesState,
                                weeklyCompletionRatio = weeklyRatio,
                                streakDays = recentLogs.count { it.completedCount >= it.totalCount && it.totalCount > 0 },
                                onToggleExercise = { item ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        db.trainingDao().updateExercise(item.copy(isCompletedToday = !item.isCompletedToday))
                                    }
                                },
                                onStartWorkoutClick = { currentScreen = "timer" }
                            )
                        }
                        "timer" -> {
                            ActiveWorkoutScreen(
                                exercises = exercisesState,
                                autoAdvance = true,
                                soundAlertEnabled = true,
                                onMarkExerciseComplete = { item ->
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        db.trainingDao().updateExercise(item.copy(isCompletedToday = true))
                                    }
                                },
                                onClose = { currentScreen = "dashboard" }
                            )
                        }
                    }
                }
            }
        }
    }
}
