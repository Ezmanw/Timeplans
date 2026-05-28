package com.grinkware.timeplans.data

data class SchoolYear(
    val id: Long = 0,
    val yearName: String,
    val isActive: Boolean = false,
    val hasTwoWeeks: Boolean = false
)

data class Lesson(
    val id: Long = 0,
    val timetableId: Long,
    val name: String,
    val teacher: String,
    val room: String,
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val startTimeMinutes: Int, // minutes since midnight (e.g. 510 = 08:30)
    val endTimeMinutes: Int, // minutes since midnight (e.g. 570 = 09:30)
    val colorHex: Int, // Hex value of the subject color
    val notes: String = "",
    val homeworkLink: String = "",
    val weekType: String = "BOTH", // "A", "B", or "BOTH"
    val periodType: String = "CLASS" // "CLASS", "BREAK", "LUNCH", "ASSEMBLY"
) {
    val startTimeString: String
        get() = formatMinutes(startTimeMinutes)

    val endTimeString: String
        get() = formatMinutes(endTimeMinutes)

    private fun formatMinutes(minutes: Int): String {
        val hrs = minutes / 60
        val mins = minutes % 60
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", hrs, mins)
    }
}

data class AttendanceRecord(
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val status: String, // "IN", "OFF", "LATE", "HOLIDAY", "SICK"
    val notes: String = ""
)

data class TaskItem(
    val id: Long = 0,
    val lessonId: Long? = null,
    val title: String,
    val description: String = "",
    val dueDate: String, // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val taskType: String = "HOMEWORK", // "HOMEWORK", "REVISION", "EXAM"
    val priority: String = "MEDIUM" // "HIGH", "MEDIUM", "LOW"
)

data class ExamItem(
    val id: Long = 0,
    val subject: String,
    val date: String, // YYYY-MM-DD
    val time: String = "09:00", // HH:MM
    val room: String = "",
    val notes: String = ""
)

data class LessonOverride(
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val lessonId: Long,
    val isCancelled: Boolean = false,
    val newRoom: String = "",
    val newTeacher: String = ""
)

data class AppSettings(
    val darkMode: String = "AUTO", // "AUTO", "LIGHT", "DARK"
    val amoledMode: Boolean = false,
    val dynamicTheme: Boolean = true,
    val density: String = "NORMAL", // "COMPACT", "NORMAL", "SPACIOUS"
    val fontStyle: String = "SYSTEM", // "SYSTEM", "INTER", "MONOSPACE", "SANS"
    val showNotifications: Boolean = true,
    val endOfYearDate: String = "2026-07-20", // Default end of year
    val alarmLeadMinutes: Int = 10,
    val onboardingCompleted: Boolean = false,
    val todayWidgetOrder: String = "TIMELINE,COUNTDOWN,HERO,ATTENDANCE,INSIGHTS,DEADLINES",
    val todayWidgetVisibility: String = "TIMELINE,COUNTDOWN,HERO,ATTENDANCE,INSIGHTS,DEADLINES",
    val tasksSortBy: String = "DUE_ASC"
)

data class GradeEntry(
    val id: Long = 0,
    val subject: String,
    val title: String,
    val score: Double,
    val maxScore: Double,
    val weight: Double = 1.0,
    val date: String // YYYY-MM-DD
) {
    val percentage: Double
        get() = if (maxScore > 0) (score / maxScore) * 100.0 else 0.0
}

data class StudySession(
    val id: Long = 0,
    val subject: String,
    val durationMinutes: Int,
    val date: String, // YYYY-MM-DD
    val rating: Int = 0, // 0 to 5
    val reflection: String = ""
)

data class Flashcard(
    val id: Long = 0,
    val subject: String,
    val front: String,
    val back: String,
    val box: Int = 1, // 1 to 5
    val lastReviewed: String = "" // YYYY-MM-DD
)

data class StudyTarget(
    val id: Long = 0,
    val subject: String,
    val targetMinutes: Int
)

