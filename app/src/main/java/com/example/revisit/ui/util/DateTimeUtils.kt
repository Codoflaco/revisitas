// File: com/example/revisit/ui/util/DateTimeUtils.kt
package com.example.revisit.ui.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    internal fun getDefaultDeviceZoneId(): ZoneId = ZoneId.systemDefault() // Represents the device's current timezone
    //private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)
    //private val displayDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val displayDateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ROOT)

    // --- Formatters using java.time (more reliable) ---


    /**
     * Formats a given UTC timestamp (Long) into a combined date and time string
     * for display, respecting the device's current timezone and locale.
     * Uses a localized format.
     */


    fun formatDateTimeForDisplay(timestamp: Long?): String {
        if (timestamp == null) return "N/A"
        // Considera si quieres mostrar la hora en el TimeZone del dispositivo o UTC
        // Por ahora, usamos el TimeZone por defecto del dispositivo para la visualización.
        return displayDateTimeFormat.format(Date(timestamp))
    }

    fun formatDateForDisplay(timestamp: Long?): String {
        if (timestamp == null) return "N/A"
        return displayDateFormat.format(Date(timestamp))
    }


    // NUEVA FUNCIÓN: Obtener el timestamp de la medianoche (inicio del día) en UTC
    fun getStartOfDayUTCTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    // NUEVA FUNCIÓN: Obtener el timestamp del final del día (un milisegundo antes de la medianoche siguiente) en UTC
    fun getEndOfDayUTCTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    // You can add similar _Pattern functions for date and time if specifically needed.


    /**
     * Returns a default UTC timestamp for the next visit, e.g., 3 days from now at 11:30
     * in the device's local timezone, then converted back to a UTC timestamp.
     */
    fun getDefaultNextVisitDateTime(): Long {
        return ZonedDateTime.now(getDefaultDeviceZoneId())
            .plusDays(3)
            .withHour(11)
            .withMinute(30)
            .withSecond(0)
            .withNano(0)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Checks if the system is set to 24-hour format.
     */
    fun isSystem24Hour(context: Context): Boolean {
        return DateFormat.is24HourFormat(context) // This Android API is fine
    }

    /**
     * Combines a date (from a UTC timestamp) and a time (hour, minute) into a new UTC timestamp.
     * The date part is taken from originalTimestampUTC, and the time part (hour, minute)
     * is interpreted in the device's local timezone context for that date.
     */
    fun combineDateAndTime(originalDateTimestampUTC: Long, hour: Int, minute: Int): Long {
        // Get the date part in the device's local timezone
        val localDate = Instant.ofEpochMilli(originalDateTimestampUTC)
            .atZone(getDefaultDeviceZoneId())
            .toLocalDate()

        // Combine with the given hour and minute in the device's local timezone
        val localDateTime = localDate.atTime(hour, minute, 0, 0)

        // Convert this local DateTime back to a UTC timestamp
        return localDateTime.atZone(getDefaultDeviceZoneId()).toInstant().toEpochMilli()
    }

    // --- Utility to get components for TimePicker/DatePicker initial state ---

    /**
     * Gets the hour of the day from a UTC timestamp, as it would be in the device's local timezone.
     */
    fun getHourDevice(timestampUTC: Long): Int {
        return Instant.ofEpochMilli(timestampUTC).atZone(getDefaultDeviceZoneId()).hour
    }

    /**
     * Gets the minute of the hour from a UTC timestamp, as it would be in the device's local timezone.
     */
    fun getMinuteDevice(timestampUTC: Long): Int {
        return Instant.ofEpochMilli(timestampUTC).atZone(getDefaultDeviceZoneId()).minute
    }
}