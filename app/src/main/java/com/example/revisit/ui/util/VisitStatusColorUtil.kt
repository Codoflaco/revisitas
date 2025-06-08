package com.example.revisit.ui.util

import androidx.compose.ui.graphics.Color
import java.util.Calendar

object VisitStatusColorUtil {

    val ColorPast: Color = Color.Red
    val ColorToday: Color = Color(0xFFFFA500)
    val ColorApproaching: Color = Color.Yellow
    val ColorFar: Color = Color.Green
    val ColorNoDate: Color = Color.Gray

    const val PERCENTAGE_THRESHOLD_FOR_APPROACHING: Double = 0.50 // 50%

    fun getVisitStatusColor(
        nextVisitTimestamp: Long?,
        referenceStartDateForNextVisit: Long?
    ): Color {
        if (nextVisitTimestamp == null || nextVisitTimestamp == 0L) {
            return ColorNoDate
        }

        val validReferenceStartDate = referenceStartDateForNextVisit ?: System.currentTimeMillis()

        val today = Calendar.getInstance()
        val todayStartMillis = today.timeInMillis

        Calendar.getInstance().apply { timeInMillis = nextVisitTimestamp }

        val todayNormalized = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val nextVisitNormalized = Calendar.getInstance().apply {
            timeInMillis = nextVisitTimestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        if (nextVisitNormalized.before(todayNormalized)) {
            return ColorPast
        }

        if (nextVisitNormalized.get(Calendar.YEAR) == todayNormalized.get(Calendar.YEAR) &&
            nextVisitNormalized.get(Calendar.DAY_OF_YEAR) == todayNormalized.get(Calendar.DAY_OF_YEAR)) {
            return ColorToday
        }

        if (validReferenceStartDate > nextVisitTimestamp) {

            return ColorFar
        }

        val totalDurationMillis = nextVisitTimestamp - validReferenceStartDate
        val elapsedMillisSinceReference = todayStartMillis - validReferenceStartDate

        if (totalDurationMillis <= 0) {
            return ColorFar
        }

        val percentageElapsed = elapsedMillisSinceReference.toDouble() / totalDurationMillis

        return when {
            percentageElapsed < PERCENTAGE_THRESHOLD_FOR_APPROACHING -> ColorFar
            else -> ColorApproaching
        }
    }
}