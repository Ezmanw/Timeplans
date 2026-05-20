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
        return String.format("%02d:%02d", hrs, mins)
    }
}

data class AttendanceRecord(
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val status: String, // "IN", "OFF", "LATE", "HOLIDAY", "SICK"
    val notes: String = ""
)

data class SubjectAttendance(
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val lessonId: Long,
    val status: String // "PRESENT", "ABSENT", "LATE"
)

data class TaskItem(
    val id: Long = 0,
    val lessonId: Long? = null,
    val title: String,
    val description: String = "",
    val dueDate: String, // YYYY-MM-DD
    val isCompleted: Boolean = false,
    val taskType: String = "HOMEWORK" // "HOMEWORK", "REVISION", "EXAM"
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
    val onboardingCompleted: Boolean = false
)
