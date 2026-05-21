package com.grinkware.timeplans.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.data.TaskItem
import com.grinkware.timeplans.data.ExamItem
import com.grinkware.timeplans.data.StudySession
import com.grinkware.timeplans.data.Flashcard
import com.grinkware.timeplans.data.GradeEntry
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.DashboardState
import com.grinkware.timeplans.ui.TimelineItem
import com.grinkware.timeplans.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val state by viewModel.dashboardState
    val spacing = LocalSpacing.current
    val activeYear = viewModel.activeTimetable.value
    val endOfYearCountdown = viewModel.getCountdownToEndOfYear()

    val todayStr = remember { viewModel.getTodayDateString() }
    val studySessions = viewModel.studySessionsList.value
    val studyTargets = viewModel.studyTargetsList.value
    val grades = viewModel.gradesList.value
    val flashcards = viewModel.flashcardsList.value

    val totalTargetMins = remember(studyTargets) { studyTargets.sumOf { it.targetMinutes } }
    val completedMins = remember(studySessions, todayStr) { getStudyMinutesThisWeek(studySessions, todayStr) }
    val studyStreak = remember(studySessions, todayStr) { calculateStudyStreak(studySessions, todayStr) }
    val dueFlashcards = remember(flashcards) { flashcards.count { isDashboardFlashcardDue(it) } }
    val tasks = viewModel.tasks.value
    val exams = viewModel.exams.value

    val upcomingDeadlines = remember(tasks, exams, todayStr) {
        val list = mutableListOf<DashboardDeadline>()
        tasks.filter { !it.isCompleted }.forEach { list.add(DashboardDeadline.TaskDeadline(it)) }
        exams.filter { it.date >= todayStr }.forEach { list.add(DashboardDeadline.ExamDeadline(it)) }
        list.sortBy { it.date }
        list
    }

    val settingsState = viewModel.settings.value
    val widgetOrder = remember(settingsState.todayWidgetOrder) {
        settingsState.todayWidgetOrder.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
    val widgetVisibility = remember(settingsState.todayWidgetVisibility) {
        settingsState.todayWidgetVisibility.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.small)
    ) {
        widgetOrder.forEach { widgetKey ->
            if (widgetVisibility.contains(widgetKey)) {
                when (widgetKey) {
                    "COUNTDOWN" -> {
                        // School Year Info & Countdown Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.medium),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = activeYear?.yearName ?: "No Year Selected",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Active School Timetable",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .padding(horizontal = spacing.small, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = endOfYearCountdown,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "HERO" -> {
                        // Today's Status Hero Card
                        item {
                            TodayHeroCard(state = state, spacing = spacing)
                        }
                    }
                    "ATTENDANCE" -> {
                        // Daily Attendance Status Quick Updater Panel
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                                ) {
                                    Text(
                                        text = "Today's Attendance Status",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tap to update your school attendance status for today.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                                    ) {
                                        val statuses = listOf(
                                            AttendanceOption("IN", "In", Color(0xFF10B981)),
                                            AttendanceOption("OFF", "Off", Color(0xFFEF4444)),
                                            AttendanceOption("LATE", "Late", Color(0xFFF59E0B)),
                                            AttendanceOption("HOLIDAY", "Holiday", Color(0xFF3B82F6)),
                                            AttendanceOption("SICK", "Sick", Color(0xFF8B5CF6))
                                        )

                                        statuses.forEach { option ->
                                            val isSelected = state.todayAttendanceStatus == option.code
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (isSelected) option.color.copy(alpha = 0.2f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                    )
                                                    .border(
                                                        width = 1.5.dp,
                                                        color = if (isSelected) option.color else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.updateDailyAttendance(option.code)
                                                    }
                                                    .padding(vertical = spacing.small),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = option.label,
                                                    color = if (isSelected) option.color else MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "INSIGHTS" -> {
                        // Progress & Insights Header
                        item {
                            Text(
                                text = "Progress & Insights",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = spacing.small)
                            )
                        }

                        // Progress & Insights Widgets Row 1 (Study Goal & Academic Standing)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    WeeklyStudyGoalCard(totalTargetMins = totalTargetMins, completedMins = completedMins, spacing = spacing)
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AcademicOverviewCard(grades = grades, spacing = spacing)
                                }
                            }
                        }

                        // Progress & Insights Widgets Row 2 (Focus Streak & Quote Card)
                        item {
                            FocusStreakQuoteCard(streakCount = studyStreak, todayStr = todayStr, spacing = spacing)
                        }

                        // Progress & Insights Widgets Row 3 (Flashcards Due Alert Card)
                        item {
                            LeitnerReviewReminderCard(dueCount = dueFlashcards, spacing = spacing)
                        }
                    }
                    "TIMELINE" -> {
                        // Timeline Header
                        item {
                            Text(
                                text = "Today's Timeline",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = spacing.small)
                            )
                        }

                        // Timeline Content
                        if (state.todayLessonsWithBreaks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = spacing.large),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "No classes",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(spacing.small))
                                        Text(
                                            text = "No classes scheduled today",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.todayLessonsWithBreaks) { item ->
                                when (item) {
                                    is TimelineItem.ClassPeriod -> {
                                        ClassPeriodRow(
                                            lesson = item.lesson,
                                            isCancelled = item.isCancelled,
                                            roomOverridden = item.roomOverridden,
                                            teacherOverridden = item.teacherOverridden,
                                            isCurrent = state.currentLesson?.id == item.lesson.id,
                                            spacing = spacing
                                        )
                                    }
                                    is TimelineItem.BreakPeriod -> {
                                        BreakPeriodRow(
                                            name = item.name,
                                            startMin = item.startMin,
                                            endMin = item.endMin,
                                            spacing = spacing
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "DEADLINES" -> {
                        // Deadlines Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = spacing.medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Upcoming Deadlines",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (upcomingDeadlines.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${upcomingDeadlines.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Deadlines Content
                        if (upcomingDeadlines.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(spacing.medium),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🎉 All caught up! No upcoming tasks or exams.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(upcomingDeadlines) { deadline ->
                                DeadlineRow(
                                    deadline = deadline,
                                    viewModel = viewModel,
                                    todayStr = todayStr,
                                    spacing = spacing
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class DashboardDeadline {
    data class TaskDeadline(val task: TaskItem) : DashboardDeadline() {
        override val date: String get() = task.dueDate
    }
    data class ExamDeadline(val exam: ExamItem) : DashboardDeadline() {
        override val date: String get() = exam.date
    }
    abstract val date: String
}

@Composable
fun DeadlineRow(
    deadline: DashboardDeadline,
    viewModel: AppViewModel,
    todayStr: String,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val diffDays = getDaysDifference(deadline.date, todayStr)
    
    // Countdown Chip Styling
    val (chipColor, chipTextColor, chipText) = when {
        diffDays < 0 -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, "Overdue")
        diffDays == 0 -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Today")
        diffDays == 1 -> Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), "Tomorrow")
        diffDays in 2..7 -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "In $diffDays d")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, deadline.date)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            when (deadline) {
                is DashboardDeadline.TaskDeadline -> {
                    val task = deadline.task
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { viewModel.toggleTask(task) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val badgeText = when (task.taskType) {
                                "HOMEWORK" -> "📝 Homework"
                                "REVISION" -> "📖 Revision"
                                else -> "📋 Task"
                            }
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            val linkedLesson = viewModel.lessons.value.find { it.id == task.lessonId }
                            if (linkedLesson != null) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = linkedLesson.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                is DashboardDeadline.ExamDeadline -> {
                    val exam = deadline.exam
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Exam",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exam: ${exam.subject}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚨 Exam",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (exam.room.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Room ${exam.room}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.labelSmall,
                    color = chipTextColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun getDaysDifference(targetDateStr: String, todayDateStr: String): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val target = sdf.parse(targetDateStr)
        val today = sdf.parse(todayDateStr)
        if (target != null && today != null) {
            val diff = target.time - today.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } else 0
    } catch (_: Exception) {
        0
    }
}

@Composable
fun TodayHeroCard(state: DashboardState, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    
    // Scale down RGB components to create rich, dark shades of the active theme colors
    val darkPrimary = Color(
        red = primary.red * 0.25f,
        green = primary.green * 0.25f,
        blue = primary.blue * 0.25f,
        alpha = 1f
    )
    val darkTertiary = Color(
        red = tertiary.red * 0.15f,
        green = tertiary.green * 0.15f,
        blue = tertiary.blue * 0.15f,
        alpha = 1f
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(darkPrimary, darkTertiary)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradientBrush)
                .padding(spacing.medium)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TODAY IN TIMEPLANS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = state.endOfDayStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = state.currentPeriodLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                val currentLesson = state.currentLesson
                if (currentLesson != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Text(
                            text = "Room ${currentLesson.room}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Teacher: ${currentLesson.teacher}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }

                Text(
                    text = state.timeRemainingString,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                val nextLesson = state.nextLesson
                if (nextLesson != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                    Text(
                        text = "Next Class: ${nextLesson.name} at ${nextLesson.startTimeString} in Room ${nextLesson.room}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
fun ClassPeriodRow(
    lesson: Lesson,
    isCancelled: Boolean,
    roomOverridden: String?,
    teacherOverridden: String?,
    isCurrent: Boolean,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val cardColor = Color(lesson.colorHex)
    val isAcademic = lesson.periodType == "CLASS" || lesson.periodType == "ASSEMBLY"
    val displayName = when (lesson.periodType) {
        "BREAK" -> if (lesson.name.equals("Break", ignoreCase = true)) "☕ Break" else "☕ ${lesson.name}"
        "LUNCH" -> if (lesson.name.equals("Lunch", ignoreCase = true)) "🍴 Lunch" else "🍴 ${lesson.name}"
        "ASSEMBLY" -> if (lesson.name.equals("Assembly", ignoreCase = true)) "📢 Assembly" else "📢 ${lesson.name}"
        else -> lesson.name
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        // Time markers
        Column(
            modifier = Modifier
                .width(55.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = lesson.startTimeString,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(
                        if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(vertical = 4.dp)
            )
            Text(
                text = lesson.endTimeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Lesson Card
        Card(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = if (isCurrent) 2.dp else 0.dp,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (lesson.periodType == "BREAK" || lesson.periodType == "LUNCH") {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Colored left accent bar
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(6.dp)
                        .background(cardColor)
                )

                Column(
                    modifier = Modifier
                        .padding(spacing.medium)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayName,
                            fontWeight = FontWeight.Bold,
                            color = if (isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                            style = if (isCancelled) MaterialTheme.typography.titleMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough) else MaterialTheme.typography.titleMedium
                        )
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NOW",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (isAcademic && (lesson.room.isNotEmpty() || lesson.teacher.isNotEmpty())) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            if (lesson.room.isNotEmpty()) {
                                Text(
                                    text = "Room ${lesson.room}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (roomOverridden != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (roomOverridden != null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (lesson.room.isNotEmpty() && lesson.teacher.isNotEmpty()) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (lesson.teacher.isNotEmpty()) {
                                Text(
                                    text = lesson.teacher,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (teacherOverridden != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (teacherOverridden != null) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    if (isCancelled) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Cancelled",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = "Cancelled for Today",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakPeriodRow(
    name: String,
    startMin: Int,
    endMin: Int,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val duration = endMin - startMin
    val startTimeStr = String.format(Locale.getDefault(), "%02d:%02d", startMin / 60, startMin % 60)
    val endTimeStr = String.format(Locale.getDefault(), "%02d:%02d", endMin / 60, endMin % 60)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Column(
            modifier = Modifier
                .width(55.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = startTimeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Text(
                text = endTimeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium, vertical = spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${duration}m duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class AttendanceOption(
    val code: String,
    val label: String,
    val color: Color
)

// --- DASHBOARD WIDGETS ---

@Composable
fun WeeklyStudyGoalCard(
    totalTargetMins: Int,
    completedMins: Int,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Text(
                text = "Weekly Study Target",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (totalTargetMins > 0) {
                val progress = (completedMins.toFloat() / totalTargetMins).coerceIn(0f, 1f)
                val percentage = (progress * 100).toInt()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                        )
                    }
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                val targetHours = totalTargetMins / 60f
                val completedHours = completedMins / 60f
                Text(
                    text = String.format(Locale.getDefault(), "%.1f / %.1f hours", completedHours, targetHours),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = if (progress >= 1f) "🎉 Goal completed! Outstanding job!" else "Keep studying to reach your weekly goal!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "No study targets set. Set weekly targets in the Stats tab to track progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class DashboardGradeStats(
    val averagePercent: Double,
    val gpaStr: String,
    val standingBadge: String,
    val badgeColor: Color
)

@Composable
fun AcademicOverviewCard(
    grades: List<GradeEntry>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val stats = remember(grades) {
        if (grades.isEmpty()) {
            DashboardGradeStats(0.0, "N/A", "No grades logged yet", Color.Gray)
        } else {
            val subjectsWithGrades = grades.map { it.subject }.distinct()
            val subjectAverages = subjectsWithGrades.associateWith { sub ->
                val subGrades = grades.filter { it.subject == sub }
                val weightSum = subGrades.sumOf { it.weight }
                if (weightSum > 0.0) {
                    subGrades.sumOf { it.percentage * it.weight } / weightSum
                } else {
                    0.0
                }
            }
            
            val avg = if (subjectAverages.isNotEmpty()) {
                subjectAverages.values.average()
            } else {
                0.0
            }
            
            val subjectGPAs = subjectAverages.mapValues { (_, percentage) ->
                when {
                    percentage >= 90.0 -> 4.0
                    percentage >= 80.0 -> 3.5
                    percentage >= 70.0 -> 3.0
                    percentage >= 60.0 -> 2.5
                    percentage >= 50.0 -> 2.0
                    else -> 1.0
                }
            }
            val overallGPA = if (subjectGPAs.isNotEmpty()) subjectGPAs.values.average() else 0.0
            val gpaFormatted = String.format(Locale.getDefault(), "%.2f", overallGPA)
            
            val (badge, color) = when {
                avg >= 90.0 -> "Outstanding 🌟" to Color(0xFF10B981)
                avg >= 80.0 -> "Excellent 📈" to Color(0xFF3B82F6)
                avg >= 70.0 -> "Good 👍" to Color(0xFFF59E0B)
                avg >= 60.0 -> "Satisfactory 📖" to Color(0xFF8B5CF6)
                else -> "Needs Focus 🎯" to Color(0xFFEF4444)
            }
            DashboardGradeStats(avg, gpaFormatted, badge, color)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grades & GPA",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (grades.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(stats.badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stats.standingBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = stats.badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (grades.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", stats.averagePercent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Average",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stats.gpaStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "GPA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                val progress = (stats.averagePercent.toFloat() / 100f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress)
                            .clip(CircleShape)
                            .background(stats.badgeColor)
                    )
                }
            } else {
                Text(
                    text = "Track your grades and estimated GPA by logging results in the Stats tab.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FocusStreakQuoteCard(
    streakCount: Int,
    todayStr: String,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Focus Streak & Motivation",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$streakCount Days 🔥",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            val quotes = remember {
                listOf(
                    "The secret of getting ahead is getting started. — Mark Twain",
                    "It always seems impossible until it's done. — Nelson Mandela",
                    "Focus on being productive instead of busy. — Tim Ferriss",
                    "There are no shortcuts to any place worth going. — Beverly Sills",
                    "Do something today that your future self will thank you for.",
                    "Small progress is still progress. Keep going!",
                    "Success is the sum of small efforts, repeated day in and day out.",
                    "Your talent determines what you can do. Your motivation determines how much you are willing to do.",
                    "Start where you are. Use what you have. Do what you can. — Arthur Ashe",
                    "Believe you can and you're halfway there. — Theodore Roosevelt",
                    "Quality is not an act, it is a habit. — Aristotle",
                    "The mind is not a vessel to be filled, but a fire to be kindled. — Plutarch"
                )
            }
            val quoteIndex = remember(todayStr) {
                val hash = todayStr.hashCode()
                if (hash == Int.MIN_VALUE) 0 else Math.abs(hash) % quotes.size
            }
            val dailyQuote = quotes[quoteIndex]

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(spacing.medium)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = dailyQuote,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun LeitnerReviewReminderCard(
    dueCount: Int,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    if (dueCount > 0) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Flashcards Due for Review",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "You have $dueCount flashcards due for review. Refresh your memory now!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✅", fontSize = 18.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Flashcard Palace Pristine",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "All caught up! No flashcards require review right now.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- CALCULATION HELPERS ---

private fun getStudyMinutesThisWeek(sessions: List<StudySession>, todayStr: String): Int {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val startOfWeekStr = sdf.format(cal.time)
    
    return sessions.filter { it.date >= startOfWeekStr && it.date <= todayStr }
        .sumOf { it.durationMinutes }
}

private fun calculateStudyStreak(sessions: List<StudySession>, todayStr: String): Int {
    val dates = sessions.filter { it.durationMinutes > 0 }.map { it.date }.toSet()
    if (dates.isEmpty()) return 0
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val yesterdayCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val yesterdayStr = sdf.format(yesterdayCal.time)
    
    if (!dates.contains(todayStr) && !dates.contains(yesterdayStr)) {
        return 0
    }
    
    var streak = 0
    val checkCal = Calendar.getInstance()
    
    if (dates.contains(todayStr)) {
        streak++
        checkCal.add(Calendar.DAY_OF_YEAR, -1)
    } else if (dates.contains(yesterdayStr)) {
        streak++
        checkCal.add(Calendar.DAY_OF_YEAR, -2)
    }
    
    while (true) {
        val checkStr = sdf.format(checkCal.time)
        if (dates.contains(checkStr)) {
            streak++
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            break
        }
    }
    return streak
}

private fun isDashboardFlashcardDue(card: Flashcard): Boolean {
    if (card.lastReviewed.isEmpty()) return true
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val lastDate = sdf.parse(card.lastReviewed) ?: return true
        val cal = Calendar.getInstance()
        val today = cal.time

        val diffMs = today.time - lastDate.time
        val diffDays = diffMs / (1000L * 60 * 60 * 24)

        val requiredInterval = when (card.box) {
            1 -> 0L
            2 -> 2L
            3 -> 4L
            4 -> 7L
            5 -> 14L
            else -> 0L
        }
        diffDays >= requiredInterval
    } catch (_: Exception) {
        true
    }
}
