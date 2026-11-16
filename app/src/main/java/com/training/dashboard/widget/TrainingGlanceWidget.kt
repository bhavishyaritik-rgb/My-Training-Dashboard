package com.training.dashboard.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.training.dashboard.MainActivity
import com.training.dashboard.data.TrainingDatabase
import java.time.LocalDate

val ExerciseIdKey = ActionParameters.Key<Long>("exercise_id")

class TrainingGlanceWidget : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(250.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 220.dp)
    }

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = TrainingDatabase.getInstance(context)
        val todayDow = LocalDate.now().dayOfWeek.value
        val exercises = db.trainingDao().getExercisesForDaySync(todayDow)
        val completed = exercises.count { it.isCompletedToday }
        val total = exercises.size
        val nextExercise = exercises.firstOrNull { !it.isCompletedToday }

        provideContent {
            val size = LocalSize.current
            WidgetContent(
                context = context,
                completed = completed,
                total = total,
                nextName = nextExercise?.name ?: if (total > 0) "All Done! 🎉" else "Rest Day",
                nextId = nextExercise?.id ?: -1L,
                isSmall = size.width < 200.dp
            )
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        completed: Int,
        total: Int,
        nextName: String,
        nextId: Long,
        isSmall: Boolean
    ) {
        val progress = if (total > 0) completed.toFloat() / total else 0f
        val openAppIntent = Intent(context, MainActivity::class.java)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF14171F)))
                .cornerRadius(20.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(openAppIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isSmall) "$completed/$total" else "Today's Training",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (!isSmall) {
                    Text(text = "$completed/$total", style = TextStyle(color = ColorProvider(Color(0xFF3B82F6)), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
                color = ColorProvider(Color(0xFF3B82F6)),
                backgroundColor = ColorProvider(Color(0xFF283042))
            )
            Spacer(GlanceModifier.height(10.dp))
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (nextId != -1L) "NEXT: $nextName" else nextName,
                    style = TextStyle(color = ColorProvider(Color(0xFFE5E7EB)), fontSize = 13.sp),
                    modifier = GlanceModifier.defaultWeight(),
                    maxLines = 1
                )
                if (nextId != -1L) {
                    Button(
                        text = "✓",
                        onClick = actionRunCallback<CompleteNextExerciseAction>(actionParametersOf(ExerciseIdKey to nextId))
                    )
                }
            }
        }
    }
}

class CompleteNextExerciseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[ExerciseIdKey] ?: return
        val db = TrainingDatabase.getInstance(context)
        val todayDow = LocalDate.now().dayOfWeek.value
        val item = db.trainingDao().getExercisesForDaySync(todayDow).find { it.id == id }
        if (item != null) {
            db.trainingDao().updateExercise(item.copy(isCompletedToday = true))
        }
        TrainingGlanceWidget().updateAll(context)
    }
}

class TrainingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TrainingGlanceWidget()
}
