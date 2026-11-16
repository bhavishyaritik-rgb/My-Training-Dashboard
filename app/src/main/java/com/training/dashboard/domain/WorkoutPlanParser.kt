package com.training.dashboard.domain

import com.training.dashboard.data.DayPlan
import com.training.dashboard.data.ExerciseItem
import java.time.DayOfWeek

object WorkoutPlanParser {
    private val dayMap = mapOf(
        "monday" to DayOfWeek.MONDAY.value,
        "tuesday" to DayOfWeek.TUESDAY.value,
        "wednesday" to DayOfWeek.WEDNESDAY.value,
        "thursday" to DayOfWeek.THURSDAY.value,
        "friday" to DayOfWeek.FRIDAY.value,
        "saturday" to DayOfWeek.SATURDAY.value,
        "sunday" to DayOfWeek.SUNDAY.value
    )

    fun parsePlanText(rawText: String): Map<DayPlan, List<ExerciseItem>> {
        val result = mutableMapOf<DayPlan, MutableList<ExerciseItem>>()
        var currentDay: DayPlan? = null
        var sortIdx = 0

        rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            val lower = line.lowercase()
            val matchedDay = dayMap.entries.firstOrNull { lower.startsWith(it.key) }

            if (matchedDay != null) {
                val titlePart = line.substringAfter("—", "")
                    .ifEmpty { line.substringAfter("-", "") }
                    .ifEmpty { line.substringAfter(":", "Training Session") }
                    .trim()
                val isRest = titlePart.contains("rest", ignoreCase = true)

                currentDay = DayPlan(
                    dayOfWeek = matchedDay.value,
                    title = titlePart.ifEmpty { "Workout" },
                    isRestDay = isRest
                )
                result.putIfAbsent(currentDay!!, mutableListOf())
                sortIdx = 0
            } else if (currentDay != null) {
                val cleanLine = line.removePrefix("[ ]").removePrefix("[x]").trim()
                val parts = cleanLine.split("—", " - ", ":")
                val name = parts[0].trim()
                val prescription = parts.getOrNull(1)?.trim() ?: "3 × 10"
                val setsRegex = Regex("""(\d+)\s*[x×]\s*(.+)""", RegexOption.IGNORE_CASE)
                val match = setsRegex.find(prescription)
                val sets = match?.groupValues?.get(1)?.toIntOrNull() ?: 3
                val repsOrDur = match?.groupValues?.get(2)?.trim() ?: prescription
                val secMatch = Regex("""(\d+)\s*(sec|s\b)""", RegexOption.IGNORE_CASE).find(repsOrDur)
                val durationSec = secMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

                result[currentDay!!]?.add(
                    ExerciseItem(
                        dayOfWeek = currentDay!!.dayOfWeek,
                        name = name,
                        sets = sets,
                        repsOrDuration = repsOrDur,
                        durationSeconds = durationSec,
                        sortOrder = sortIdx++
                    )
                )
            }
        }
        return result
    }
}
