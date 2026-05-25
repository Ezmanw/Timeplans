package com.grinkware.timeplans.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import androidx.core.database.sqlite.transaction

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
        db.transaction {
            execSQL("UPDATE timetables SET is_active = 0")
            execSQL("UPDATE timetables SET is_active = 1 WHERE id = ?", arrayOf(timetableId))
        }
    }

    fun deleteTimetable(timetableId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("timetables", "id = ?", arrayOf(timetableId.toString()))
    }

    fun duplicateTimetable(sourceId: Long, newYearName: String): Long {
        val db = dbHelper.writableDatabase
        return db.transaction {
            // Find source timetable
            var hasTwoWeeks = false
            val tCursor = rawQuery("SELECT has_two_weeks FROM timetables WHERE id = ?", arrayOf(sourceId.toString()))
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
            val newId = insert("timetables", null, values)

            // Copy all lessons
            val lCursor = rawQuery(
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
                insert("lessons", null, lValues)
            }
            lCursor.close()
            newId
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
    fun saveAttendanceRecord(date: String, status: String, notes: String = "") {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("date", date)
            put("status", status)
            put("notes", notes)
        }
        db.insertWithOnConflict("attendance", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
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
        val cursor = db.rawQuery("SELECT id, lesson_id, title, description, due_date, is_completed, task_type, priority FROM tasks ORDER BY due_date ASC", null)
        while (cursor.moveToNext()) {
            list.add(
                TaskItem(
                    id = cursor.getLong(0),
                    lessonId = if (cursor.isNull(1)) null else cursor.getLong(1),
                    title = cursor.getString(2),
                    description = cursor.getString(3) ?: "",
                    dueDate = cursor.getString(4),
                    isCompleted = cursor.getInt(5) == 1,
                    taskType = cursor.getString(6),
                    priority = cursor.getString(7) ?: "MEDIUM"
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
            put("priority", task.priority)
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
            put("priority", task.priority)
        }
        db.update("tasks", values, "id = ?", arrayOf(task.id.toString()))
    }

    fun deleteTask(taskId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("tasks", "id = ?", arrayOf(taskId.toString()))
    }

    fun deleteCompletedTasks(taskType: String) {
        val db = dbHelper.writableDatabase
        db.delete("tasks", "is_completed = 1 AND task_type = ?", arrayOf(taskType))
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


    fun deleteExam(examId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("exams", "id = ?", arrayOf(examId.toString()))
    }

    fun deletePastExams(currentDate: String) {
        val db = dbHelper.writableDatabase
        db.delete("exams", "date < ?", arrayOf(currentDate))
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
        val cursor = db.rawQuery("SELECT \"key\", value FROM settings", null)
        var darkMode = "AUTO"
        var amoledMode = false
        var dynamicTheme = true
        var density = "NORMAL"
        var fontStyle = "SYSTEM"
        var showNotifications = true
        var endOfYearDate = "2026-07-20"
        var alarmLeadMinutes = 10
        var onboardingCompleted = false
        var todayWidgetOrder = "TIMELINE,COUNTDOWN,HERO,ATTENDANCE,INSIGHTS,DEADLINES"
        var todayWidgetVisibility = "TIMELINE,COUNTDOWN,HERO,ATTENDANCE,INSIGHTS,DEADLINES"

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
                "todayWidgetOrder" -> todayWidgetOrder = value
                "todayWidgetVisibility" -> todayWidgetVisibility = value
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
            onboardingCompleted = onboardingCompleted,
            todayWidgetOrder = todayWidgetOrder,
            todayWidgetVisibility = todayWidgetVisibility
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

    // --- GRADES ---

    fun getGrades(): List<GradeEntry> {
        val list = mutableListOf<GradeEntry>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subject, title, score, max_score, weight, date FROM grades ORDER BY date DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                GradeEntry(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    title = cursor.getString(2),
                    score = cursor.getDouble(3),
                    maxScore = cursor.getDouble(4),
                    weight = cursor.getDouble(5),
                    date = cursor.getString(6)
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveGrade(grade: GradeEntry): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (grade.id > 0) {
                put("id", grade.id)
            }
            put("subject", grade.subject)
            put("title", grade.title)
            put("score", grade.score)
            put("max_score", grade.maxScore)
            put("weight", grade.weight)
            put("date", grade.date)
        }
        return db.insertWithOnConflict("grades", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteGrade(gradeId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("grades", "id = ?", arrayOf(gradeId.toString()))
    }

    // --- STUDY SESSIONS ---

    fun getStudySessions(): List<StudySession> {
        val list = mutableListOf<StudySession>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subject, duration_minutes, date, rating, reflection FROM study_sessions ORDER BY date DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                StudySession(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    durationMinutes = cursor.getInt(2),
                    date = cursor.getString(3),
                    rating = cursor.getInt(4),
                    reflection = cursor.getString(5) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveStudySession(session: StudySession): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (session.id > 0) {
                put("id", session.id)
            }
            put("subject", session.subject)
            put("duration_minutes", session.durationMinutes)
            put("date", session.date)
            put("rating", session.rating)
            put("reflection", session.reflection)
        }
        return db.insertWithOnConflict("study_sessions", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteStudySession(sessionId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("study_sessions", "id = ?", arrayOf(sessionId.toString()))
    }

    // --- FLASHCARDS ---

    fun getFlashcards(): List<Flashcard> {
        val list = mutableListOf<Flashcard>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subject, front, back, box, last_reviewed FROM flashcards ORDER BY id DESC", null)
        while (cursor.moveToNext()) {
            list.add(
                Flashcard(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    front = cursor.getString(2),
                    back = cursor.getString(3),
                    box = cursor.getInt(4),
                    lastReviewed = cursor.getString(5) ?: ""
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveFlashcard(flashcard: Flashcard): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (flashcard.id > 0) {
                put("id", flashcard.id)
            }
            put("subject", flashcard.subject)
            put("front", flashcard.front)
            put("back", flashcard.back)
            put("box", flashcard.box)
            put("last_reviewed", flashcard.lastReviewed)
        }
        return db.insertWithOnConflict("flashcards", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteFlashcard(flashcardId: Long) {
        val db = dbHelper.writableDatabase
        db.delete("flashcards", "id = ?", arrayOf(flashcardId.toString()))
    }

    // --- STUDY TARGETS ---

    fun getStudyTargets(): List<StudyTarget> {
        val list = mutableListOf<StudyTarget>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT id, subject, target_minutes FROM study_targets", null)
        while (cursor.moveToNext()) {
            list.add(
                StudyTarget(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    targetMinutes = cursor.getInt(2)
                )
            )
        }
        cursor.close()
        return list
    }

    fun saveStudyTarget(target: StudyTarget): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (target.id > 0) {
                put("id", target.id)
            }
            put("subject", target.subject)
            put("target_minutes", target.targetMinutes)
        }
        return db.insertWithOnConflict("study_targets", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteStudyTarget(subject: String) {
        val db = dbHelper.writableDatabase
        db.delete("study_targets", "subject = ?", arrayOf(subject))
    }

    fun backupAllDataJSON(): String {
        val backupJson = JSONObject()
        val db = dbHelper.readableDatabase
        val tables = listOf(
            "timetables", "lessons", "attendance", "subject_attendance",
            "tasks", "exams", "lesson_overrides", "settings",
            "grades", "study_sessions", "flashcards", "study_targets"
        )
        
        for (table in tables) {
            val tableArray = JSONArray()
            val cursor = db.rawQuery("SELECT * FROM $table", null)
            val columnNames = cursor.columnNames
            while (cursor.moveToNext()) {
                val row = JSONObject()
                for (i in columnNames.indices) {
                    val columnName = columnNames[i]
                    if (cursor.isNull(i)) {
                        row.put(columnName, JSONObject.NULL)
                    } else {
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> row.put(columnName, cursor.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(columnName, cursor.getDouble(i))
                            Cursor.FIELD_TYPE_STRING -> row.put(columnName, cursor.getString(i))
                            Cursor.FIELD_TYPE_BLOB -> {
                                val blob = cursor.getBlob(i)
                                val base64Blob = Base64.encodeToString(blob, Base64.DEFAULT)
                                row.put(columnName, base64Blob)
                            }
                            else -> row.put(columnName, cursor.getString(i))
                        }
                    }
                }
                tableArray.put(row)
            }
            cursor.close()
            backupJson.put(table, tableArray)
        }
        
        val jsonStr = backupJson.toString()
        return Base64.encodeToString(jsonStr.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    }

    fun restoreAllDataJSON(backupCode: String): Boolean {
        val db = dbHelper.writableDatabase
        return try {
            db.transaction {
                val decodedBytes = Base64.decode(backupCode, Base64.DEFAULT)
                val jsonStr = String(decodedBytes, StandardCharsets.UTF_8)
                val backupJson = JSONObject(jsonStr)
                
                val tables = listOf(
                    "timetables", "lessons", "attendance", "subject_attendance",
                    "tasks", "exams", "lesson_overrides", "settings",
                    "grades", "study_sessions", "flashcards", "study_targets"
                )
                
                // Delete all rows in reverse order to respect foreign keys
                val deleteTables = listOf(
                    "study_targets", "flashcards", "study_sessions", "grades",
                    "settings", "lesson_overrides", "exams", "tasks",
                    "subject_attendance", "attendance", "lessons", "timetables"
                )
                for (table in deleteTables) {
                    delete(table, null, null)
                }
                
                // Insert all rows in forward order to respect foreign keys
                for (table in tables) {
                    if (!backupJson.has(table)) continue
                    val tableArray = backupJson.getJSONArray(table)
                    for (i in 0 until tableArray.length()) {
                        val row = tableArray.getJSONObject(i)
                        val values = ContentValues()
                        val keys = row.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = row.get(key)
                            if (value == JSONObject.NULL) {
                                values.putNull(key)
                            } else if (value is Long || value is Int) {
                                values.put(key, (value as Number).toLong())
                            } else if (value is Double) {
                                values.put(key, value)
                            } else if (value is Boolean) {
                                values.put(key, if (value) 1 else 0)
                            } else {
                                values.put(key, value.toString())
                            }
                        }
                        insert(table, null, values)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLocalRestorePoints(): List<Pair<String, String>> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Pair<String, String>>()
        for (i in 1..3) {
            val cursorTime = db.rawQuery("SELECT value FROM settings WHERE \"key\" = ?", arrayOf("backup_${i}_time"))
            var time = ""
            if (cursorTime.moveToFirst()) {
                time = cursorTime.getString(0)
            }
            cursorTime.close()
            
            val cursorCode = db.rawQuery("SELECT value FROM settings WHERE \"key\" = ?", arrayOf("backup_${i}_code"))
            var code = ""
            if (cursorCode.moveToFirst()) {
                code = cursorCode.getString(0)
            }
            cursorCode.close()
            
            if (time.isNotEmpty() && code.isNotEmpty()) {
                list.add(Pair(time, code))
            }
        }
        return list
    }

    fun createLocalRestorePoint(backupCode: String, timestamp: String) {
        val db = dbHelper.writableDatabase
        db.transaction {
            // Shift 2 to 3
            var code2 = ""
            var time2 = ""
            val c2 = rawQuery("SELECT value FROM settings WHERE \"key\" = 'backup_2_code'", null)
            if (c2.moveToFirst()) code2 = c2.getString(0)
            c2.close()
            val t2 = rawQuery("SELECT value FROM settings WHERE \"key\" = 'backup_2_time'", null)
            if (t2.moveToFirst()) time2 = t2.getString(0)
            t2.close()
            
            if (code2.isNotEmpty() && time2.isNotEmpty()) {
                execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_3_code', ?)", arrayOf(code2))
                execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_3_time', ?)", arrayOf(time2))
            }
            
            // Shift 1 to 2
            var code1 = ""
            var time1 = ""
            val c1 = rawQuery("SELECT value FROM settings WHERE \"key\" = 'backup_1_code'", null)
            if (c1.moveToFirst()) code1 = c1.getString(0)
            c1.close()
            val t1 = rawQuery("SELECT value FROM settings WHERE \"key\" = 'backup_1_time'", null)
            if (t1.moveToFirst()) time1 = t1.getString(0)
            t1.close()
            
            if (code1.isNotEmpty() && time1.isNotEmpty()) {
                execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_2_code', ?)", arrayOf(code1))
                execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_2_time', ?)", arrayOf(time1))
            }
            
            // Save new as 1
            execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_1_code', ?)", arrayOf(backupCode))
            execSQL("INSERT OR REPLACE INTO settings (\"key\", value) VALUES ('backup_1_time', ?)", arrayOf(timestamp))
        }
    }
}

data class SubjectStats(
    val subjectName: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val color: Int
)
