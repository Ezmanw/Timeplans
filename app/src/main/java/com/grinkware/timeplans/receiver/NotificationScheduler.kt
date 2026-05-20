package com.grinkware.timeplans.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.data.TimeplansRepository
import java.util.*

object NotificationScheduler {
    private const val ALARM_REQ_CODE = 1001

    fun scheduleNextAlarm(context: Context) {
        val repository = TimeplansRepository(context)
        val settings = repository.getSettings()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Always schedule/update homework alarms
        scheduleHomeworkAlarms(context)

        // If notifications are disabled, cancel any scheduled alarms
        if (!settings.showNotifications) {
            val intent = Intent(context, LessonAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
            return
        }

        val active = repository.getActiveTimetable() ?: return
        val currentLessons = repository.getLessons(active.id)
        if (currentLessons.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val leadMins = settings.alarmLeadMinutes
        val now = System.currentTimeMillis()

        var bestAlarmTime: Long = Long.MAX_VALUE
        var bestLesson: Lesson? = null

        // Look ahead for the next 7 days (including today)
        for (dayOffset in 0..7) {
            val futureCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val sysDay = when (futureCal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            val weekNum = futureCal.get(Calendar.WEEK_OF_YEAR)
            val weekStr = if (weekNum % 2 == 0) "A" else "B"

            // Filter lessons for this day
            val dayLessons = currentLessons.filter { it.dayOfWeek == sysDay }
            val filteredLessons = if (active.hasTwoWeeks) {
                dayLessons.filter { it.weekType == "BOTH" || it.weekType == weekStr }
            } else {
                dayLessons
            }

            for (lesson in filteredLessons) {
                // Calculate when this alarm should fire
                val alarmTimeCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, (lesson.startTimeMinutes - leadMins) / 60)
                    set(Calendar.MINUTE, (lesson.startTimeMinutes - leadMins) % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val alarmTimeMs = alarmTimeCal.timeInMillis
                if (alarmTimeMs > now && alarmTimeMs < bestAlarmTime) {
                    bestAlarmTime = alarmTimeMs
                    bestLesson = lesson
                }
            }
        }

        if (bestLesson != null && bestAlarmTime != Long.MAX_VALUE) {
            val alarmIntent = Intent(context, LessonAlarmReceiver::class.java).apply {
                putExtra("lesson_name", bestLesson.name)
                putExtra("room", bestLesson.room)
                putExtra("time", bestLesson.startTimeString)
                putExtra("period_type", bestLesson.periodType)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQ_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        bestAlarmTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        bestAlarmTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Missing exact alarm permission
            }
        }
    }

    private fun scheduleHomeworkAlarms(context: Context) {
        val repository = TimeplansRepository(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settings = repository.getSettings()

        val allTasks = repository.getTasks()

        for (task in allTasks) {
            val requestCode = 20000 + task.id.toInt()
            val alarmIntent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
                putExtra("task_id", task.id)
            }

            // If notifications are disabled or the task is completed, cancel it
            if (!settings.showNotifications || task.isCompleted || task.taskType != "HOMEWORK") {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    alarmIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
                continue
            }

            // Calculate alarm time (due date at 18:00 the day before, or 08:00 AM on the due date)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dueCal = java.util.Calendar.getInstance()
            try {
                val parsedDate = format.parse(task.dueDate)
                if (parsedDate != null) {
                    dueCal.time = parsedDate
                    // Try the day before at 18:00
                    dueCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    dueCal.set(java.util.Calendar.HOUR_OF_DAY, 18)
                    dueCal.set(java.util.Calendar.MINUTE, 0)
                    dueCal.set(java.util.Calendar.SECOND, 0)
                    dueCal.set(java.util.Calendar.MILLISECOND, 0)

                    val now = System.currentTimeMillis()
                    if (dueCal.timeInMillis <= now) {
                        // Fallback: Due date at 08:00 AM
                        dueCal.time = parsedDate
                        dueCal.set(java.util.Calendar.HOUR_OF_DAY, 8)
                        dueCal.set(java.util.Calendar.MINUTE, 0)
                        dueCal.set(java.util.Calendar.SECOND, 0)
                        dueCal.set(java.util.Calendar.MILLISECOND, 0)
                    }

                    if (dueCal.timeInMillis > now) {
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            alarmIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    dueCal.timeInMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    dueCal.timeInMillis,
                                    pendingIntent
                                )
                            }
                        } catch (e: SecurityException) {
                            // Missing permission
                        }
                    } else {
                        // In the past, cancel any active alarm
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            alarmIntent,
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        )
                        if (pendingIntent != null) {
                            alarmManager.cancel(pendingIntent)
                            pendingIntent.cancel()
                        }
                    }
                }
            } catch (e: Exception) {
                // Formatting or parsing error
            }
        }
    }
}
