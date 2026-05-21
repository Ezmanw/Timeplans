package com.grinkware.timeplans.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "timeplans.db"
        const val DATABASE_VERSION = 5
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE timetables (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                year_name TEXT NOT NULL,
                is_active INTEGER DEFAULT 0,
                has_two_weeks INTEGER DEFAULT 0
            )
        """)

        db.execSQL("""
            CREATE TABLE lessons (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timetable_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                teacher TEXT,
                room TEXT,
                day_of_week INTEGER,
                start_time INTEGER,
                end_time INTEGER,
                color INTEGER,
                notes TEXT,
                homework_link TEXT,
                week_type TEXT DEFAULT 'BOTH',
                period_type TEXT DEFAULT 'CLASS',
                FOREIGN KEY(timetable_id) REFERENCES timetables(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("""
            CREATE TABLE attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT UNIQUE NOT NULL,
                status TEXT NOT NULL,
                notes TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE subject_attendance (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                lesson_id INTEGER NOT NULL,
                status TEXT NOT NULL,
                FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
                UNIQUE(date, lesson_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lesson_id INTEGER,
                title TEXT NOT NULL,
                description TEXT,
                due_date TEXT NOT NULL,
                is_completed INTEGER DEFAULT 0,
                task_type TEXT NOT NULL,
                priority TEXT DEFAULT 'MEDIUM',
                FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE SET NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE exams (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT NOT NULL,
                date TEXT NOT NULL,
                time TEXT DEFAULT '09:00',
                room TEXT,
                notes TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE lesson_overrides (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                date TEXT NOT NULL,
                lesson_id INTEGER NOT NULL,
                is_cancelled INTEGER DEFAULT 0,
                new_room TEXT,
                new_teacher TEXT,
                FOREIGN KEY(lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
                UNIQUE(date, lesson_id)
            )
        """)

        db.execSQL("""
            CREATE TABLE settings (
                "key" TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
        """)

        // Seed default settings
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('darkMode', 'AUTO')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('amoledMode', '0')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('dynamicTheme', '1')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('density', 'NORMAL')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('fontStyle', 'SYSTEM')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('showNotifications', '1')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('endOfYearDate', '2026-07-20')")
        db.execSQL("INSERT INTO settings (\"key\", value) VALUES ('alarmLeadMinutes', '10')")

        // Seed default school year
        db.execSQL("INSERT INTO timetables (year_name, is_active, has_two_weeks) VALUES ('Year 10', 1, 0)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS grades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT NOT NULL,
                title TEXT NOT NULL,
                score REAL NOT NULL,
                max_score REAL NOT NULL,
                weight REAL DEFAULT 1.0,
                date TEXT NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS study_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT NOT NULL,
                duration_minutes INTEGER NOT NULL,
                date TEXT NOT NULL,
                rating INTEGER DEFAULT 0,
                reflection TEXT DEFAULT ''
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flashcards (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT NOT NULL,
                front TEXT NOT NULL,
                back TEXT NOT NULL,
                box INTEGER DEFAULT 1,
                last_reviewed TEXT
            )
        """)

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS study_targets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                subject TEXT UNIQUE NOT NULL,
                target_minutes INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE lessons ADD COLUMN period_type TEXT DEFAULT 'CLASS'")
            } catch (_: Exception) {
                // Ignore
            }
        }
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS grades (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    title TEXT NOT NULL,
                    score REAL NOT NULL,
                    max_score REAL NOT NULL,
                    weight REAL DEFAULT 1.0,
                    date TEXT NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS study_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    duration_minutes INTEGER NOT NULL,
                    date TEXT NOT NULL
                )
            """)
        }
        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS flashcards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT NOT NULL,
                    front TEXT NOT NULL,
                    back TEXT NOT NULL,
                    box INTEGER DEFAULT 1,
                    last_reviewed TEXT
                )
            """)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS study_targets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject TEXT UNIQUE NOT NULL,
                    target_minutes INTEGER NOT NULL
                )
            """)
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority TEXT DEFAULT 'MEDIUM'")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN rating INTEGER DEFAULT 0")
            } catch (_: Exception) {}
            try {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN reflection TEXT DEFAULT ''")
            } catch (_: Exception) {}
        }
    }
}
