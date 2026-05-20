package com.grinkware.timeplans.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class LessonAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = com.grinkware.timeplans.data.TimeplansRepository(context)
        if (!repository.getSettings().showNotifications) {
            return
        }

        val lessonName = intent.getStringExtra("lesson_name") ?: "Upcoming Lesson"
        val room = intent.getStringExtra("room") ?: ""
        val startTime = intent.getStringExtra("time") ?: ""
        val periodType = intent.getStringExtra("period_type") ?: "CLASS"

        val channelId = "lesson_alarms"
        val notificationId = 2002

        // Create Channel on 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Class Reminders"
            val desc = "Notifications shown before a class begins"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val title = when (periodType) {
            "BREAK" -> "Time for Break"
            "LUNCH" -> "Time for Lunch"
            "ASSEMBLY" -> "Upcoming Assembly: $lessonName"
            else -> "Next Up: $lessonName"
        }

        val text = when (periodType) {
            "BREAK", "LUNCH" -> "Starts at $startTime"
            else -> if (room.isNotEmpty()) "Starts at $startTime in Room $room" else "Starts at $startTime"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // System alarm icon
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val compatManager = NotificationManagerCompat.from(context)
            compatManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Missing post notifications permission
        }

        // Schedule the next chronological class alarm
        NotificationScheduler.scheduleNextAlarm(context)
    }
}
