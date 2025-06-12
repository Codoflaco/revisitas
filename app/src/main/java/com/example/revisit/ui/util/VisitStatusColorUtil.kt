package com.example.revisit.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.util.Calendar
import java.util.concurrent.TimeUnit
import com.example.revisit.ui.theme.VisitStatusAppColors

object VisitStatusColorUtil {

    val ColorNoDate: Color = Color.Gray

    @Composable
    fun getVisitStatusColor(
        nextVisitTimestamp: Long?
    ): Color {
        if (nextVisitTimestamp == null || nextVisitTimestamp == 0L) {
            return ColorNoDate
        }

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val nextVisitDate = Calendar.getInstance()
        nextVisitDate.timeInMillis = nextVisitTimestamp
        nextVisitDate.set(Calendar.HOUR_OF_DAY, 0)
        nextVisitDate.set(Calendar.MINUTE, 0)
        nextVisitDate.set(Calendar.SECOND, 0)
        nextVisitDate.set(Calendar.MILLISECOND, 0)

        val diffMillis = nextVisitDate.timeInMillis - today.timeInMillis
        val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            nextVisitDate.before(today) -> {

                VisitStatusAppColors.Overdue
            }

            diffDays == 0L -> {
                VisitStatusAppColors.Today
            }

            diffDays in 1..3 -> {
                VisitStatusAppColors.DueSoon
            }

            diffDays > 3 -> {
                VisitStatusAppColors.DueFar
            }
            else -> ColorNoDate
        }
    }
}