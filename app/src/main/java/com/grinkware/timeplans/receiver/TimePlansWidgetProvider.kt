package com.grinkware.timeplans.receiver

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.grinkware.timeplans.R
import com.grinkware.timeplans.data.TimeplansRepository
import java.text.SimpleDateFormat
import java.util.*

class TimePlansWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Manual updates from app view model
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, TimePlansWidgetProvider::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        onUpdate(context, appWidgetManager, appWidgetIds)
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val repository = TimeplansRepository(context)
        val active = repository.getActiveTimetable()
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        if (active == null) {
            views.setTextViewText(R.id.widget_lesson_name, "No active plan")
            views.setTextViewText(R.id.widget_lesson_details, "Open TimePlans to begin")
            views.setTextViewText(R.id.widget_next_lesson, "")
            views.setTextViewText(R.id.widget_attendance_status, "")
            appWidgetManager.updateAppWidget(appWidgetId, views)
            return
        }

        val lessons = repository.getLessons(active.id)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMin = hour * 60 + minute

        val sysDay = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }

        val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
        val weekStr = if (weekNum % 2 == 0) "A" else "B"

        val todayLessonsRaw = lessons.filter { it.dayOfWeek == sysDay }
        val todayLessonsFiltered = if (active.hasTwoWeeks) {
            todayLessonsRaw.filter { it.weekType == "BOTH" || it.weekType == weekStr }
        } else {
            todayLessonsRaw
        }.sortedBy { it.startTimeMinutes }

        // Find overrides
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val overrides = repository.getOverridesForDate(todayStr)

        var currentTitle = "Free Time"
        var currentDetail = "No class at the moment"
        var nextText = ""

        if (todayLessonsFiltered.isNotEmpty()) {
            val firstLesson = todayLessonsFiltered.first()
            val lastLesson = todayLessonsFiltered.last()

            var activeLesson = todayLessonsFiltered.find { currentMin in it.startTimeMinutes until it.endTimeMinutes }
            var upcomingLesson = todayLessonsFiltered.find { it.startTimeMinutes > currentMin }

            // Resolve overrides for active lesson
            if (activeLesson != null) {
                val override = overrides.find { it.lessonId == activeLesson.id }
                if (override?.isCancelled == true) {
                    currentTitle = "Cancelled: ${activeLesson.name}"
                    currentDetail = "Was in Room ${activeLesson.room}"
                } else {
                    val finalRoom = if (override != null && override.newRoom.isNotEmpty()) override.newRoom else activeLesson.room
                    val finalTeacher = if (override != null && override.newTeacher.isNotEmpty()) override.newTeacher else activeLesson.teacher
                    currentTitle = activeLesson.name
                    currentDetail = "Room $finalRoom • $finalTeacher"
                }
            } else if (currentMin < firstLesson.startTimeMinutes) {
                currentTitle = "School Not Started"
                val diff = firstLesson.startTimeMinutes - currentMin
                currentDetail = "First class: ${firstLesson.name} in ${diff}m"
            } else if (currentMin >= lastLesson.endTimeMinutes) {
                currentTitle = "School Finished"
                currentDetail = "Have a nice day!"
            } else {
                currentTitle = "Break / Gap"
                if (upcomingLesson != null) {
                    val diff = upcomingLesson.startTimeMinutes - currentMin
                    currentDetail = "Next class starts in ${diff}m"
                }
            }

            if (upcomingLesson != null) {
                val override = overrides.find { it.lessonId == upcomingLesson.id }
                val finalRoom = if (override != null && override.newRoom.isNotEmpty()) override.newRoom else upcomingLesson.room
                nextText = "Next: ${upcomingLesson.name} at ${upcomingLesson.startTimeString} (Rm $finalRoom)"
            }
        }

        // Attendance today
        val todayRecord = repository.getAttendanceRecords().find { it.date == todayStr }
        val attendanceLabel = when (todayRecord?.status) {
            "IN" -> "Status: In School"
            "OFF" -> "Status: Off Today"
            "LATE" -> "Status: Late Today"
            "HOLIDAY" -> "Status: On Holiday"
            "SICK" -> "Status: Sick Today"
            else -> "Status: Unmarked"
        }

        views.setTextViewText(R.id.widget_lesson_name, currentTitle)
        views.setTextViewText(R.id.widget_lesson_details, currentDetail)
        views.setTextViewText(R.id.widget_next_lesson, nextText)
        views.setTextViewText(R.id.widget_attendance_status, attendanceLabel)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
