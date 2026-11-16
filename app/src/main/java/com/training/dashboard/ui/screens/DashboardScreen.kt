package com.training.dashboard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.training.dashboard.data.ExerciseItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    workoutTitle: String,
    exercises: List<ExerciseItem>,
    weeklyCompletionRatio: Float,
    streakDays: Int,
    onToggleExercise: (ExerciseItem) -> Unit,
    onStartWorkoutClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }
    val dateHeader = remember { today.format(DateTimeFormatter.ofPattern("EEEE, MMM d")) }

    val totalCount = exercises.size
    val completedCount = exercises.count { it.isCompletedToday }
    val remainingCount = (totalCount - completedCount).coerceAtLeast(0)
    val dailyProgress by animateFloatAsState(
        targetValue = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f,
        label = "dailyProgress"
    )
    val allFinished = totalCount > 0 && completedCount == totalCount

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 8.dp) {
                Button(
                    onClick = onStartWorkoutClick,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (completedCount > 0 && !allFinished) "Resume Workout" else "Start Workout", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Column {
                    Text(text = dateHeader.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(text = workoutTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFF3B82F6).copy(alpha = 0.16f), Color(0xFF8B5CF6).copy(alpha = 0.14f))))
                            .padding(20.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("Completed", "$completedCount / $totalCount")
                            StatPill("Remaining", "$remainingCount")
                            StatPill("Completion", "${(dailyProgress * 100).toInt()}%")
                        }
                        Spacer(Modifier.height(16.dp))
                        LinearProgressIndicator(progress = { dailyProgress }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)))
                        Spacer(Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Weekly Goal: ${(weeklyCompletionRatio * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("🔥 $streakDays-day streak", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            item {
                AnimatedVisibility(visible = allFinished, enter = scaleIn()) {
                    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))) {
                        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Session Complete!", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text("All $totalCount exercises logged for today.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            items(exercises, key = { it.id }) { item ->
                ExerciseChecklistCard(exercise = item, onToggle = {
                    onToggleExercise(item)
                    scope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val res = snackbarHostState.showSnackbar(
                            message = if (!item.isCompletedToday) "Checked: ${item.name}" else "Unchecked: ${item.name}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            onToggleExercise(item.copy(isCompletedToday = !item.isCompletedToday))
                        }
                    }
                })
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExerciseChecklistCard(exercise: ExerciseItem, onToggle: () -> Unit) {
    Card(onClick = onToggle, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (exercise.isCompletedToday) MaterialTheme.colorScheme.surface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = exercise.isCompletedToday, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = exercise.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textDecoration = if (exercise.isCompletedToday) TextDecoration.LineThrough else TextDecoration.None)
                Text(text = "${exercise.sets} sets × ${exercise.repsOrDuration}  •  Rest ${exercise.restSeconds}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
