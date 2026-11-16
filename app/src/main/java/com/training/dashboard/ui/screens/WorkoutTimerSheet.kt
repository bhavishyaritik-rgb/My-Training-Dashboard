package com.training.dashboard.ui.screens

import android.content.Context
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.training.dashboard.data.ExerciseItem
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    exercises: List<ExerciseItem>,
    autoAdvance: Boolean,
    soundAlertEnabled: Boolean,
    onMarkExerciseComplete: (ExerciseItem) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(exercises.indexOfFirst { !it.isCompletedToday }.coerceAtLeast(0)) }
    val currentExercise = exercises.getOrNull(currentIndex)

    var isRestPhase by remember { mutableStateOf(false) }
    var secondsLeft by remember(currentIndex, isRestPhase) {
        mutableIntStateOf(if (isRestPhase) (currentExercise?.restSeconds ?: 45) else (currentExercise?.durationSeconds?.takeIf { it > 0 } ?: 30))
    }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft -= 1
        } else if (isRunning && secondsLeft == 0) {
            isRunning = false
            triggerTimerFeedback(context, soundAlertEnabled)
            if (!isRestPhase && currentExercise != null) {
                isRestPhase = true
                if (autoAdvance) isRunning = true
            } else if (isRestPhase && currentExercise != null) {
                onMarkExerciseComplete(currentExercise)
                isRestPhase = false
                if (autoAdvance && currentIndex < exercises.lastIndex) currentIndex++
            }
        }
    }

    if (currentExercise == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = onClose) { Text("Workout Complete — Back to Dashboard") }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (isRestPhase) "REST & RECOVER" else "EXERCISE ${currentIndex + 1} OF ${exercises.size}", color = if (isRestPhase) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(currentExercise.name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            Text("${currentExercise.sets} × ${currentExercise.repsOrDuration}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60), fontSize = 68.sp, fontWeight = FontWeight.Black)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { isRunning = !isRunning }, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text(if (isRunning) "Pause" else "Start / Resume", fontSize = 16.sp)
                }
                OutlinedButton(onClick = {
                    isRunning = false
                    secondsLeft = if (isRestPhase) currentExercise.restSeconds else currentExercise.durationSeconds.coerceAtLeast(30)
                }, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("Reset")
                }
            }
            Button(onClick = {
                onMarkExerciseComplete(currentExercise)
                isRestPhase = false
                if (currentIndex < exercises.lastIndex) currentIndex++ else onClose()
            }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                Text("Complete & Next Exercise", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun triggerTimerFeedback(context: Context, playSound: Boolean) {
    val vibrator = context.getSystemService(Vibrator::class.java)
    vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
    if (playSound) {
        ToneGenerator(AudioManager.STREAM_ALARM, 85).startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
    }
}
