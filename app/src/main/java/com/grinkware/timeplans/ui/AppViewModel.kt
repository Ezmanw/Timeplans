package com.grinkware.timeplans.ui

import android.app.Application
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.grinkware.timeplans.data.*
import com.grinkware.timeplans.receiver.LessonAlarmReceiver
import com.grinkware.timeplans.receiver.TimePlansWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TimeplansRepository(application)

    // State Variables
    val settings = mutableStateOf(AppSettings())
    val activeTimetable = mutableStateOf<SchoolYear?>(null)
    val timetables = mutableStateOf<List<SchoolYear>>(emptyList())
    val lessons = mutableStateOf<List<Lesson>>(emptyList())
    val attendanceRecords = mutableStateOf<List<AttendanceRecord>>(emptyList())
    val subjectStats = mutableStateOf<List<SubjectStats>>(emptyList())
    val tasks = mutableStateOf<List<TaskItem>>(emptyList())
    val exams = mutableStateOf<List<ExamItem>>(emptyList())
    val overrides = mutableStateOf<List<LessonOverride>>(emptyList())

    // Dashboard State
    val dashboardState = mutableStateOf(DashboardState())

    // Selected Day and Week for Timetable Screen
    val selectedDay = mutableStateOf(1) // 1 = Mon, ..., 7 = Sun
    val activeWeek = mutableStateOf("A") // "A" or "B"
    val manualWeekOverride = mutableStateOf<String?>(null)

    // Search query
    val searchQuery = mutableStateOf("")

    init {
        loadAllData()
        
        // Start live ticker for progress and countdowns
        viewModelScope.launch {
            while (isActive) {
                updateDashboardCalculations()
                delay(5000) // update every 5 seconds
            }
        }
    }

    fun loadAllData() {
        refreshSettings()
        refreshTimetables()
        refreshAttendance()
        refreshTasks()
        refreshExams()
        refreshOverrides()
        refreshLessons()
    }

    fun refreshSettings() {
        settings.value = repository.getSettings()
    }

    fun refreshTimetables() {
        val list = repository.getTimetables()
        timetables.value = list
        val active = repository.getActiveTimetable() ?: list.firstOrNull()
        activeTimetable.value = active
    }

    fun refreshLessons() {
        val activeId = activeTimetable.value?.id ?: return
        lessons.value = repository.getLessons(activeId)
        updateDashboardCalculations()
        updateWidget()
        scheduleNotifications()
    }

    fun refreshAttendance() {
        attendanceRecords.value = repository.getAttendanceRecords()
        val activeId = activeTimetable.value?.id ?: return
        subjectStats.value = repository.getSubjectAttendanceStats(activeId)
    }

    fun refreshTasks() {
        tasks.value = repository.getTasks()
        scheduleNotifications()
    }

    fun refreshExams() {
        exams.value = repository.getExams()
    }

    fun refreshOverrides() {
        val todayStr = getTodayDateString()
        overrides.value = repository.getOverridesForDate(todayStr)
    }

    // --- ACTIONS ---

    fun updateSetting(key: String, value: String) {
        repository.saveSetting(key, value)
        refreshSettings()
        if (key == "endOfYearDate") {
            updateDashboardCalculations()
        }
    }

    fun setSchoolYearActive(timetableId: Long) {
        repository.setActiveTimetable(timetableId)
        refreshTimetables()
        refreshLessons()
        refreshAttendance()
    }

    fun createSchoolYear(name: String, hasTwoWeeks: Boolean) {
        val id = repository.insertTimetable(name, hasTwoWeeks)
        setSchoolYearActive(id)
    }

    fun deleteSchoolYear(timetableId: Long) {
        repository.deleteTimetable(timetableId)
        refreshTimetables()
        refreshLessons()
    }

    fun duplicateSchoolYear(sourceId: Long, newYearName: String) {
        repository.duplicateTimetable(sourceId, newYearName)
        refreshTimetables()
    }

    fun promoteSchoolYear(sourceId: Long, newYearName: String) {
        repository.promoteToNextYear(sourceId, newYearName)
        refreshTimetables()
        refreshLessons()
    }

    // Lessons
    fun addLesson(lesson: Lesson) {
        repository.insertLesson(lesson)
        refreshLessons()
    }

    fun editLesson(lesson: Lesson) {
        repository.updateLesson(lesson)
        refreshLessons()
    }

    fun deleteLesson(lessonId: Long) {
        repository.deleteLesson(lessonId)
        refreshLessons()
    }

    // Tasks
    fun addTask(task: TaskItem) {
        repository.insertTask(task)
        refreshTasks()
    }

    fun toggleTask(task: TaskItem) {
        repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        refreshTasks()
    }

    fun deleteTask(taskId: Long) {
        repository.deleteTask(taskId)
        refreshTasks()
    }

    // Exams
    fun addExam(exam: ExamItem) {
        repository.insertExam(exam)
        refreshExams()
    }

    fun deleteExam(examId: Long) {
        repository.deleteExam(examId)
        refreshExams()
    }

    // Attendance
    fun updateDailyAttendance(status: String, notes: String = "") {
        val todayStr = getTodayDateString()
        repository.saveAttendanceRecord(todayStr, status, notes)
        refreshAttendance()
        updateWidget()
    }

    fun logSubjectAttendance(date: String, lessonId: Long, status: String) {
        repository.saveSubjectAttendance(date, lessonId, status)
        refreshAttendance()
    }

    // Overrides
    fun cancelLessonForToday(lessonId: Long) {
        val todayStr = getTodayDateString()
        val existing = overrides.value.find { it.lessonId == lessonId }
        val newOverride = LessonOverride(
            id = existing?.id ?: 0,
            date = todayStr,
            lessonId = lessonId,
            isCancelled = true,
            newRoom = existing?.newRoom ?: "",
            newTeacher = existing?.newTeacher ?: ""
        )
        repository.saveOverride(newOverride)
        refreshOverrides()
        updateDashboardCalculations()
        updateWidget()
    }

    fun changeRoomOrTeacherForToday(lessonId: Long, room: String, teacher: String) {
        val todayStr = getTodayDateString()
        val existing = overrides.value.find { it.lessonId == lessonId }
        val newOverride = LessonOverride(
            id = existing?.id ?: 0,
            date = todayStr,
            lessonId = lessonId,
            isCancelled = existing?.isCancelled ?: false,
            newRoom = room,
            newTeacher = teacher
        )
        repository.saveOverride(newOverride)
        refreshOverrides()
        updateDashboardCalculations()
        updateWidget()
    }

    fun clearOverridesForLessonToday(lessonId: Long) {
        val todayStr = getTodayDateString()
        repository.deleteOverride(todayStr, lessonId)
        refreshOverrides()
        updateDashboardCalculations()
        updateWidget()
    }

    // --- DASHBOARD CALCULATIONS ---

    private fun updateDashboardCalculations() {
        val active = activeTimetable.value ?: return
        val currentLessons = lessons.value
        val todayStr = getTodayDateString()

        // Get current system time and day
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMin = hour * 60 + minute

        // Map DayOfWeek (Calendar.MONDAY is 2, etc.)
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

        // Active Week logic
        val weekStr = manualWeekOverride.value ?: run {
            val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
            if (weekNum % 2 == 0) "A" else "B"
        }
        activeWeek.value = weekStr

        // Filter lessons for today
        val todayLessonsRaw = currentLessons.filter { it.dayOfWeek == sysDay }
        
        // Resolve double week support
        val todayLessonsFiltered = if (active.hasTwoWeeks) {
            todayLessonsRaw.filter { it.weekType == "BOTH" || it.weekType == weekStr }
        } else {
            todayLessonsRaw
        }

        // Apply temporary overrides for today
        val todayOverrides = overrides.value
        val todayTimeline = mutableListOf<TimelineItem>()
        var currentLesson: Lesson? = null
        var isCurrentCancelled = false
        var nextLesson: Lesson? = null

        val processedLessons = todayLessonsFiltered.map { lesson ->
            val override = todayOverrides.find { it.lessonId == lesson.id }
            val isCancelled = override?.isCancelled == true
            val finalRoom = if (override != null && override.newRoom.isNotEmpty()) override.newRoom else lesson.room
            val finalTeacher = if (override != null && override.newTeacher.isNotEmpty()) override.newTeacher else lesson.teacher
            
            val updatedLesson = lesson.copy(room = finalRoom, teacher = finalTeacher)
            updatedLesson to isCancelled
        }.sortedBy { it.first.startTimeMinutes }

        // Build Timeline with classes and breaks
        for (i in processedLessons.indices) {
            val (lesson, isCancelled) = processedLessons[i]
            
            // If there's a gap before first lesson (e.g. school starts at 08:30, and it's morning)
            if (i == 0 && lesson.startTimeMinutes > 510) { // e.g. gap before 08:30
                // We can add a custom break or skip
            }

            // Insert gap break if there's a gap between lessons
            if (i > 0) {
                val prevLesson = processedLessons[i - 1].first
                val gap = lesson.startTimeMinutes - prevLesson.endTimeMinutes
                if (gap >= 15) {
                    val breakName = if (prevLesson.endTimeMinutes >= 720 && prevLesson.endTimeMinutes <= 810) "Lunch Period" else "Break"
                    todayTimeline.add(TimelineItem.BreakPeriod(breakName, prevLesson.endTimeMinutes, lesson.startTimeMinutes))
                }
            }

            todayTimeline.add(TimelineItem.ClassPeriod(lesson, isCancelled, 
                if (lesson.room != todayLessonsFiltered.find { it.id == lesson.id }?.room) lesson.room else null,
                if (lesson.teacher != todayLessonsFiltered.find { it.id == lesson.id }?.teacher) lesson.teacher else null
            ))
        }

        // Find Current and Next Lesson
        for ((lesson, isCancelled) in processedLessons) {
            if (currentMin in lesson.startTimeMinutes until lesson.endTimeMinutes) {
                currentLesson = lesson
                isCurrentCancelled = isCancelled
            } else if (lesson.startTimeMinutes > currentMin && nextLesson == null) {
                nextLesson = lesson
            }
        }

        // Live labels & timer
        var periodLabel = "Free Time"
        var timeRemainingStr = ""
        var progress = 0f
        var endOfDay = "No classes scheduled"

        if (processedLessons.isNotEmpty()) {
            val firstLesson = processedLessons.first().first
            val lastLesson = processedLessons.last().first

            when {
                currentMin < firstLesson.startTimeMinutes -> {
                    periodLabel = "Before School"
                    val diff = firstLesson.startTimeMinutes - currentMin
                    timeRemainingStr = formatMinutesToHoursMins(diff) + " until first class"
                    progress = 0f
                    endOfDay = "Starts at ${firstLesson.startTimeString}"
                }
                currentMin >= lastLesson.endTimeMinutes -> {
                    periodLabel = "School Finished"
                    timeRemainingStr = "Enjoy your evening!"
                    progress = 1.0f
                    endOfDay = "Finished"
                }
                else -> {
                    // We are in the school day bounds
                    if (currentLesson != null) {
                        periodLabel = currentLesson.name
                        val totalDuration = currentLesson.endTimeMinutes - currentLesson.startTimeMinutes
                        val elapsed = currentMin - currentLesson.startTimeMinutes
                        progress = if (totalDuration > 0) elapsed.toFloat() / totalDuration else 0f
                        val remaining = currentLesson.endTimeMinutes - currentMin
                        timeRemainingStr = if (isCurrentCancelled) "Cancelled" else "${remaining}m remaining"
                        endOfDay = "In progress"
                    } else {
                        // We must be in a break!
                        val activeBreak = todayTimeline.filterIsInstance<TimelineItem.BreakPeriod>()
                            .find { currentMin in it.startMin until it.endMin }
                        if (activeBreak != null) {
                            periodLabel = activeBreak.name
                            val remaining = activeBreak.endMin - currentMin
                            timeRemainingStr = "${remaining}m until next class"
                            progress = 1.0f - (remaining.toFloat() / (activeBreak.endMin - activeBreak.startMin))
                            endOfDay = "Break time"
                        } else {
                            periodLabel = "Free Time"
                            timeRemainingStr = "Between classes"
                            progress = 0f
                            endOfDay = "In school"
                        }
                    }
                }
            }
        }

        // Today's general attendance
        val todayRecord = attendanceRecords.value.find { it.date == todayStr }
        val attendanceStatus = todayRecord?.status ?: "UNMARKED"

        dashboardState.value = DashboardState(
            currentLesson = currentLesson,
            isCurrentCancelled = isCurrentCancelled,
            nextLesson = nextLesson,
            currentPeriodLabel = periodLabel,
            timeRemainingString = timeRemainingStr,
            progress = progress,
            endOfDayStatus = endOfDay,
            todayLessonsWithBreaks = todayTimeline,
            todayAttendanceStatus = attendanceStatus
        )
    }

    // --- ALARM SCHEDULING ---

    fun scheduleNotifications() {
        com.grinkware.timeplans.receiver.NotificationScheduler.scheduleNextAlarm(getApplication())
    }

    // --- WIDGET TRIGGER ---

    private fun updateWidget() {
        val context = getApplication<Application>()
        val intent = Intent(context, TimePlansWidgetProvider::class.java).apply {
            action = AppWidgetManager_UPDATE_ACTION
        }
        context.sendBroadcast(intent)
    }

    // --- HELPERS ---

    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun formatMinutesToHoursMins(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return when {
            hrs > 0 -> "${hrs}h ${mins}m"
            else -> "${mins}m"
        }
    }

    fun getCountdownToEndOfYear(): String {
        val targetDateStr = settings.value.endOfYearDate
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val target = format.parse(targetDateStr) ?: return "0 days"
            val now = Date()
            val diffMs = target.time - now.time
            if (diffMs <= 0) {
                "Year Finished"
            } else {
                val days = diffMs / (1000 * 60 * 60 * 24)
                "$days days left"
            }
        } catch (e: Exception) {
            "Not Set"
        }
    }

    fun completeOnboarding(
        yearName: String,
        hasTwoWeeks: Boolean,
        showNotifications: Boolean,
        alarmLeadMinutes: Int,
        darkMode: String,
        amoledMode: Boolean,
        density: String,
        fontStyle: String,
        initialLessons: List<Lesson> = emptyList(),
        initialTasks: List<TaskItem> = emptyList()
    ) {
        // 1. Save settings
        repository.saveSetting("darkMode", darkMode)
        repository.saveSetting("amoledMode", amoledMode.toString())
        repository.saveSetting("density", density)
        repository.saveSetting("fontStyle", fontStyle)
        repository.saveSetting("showNotifications", showNotifications.toString())
        repository.saveSetting("alarmLeadMinutes", alarmLeadMinutes.toString())

        // 2. Clear any default school years and insert the user's custom school year
        val currentYears = repository.getTimetables()
        currentYears.forEach { repository.deleteTimetable(it.id) }

        val newYearId = repository.insertTimetable(yearName, hasTwoWeeks)
        repository.setActiveTimetable(newYearId)

        // Insert initial lessons and collect their IDs
        val insertedLessonIds = initialLessons.map { lesson ->
            repository.insertLesson(lesson.copy(timetableId = newYearId))
        }

        // Insert initial tasks (mapping temporary lessonIndex to inserted IDs)
        initialTasks.forEach { task ->
            val dbLessonId = task.lessonId?.let { tempIndex ->
                if (tempIndex >= 0 && tempIndex < insertedLessonIds.size) {
                    insertedLessonIds[tempIndex.toInt()]
                } else null
            }
            repository.insertTask(task.copy(lessonId = dbLessonId))
        }

        // 3. Save onboardingCompleted as true
        repository.saveSetting("onboardingCompleted", "true")

        // 4. Reload everything
        loadAllData()
    }

    companion object {
        private const val AppWidgetManager_UPDATE_ACTION = "android.appwidget.action.APPWIDGET_UPDATE"
    }
}

data class DashboardState(
    val currentLesson: Lesson? = null,
    val isCurrentCancelled: Boolean = false,
    val nextLesson: Lesson? = null,
    val currentPeriodLabel: String = "Free Time",
    val timeRemainingString: String = "",
    val progress: Float = 0f,
    val endOfDayStatus: String = "No classes scheduled",
    val todayLessonsWithBreaks: List<TimelineItem> = emptyList(),
    val todayAttendanceStatus: String = "UNMARKED"
)

sealed class TimelineItem {
    data class ClassPeriod(
        val lesson: Lesson,
        val isCancelled: Boolean,
        val roomOverridden: String?,
        val teacherOverridden: String?
    ) : TimelineItem()

    data class BreakPeriod(
        val name: String,
        val startMin: Int,
        val endMin: Int
    ) : TimelineItem()
}
