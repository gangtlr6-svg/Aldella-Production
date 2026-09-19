package com.example

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AldellaOperationsUnitTest {

    @Test
    fun testAttendanceWorkingHoursCalculation() {
        fun calculateAttendanceHours(
            in1: String, out1: String,
            in2: String, out2: String,
            in3: String, out3: String
        ): String {
            fun diffMinutes(tin: String, tout: String): Long {
                if (tin.isBlank() || tout.isBlank()) return 0
                return try {
                    val partsIn = tin.trim().split(":")
                    val partsOut = tout.trim().split(":")
                    val minIn = partsIn[0].toInt() * 60 + partsIn[1].toInt()
                    val minOut = partsOut[0].toInt() * 60 + partsOut[1].toInt()
                    if (minOut >= minIn) (minOut - minIn).toLong() else (1440 - minIn + minOut).toLong()
                } catch (e: Exception) {
                    0
                }
            }

            val totalMins = diffMinutes(in1, out1) + diffMinutes(in2, out2) + diffMinutes(in3, out3)
            val hrs = totalMins / 60
            val mins = totalMins % 60
            return String.format(Locale.ENGLISH, "%02d:%02d:00", hrs, mins)
        }

        // Session 1: 07:00 to 12:00 (5 hours = 300 mins)
        // Session 2: 12:45 to 16:00 (3 hours 15 mins = 195 mins)
        // Session 3: 16:30 to 18:30 (2 hours = 120 mins)
        // Total: 615 mins = 10 hours 15 mins -> "10:15:00"
        val total = calculateAttendanceHours("07:00", "12:00", "12:45", "16:00", "16:30", "18:30")
        assertEquals("10:15:00", total)
    }

    @Test
    fun testOperationalTimerFormatting() {
        fun formatSecondsToTime(seconds: Long): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, secs)
        }

        assertEquals("00:00:00", formatSecondsToTime(0))
        assertEquals("00:01:30", formatSecondsToTime(90))
        assertEquals("10:16:58", formatSecondsToTime(37018))
    }
}
