package com.grinkware.timeplans.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.grinkware.timeplans.data.TimeplansRepository

class HomeworkAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = TimeplansRepository(context)
        if (!repository.getSettings().showNotifications) {
            return
        }

        val taskId = intent.getLongExtra("task_id", -1L)
        if (taskId == -1L) return

        // Verify task exists and is not completed
        val task = repository.getTasks().find { it.id == taskId }
        if (task == null || task.isCompleted) {
            return
        }

        val title = task.title
        val dueDate = task.dueDate

        val channelId = "homework_alarms"
        val notificationId = 3000 + taskId.toInt()

        // Create Channel on 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Homework Reminders"
            val desc = "Notifications shown for upcoming homework deadlines"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = desc
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Homework Due soon")
            .setContentText("\"$title\" is due on $dueDate")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            val compatManager = NotificationManagerCompat.from(context)
            compatManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Missing post notifications permission
        }
    }
}
