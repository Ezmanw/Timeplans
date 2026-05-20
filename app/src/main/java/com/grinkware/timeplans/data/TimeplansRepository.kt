package com.grinkware.timeplans.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

class TimeplansRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // --- TIMETABLES / SCHOOL YEARS ---

    fun getTimetables(): List<SchoolYear> {
        val list = mutableListOf<SchoolYear>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, year_name, is_active, has_two_weeks FROM timetables ORDER BY id DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                SchoolYear(
                    id = cursor.getLong(0),
                    yearName = cursor.getString(1),
                    isActive = cursor.getInt(2) == 1,
                    hasTwoWeeks = cursor.getInt(3) == 1
                )
            )
        }
        cursor.close()
        return list
    }

    fun getActiveTimetable(): SchoolYear? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, year_name, is_active, has_two_weeks FROM timetables WHERE is_active = 1 LIMIT 1", null)
        var timetable: SchoolYear? = null
        if (cursor.moveToFirst()) {
            timetable = SchoolYear(
                id = cursor.getLong(0),
                yearName = cursor.getString(1),
                isActive = cursor.getInt(2) == 1,
                hasTwoWeeks = cursor.getInt(3) == 1
            )
        }
        cursor.close()
        return timetable
    }

    fun insertTimetable(yearName: String, hasTwoWeeks: Boolean): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("year_name", yearName)
            put("has_two_weeks", if (hasTwoWeeks) 1 else 0)
            put("is_active", 0)
        }
        return db.insert("timetables", null, values)
    }

    fun setActiveTimetable(timetableId: Long) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("UPDATE timetables SET is_active = 0")
            db.execSQL("UPDATE timetables SET is_active = 1 WHERE id = ?", arrayOf(timetableId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTimetable(timetableId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("timetables", "id = ?", arrayOf(timetableId.toString()))
    }

    fun duplicateTimetable(sourceId: Long, newYearName: String): Long {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            // Find source timetable
            var hasTwoWeeks = false
            val tCursor = db.rawQuery("SELECT has_two_weeks FROM timetables WHERE id = ?", arrayOf(sourceId.toString()))
            if (tCursor.moveToFirst()) {
                hasTwoWeeks = tCursor.getInt(0) == 1
            }
            tCursor.close()

            // Insert new timetable
            val values = ContentValues().apply {
                put("year_name", newYearName)
                put("has_two_weeks", if (hasTwoWeeks) 1 else 0)
                put("is_active", 0)
            }
            val newId = db.insert("timetables", null, values)

            // Copy all lessons
            val lCursor = db.rawQuery(
                "SELECT name, teacher, room, day_of_week, start_time, end_time, color, notes, homework_link, week_type, period_type FROM lessons WHERE timetable_id = ?",
                arrayOf(sourceId.toString())
            )
            while (lCursor.moveToNext()) {
                val lValues = ContentValues().apply {
                    put("timetable_id", newId)
                    put("name", lCursor.getString(0))
                    put("teacher", lCursor.getString(1))
                    put("room", lCursor.getString(2))
                    put("day_of_week", lCursor.getInt(3))
                    put("start_time", lCursor.getInt(4))
                    put("end_time", lCursor.getInt(5))
                    put("color", lCursor.getInt(6))
                    put("notes", lCursor.getString(7))
                    put("homework_link", lCursor.getString(8))
                    put("week_type", lCursor.getString(9))
                    put("period_type", lCursor.getString(10) ?: "CLASS")
                }
                db.insert("lessons", null, lValues)
            }
            lCursor.close()

            db.setTransactionSuccessful()
            return newId
        } finally {
            db.endTransaction()
        }
    }

    fun promoteToNextYear(sourceId: Long, newYearName: String): Long {
        // Carry over teachers, rooms, colors, and notes when matching lesson names automatically.
        // For a clean implementation, this works exactly like duplicate since it is copying
        // and setting it up as the active new year, letting the user modify what changed.
        val newId = duplicateTimetable(sourceId, newYearName)
        setActiveTimetable(newId)
        return newId
    }

    // --- LESSONS ---

    fun getLessons(timetableId: Long): List<Lesson> {
        val list = mutableListOf<Lesson>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, name, teacher, room, day_of_week, start_time, end_time, color, notes, homework_link, week_type, period_type FROM lessons WHERE timetable_id = ? ORDER BY start_time ASC",
            arrayOf(timetableId.toString())
        )
        while (cursor.moveToNext()) {
            list.add(
                Lesson(
                    id = cursor.getLong(0),
                    timetableId = timetableId,
                    name = cursor.getString(1),
                    teacher = cursor.getString(2) ?: "",
                    room = cursor.getString(3) ?: "",
                    dayOfWeek = cursor.getInt(4),
                    startTimeMinutes = cursor.getInt(5),
                    endTimeMinutes = cursor.getInt(6),
                    colorHex = cursor.getInt(7),
                    notes = cursor.getString(8) ?: "",
                    homeworkLink = cursor.getString(9) ?: "",
                    weekType = cursor.getString(10) ?: "BOTH",
                    periodType = cursor.getString(11) ?: "CLASS"
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertLesson(lesson: Lesson): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("timetable_id", lesson.timetableId)
            put("name", lesson.name)
            put("teacher", lesson.teacher)
            put("room", lesson.room)
            put("day_of_week", lesson.dayOfWeek)
            put("start_time", lesson.startTimeMinutes)
            put("end_time", lesson.endTimeMinutes)
            put("color", lesson.colorHex)
            put("notes", lesson.notes)
            put("homework_link", lesson.homeworkLink)
            put("week_type", lesson.weekType)
            put("period_type", lesson.periodType)
        }
        return db.insert("lessons", null, values)
    }

    fun updateLesson(lesson: Lesson) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("name", lesson.name)
            put("teacher", lesson.teacher)
            put("room", lesson.room)
            put("day_of_week", lesson.dayOfWeek)
            put("start_time", lesson.startTimeMinutes)
            put("end_time", lesson.endTimeMinutes)
            put("color", lesson.colorHex)
            put("notes", lesson.notes)
            put("homework_link", lesson.homeworkLink)
            put("week_type", lesson.weekType)
            put("period_type", lesson.periodType)
        }
        db.update("lessons", values, "id = ?", arrayOf(lesson.id.toString()))
    }

    fun deleteLesson(lessonId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("lessons", "id = ?", arrayOf(lessonId.toString()))
    }

    // --- ATTENDANCE ---

    fun getAttendanceRecords(): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, date, status, notes FROM attendance ORDER BY date DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                AttendanceRecord(
                    id = cursor.getLong(0),
                    date = cursor.getString(1),
                    status = cursor.getString(2),
                    notes = cursor.getString(3) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun getAttendanceForDate(date: String): AttendanceRecord? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, date, status, notes FROM attendance WHERE date = ?", arrayOf(date))
        var record: AttendanceRecord? = null
        if (cursor.moveToFirst()) {
            record = AttendanceRecord(
                id = cursor.getLong(0),
                date = cursor.getString(1),
                status = cursor.getString(2),
                notes = cursor.getString(3) ?: ""
            )
        }
        cursor.close()
        return record
    }

    fun saveAttendanceRecord(date: String, status: String, notes: String = "") {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("status", status)
            put("notes", notes)
        }
        db.insertWithOnConflict("attendance", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- SUBJECT-SPECIFIC ATTENDANCE ---

    fun getSubjectAttendance(date: String): List<SubjectAttendance> {
        val list = mutableListOf<SubjectAttendance>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, date, lesson_id, status FROM subject_attendance WHERE date = ?", arrayOf(date))
        while (cursor.moveToNext()) {
            list.add(
                SubjectAttendance(
                    id = cursor.getLong(0),
                    date = cursor.getString(1),
                    lessonId = cursor.getLong(2),
                    status = cursor.getString(3)
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveSubjectAttendance(date: String, lessonId: Long, status: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("lesson_id", lessonId)
            put("status", status)
        }
        db.insertWithOnConflict("subject_attendance", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getSubjectAttendanceStats(timetableId: Long): List<SubjectStats> {
        val stats = mutableListOf<SubjectStats>()
        val db = dbHelper.readableDatabase

        // We group by lesson name so lessons in different timeslots but same subject aggregate together
        val cursor = db.rawQuery("""
            SELECT l.name, 
                   SUM(CASE WHEN sa.status = 'PRESENT' THEN 1 ELSE 0 END) as present,
                   SUM(CASE WHEN sa.status = 'ABSENT' THEN 1 ELSE 0 END) as absent,
                   SUM(CASE WHEN sa.status = 'LATE' THEN 1 ELSE 0 END) as late,
                   l.color
            FROM lessons l
            JOIN subject_attendance sa ON l.id = sa.lesson_id
            WHERE l.timetable_id = ?
            GROUP BY l.name
        """.trimIndent(), arrayOf(timetableId.toString()))

        while (cursor.moveToNext()) {
            stats.add(
                SubjectStats(
                    subjectName = cursor.getString(0),
                    present = cursor.getInt(1),
                    absent = cursor.getInt(2),
                    late = cursor.getInt(3),
                    color = cursor.getInt(4)
                )
            )
        }
        cursor.close()
        return stats
    }

    // --- TASKS (HOMEWORK, REVISION, EXAMS) ---

    fun getTasks(): List<TaskItem> {
        val list = mutableListOf<TaskItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, lesson_id, title, description, due_date, is_completed, task_type FROM tasks ORDER BY due_date ASC", null)
        while (cursor.moveToNext()) {
            list.add(
                TaskItem(
                    id = cursor.getLong(0),
                    lessonId = if (cursor.isNull(1)) null else cursor.getLong(1),
                    title = cursor.getString(2),
                    description = cursor.getString(3) ?: "",
                    dueDate = cursor.getString(4),
                    isCompleted = cursor.getInt(5) == 1,
                    taskType = cursor.getString(6)
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertTask(task: TaskItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (task.lessonId != null) put("lesson_id", task.lessonId) else putNull("lesson_id")
            put("title", task.title)
            put("description", task.description)
            put("due_date", task.dueDate)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("task_type", task.taskType)
        }
        return db.insert("tasks", null, values)
    }

    fun updateTask(task: TaskItem) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (task.lessonId != null) put("lesson_id", task.lessonId) else putNull("lesson_id")
            put("title", task.title)
            put("description", task.description)
            put("due_date", task.dueDate)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("task_type", task.taskType)
        }
        db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(taskId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("tasks", "id = ?", arrayOf(taskId.toString()))
    }

    // --- EXAMS ---

    fun getExams(): List<ExamItem> {
        val list = mutableListOf<ExamItem>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subject, date, time, room, notes FROM exams ORDER BY date ASC, time ASC", null)
        while (cursor.moveToNext()) {
            list.add(
                ExamItem(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    date = cursor.getString(2),
                    time = cursor.getString(3) ?: "09:00",
                    room = cursor.getString(4) ?: "",
                    notes = cursor.getString(5) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun insertExam(exam: ExamItem): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("subject", exam.subject)
            put("date", exam.date)
            put("time", exam.time)
            put("room", exam.room)
            put("notes", exam.notes)
        }
        return db.insert("exams", null, values)
    }

    fun updateExam(exam: ExamItem) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("subject", exam.subject)
            put("date", exam.date)
            put("time", exam.time)
            put("room", exam.room)
            put("notes", exam.notes)
        }
        db.update("exams", values, "id = ?", arrayOf(exam.id.toString()))
    }

    fun deleteExam(examId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("exams", "id = ?", arrayOf(examId.toString()))
    }

    // --- LESSON OVERRIDES ---

    fun getOverridesForDate(date: String): List<LessonOverride> {
        val list = mutableListOf<LessonOverride>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, date, lesson_id, is_cancelled, new_room, new_teacher FROM lesson_overrides WHERE date = ?", arrayOf(date))
        while (cursor.moveToNext()) {
            list.add(
                LessonOverride(
                    id = cursor.getLong(0),
                    date = cursor.getString(1),
                    lessonId = cursor.getLong(2),
                    isCancelled = cursor.getInt(3) == 1,
                    newRoom = cursor.getString(4) ?: "",
                    newTeacher = cursor.getString(5) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveOverride(override: LessonOverride) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", override.date)
            put("lesson_id", override.lessonId)
            put("is_cancelled", if (override.isCancelled) 1 else 0)
            put("new_room", override.newRoom)
            put("new_teacher", override.newTeacher)
        }
        db.insertWithOnConflict("lesson_overrides", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteOverride(date: String, lessonId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("lesson_overrides", "date = ? AND lesson_id = ?", arrayOf(date, lessonId.toString()))
    }

    // --- SETTINGS ---

    fun getSettings(): AppSettings {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT key, value FROM settings", null)
        var darkMode = "AUTO"
        var amoledMode = false
        var dynamicTheme = true
        var density = "NORMAL"
        var fontStyle = "SYSTEM"
        var showNotifications = true
        var endOfYearDate = "2026-07-20"
        var alarmLeadMinutes = 10
        var onboardingCompleted = false

        while (cursor.moveToNext()) {
            val key = cursor.getString(0)
            val value = cursor.getString(1)
            when (key) {
                "darkMode" -> darkMode = value
                "amoledMode" -> amoledMode = value == "1" || value == "true"
                "dynamicTheme" -> dynamicTheme = value == "1" || value == "true"
                "density" -> density = value
                "fontStyle" -> fontStyle = value
                "showNotifications" -> showNotifications = value == "1" || value == "true"
                "endOfYearDate" -> endOfYearDate = value
                "alarmLeadMinutes" -> alarmLeadMinutes = value.toIntOrNull() ?: 10
                "onboardingCompleted" -> onboardingCompleted = value == "1" || value == "true"
            }
        }
        cursor.close()
        return AppSettings(
            darkMode = darkMode,
            amoledMode = amoledMode,
            dynamicTheme = dynamicTheme,
            density = density,
            fontStyle = fontStyle,
            showNotifications = showNotifications,
            endOfYearDate = endOfYearDate,
            alarmLeadMinutes = alarmLeadMinutes,
            onboardingCompleted = onboardingCompleted
        )
    }

    fun saveSetting(key: String, value: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict("settings", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }
}

data class SubjectStats(
    val subjectName: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val color: Int
)
