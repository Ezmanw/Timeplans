package com.grinkware.timeplans.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.AttendanceRecord
import com.grinkware.timeplans.data.SubjectStats
import com.grinkware.timeplans.data.GradeEntry
import com.grinkware.timeplans.data.StudySession
import com.grinkware.timeplans.data.Flashcard
import com.grinkware.timeplans.data.StudyTarget
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.drawscope.clipPath

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(viewModel: AppViewModel) {
    var selectedSubTab by remember { mutableStateOf(0) }
    val spacing = LocalSpacing.current

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            val tabs = listOf("Overview", "Attendance", "Grades", "Focus Timer", "Flashcards")
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedSubTab) {
                0 -> OverviewTab(viewModel, spacing)
                1 -> AttendanceTab(viewModel, spacing)
                2 -> GradesTab(viewModel, spacing)
                3 -> FocusTimerTab(viewModel, spacing)
                4 -> FlashcardsTab(viewModel, spacing)
            }
        }
    }
}

@Composable
fun OverviewTab(viewModel: AppViewModel, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val records = viewModel.attendanceRecords.value
    val grades = viewModel.gradesList.value
    val studySessions = viewModel.studySessionsList.value
    val lessons = viewModel.lessons.value

    // 1. Calculate Attendance
    val totalDays = records.size
    val inDays = records.count { it.status == "IN" }
    val lateDays = records.count { it.status == "LATE" }
    val holidayDays = records.count { it.status == "HOLIDAY" }
    val divisor = totalDays - holidayDays
    val attendancePercentage = if (divisor > 0) {
        ((inDays + lateDays).toFloat() / divisor * 100)
    } else {
        100f
    }

    // 2. Calculate GPA & Subject Averages
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
    val subjectGPAs = subjectAverages.mapValues { (_, percentage) ->
        when {
            percentage >= 90.0 -> 4.0
            percentage >= 80.0 -> 3.0
            percentage >= 70.0 -> 2.0
            percentage >= 60.0 -> 1.0
            else -> 0.0
        }
    }
    val cumulativeGpa = if (subjectGPAs.isNotEmpty()) {
        subjectGPAs.values.average()
    } else {
        Double.NaN
    }

    // 3. Calculate Focus Time
    val totalStudyMinutes = studySessions.sumOf { it.durationMinutes }
    val totalStudyHoursString = if (totalStudyMinutes >= 60) {
        val hrs = totalStudyMinutes / 60
        val mins = totalStudyMinutes % 60
        if (mins > 0) "${hrs}h ${mins}m" else "${hrs}h"
    } else {
        "${totalStudyMinutes}m"
    }

    // 4. Subject study minutes mapping
    val subjectStudyTime = studySessions.groupBy { it.subject }
        .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }
        .toList()
        .sortedByDescending { it.second }

    // 5. Generate warning notifications/alerts
    val alerts = remember(attendancePercentage, subjectAverages) {
        val list = mutableListOf<String>()
        if (attendancePercentage < 90f && totalDays > 0) {
            list.add("Overall attendance (${String.format(Locale.getDefault(), "%.1f%%", attendancePercentage)}) is below the target threshold (90%). Try to log more classes as present.")
        }
        subjectAverages.forEach { (sub, avg) ->
            if (avg < 60.0) {
                list.add("Performance concern in $sub: Weighted average is low (${String.format(Locale.getDefault(), "%.1f%%", avg)}). We recommend adding revision time.")
            }
        }
        list
    }

    // 6. 7-Day Study Activity Data
    val last7DaysData = remember(studySessions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("EEE", Locale.getDefault())
        
        val days = (0..6).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            c.time
        }.reversed()
        
        days.map { date ->
            val dateStr = sdf.format(date)
            val label = displaySdf.format(date)
            val minutes = studySessions.filter { it.date == dateStr }.sumOf { it.durationMinutes }
            Pair(label, minutes)
        }
    }

    // 7. Subject Distribution Data for Donut Chart
    val subjectDistribution = remember(studySessions, lessons) {
        val totalMins = studySessions.sumOf { it.durationMinutes }.toFloat()
        if (totalMins == 0f) emptyList()
        else {
            studySessions.groupBy { it.subject }
                .map { (sub, sessions) ->
                    val mins = sessions.sumOf { it.durationMinutes }
                    val lessonMatch = lessons.find { it.name.equals(sub, ignoreCase = true) }
                    val colorHex = lessonMatch?.colorHex ?: 0xFF6200EE.toInt()
                    Triple(sub, mins, Color(colorHex))
                }
                .sortedByDescending { it.second }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.medium)
    ) {
        // Quick Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // GPA Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Est. GPA", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (cumulativeGpa.isNaN()) "—" else String.format(Locale.getDefault(), "%.2f", cumulativeGpa),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${subjectsWithGrades.size} Course(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Revision Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(spacing.medium),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Study Time", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = totalStudyHoursString,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${studySessions.size} Session(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Attendance Percentage Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Attendance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Overall Attendance Rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", attendancePercentage),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = if (attendancePercentage >= 90f) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                }
            }
        }

        // Notifications/Alerts Section
        if (alerts.isNotEmpty()) {
            item {
                Text("Performance Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(alerts) { alert ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(spacing.medium))
                        Text(
                            text = alert,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Charts Section
        item {
            StudyActivityChart(daysData = last7DaysData, spacing = spacing)
        }

        if (subjectDistribution.isNotEmpty()) {
            item {
                SubjectDistributionDonutChart(
                    distribution = subjectDistribution,
                    totalMinutes = totalStudyMinutes,
                    spacing = spacing
                )
            }
        }

        // Top Revision Subjects Section
        item {
            Text("Focus Study by Subject", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (subjectStudyTime.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                        Text("No revision hours logged yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(subjectStudyTime) { pair ->
                val sub = pair.first
                val minutes = pair.second
                val lessonMatch = lessons.find { it.name.equals(sub, ignoreCase = true) }
                val colorHex = lessonMatch?.colorHex ?: 0xFF6200EE.toInt()
                val hrs = minutes / 60
                val mins = minutes % 60
                val durationText = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(colorHex)))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sub, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(durationText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = Color(colorHex))
                        }
                    }
                }
            }
        }

        // Weekly Study Targets Section
        item {
            var showEditTargetsDialog by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Weekly Study Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showEditTargetsDialog = true }) {
                    Text("Set Goals")
                }
            }
            if (showEditTargetsDialog) {
                EditTargetsDialog(
                    viewModel = viewModel,
                    onDismiss = { showEditTargetsDialog = false }
                )
            }
        }

        val targets = viewModel.studyTargetsList.value
        val distinctSubjects = lessons.filter { it.periodType == "CLASS" }.map { it.name }.distinct().sorted()
        val currentWeekSessions = studySessions.filter { isDateInCurrentWeek(it.date) }
        val weeklySubjectStudyTime = currentWeekSessions.groupBy { it.subject }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }

        if (distinctSubjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                        Text("Add subjects to your timetable to set weekly goals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(distinctSubjects) { sub ->
                val targetMinutes = targets.find { it.subject.equals(sub, ignoreCase = true) }?.targetMinutes ?: 0
                val actualMinutes = weeklySubjectStudyTime.entries.find { it.key.equals(sub, ignoreCase = true) }?.value ?: 0
                val lessonMatch = lessons.find { it.name.equals(sub, ignoreCase = true) }
                val colorHex = lessonMatch?.colorHex ?: 0xFF6200EE.toInt()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                    ) {
                        Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(colorHex)))
                        Column(
                            modifier = Modifier.weight(1f).padding(spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(sub, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                if (targetMinutes > 0) {
                                    Text(
                                        text = "$actualMinutes / ${targetMinutes}m",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Color(colorHex)
                                    )
                                } else {
                                    Text(
                                        text = "No goal set",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (targetMinutes > 0) {
                                val pct = (actualMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
                                LinearProgressIndicator(
                                    progress = { pct },
                                    color = Color(colorHex),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )
                                Text(
                                    text = if (actualMinutes >= targetMinutes) "Goal achieved! 🎉" else "${targetMinutes - actualMinutes}m remaining this week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (actualMinutes >= targetMinutes) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
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
fun AttendanceTab(viewModel: AppViewModel, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val records = viewModel.attendanceRecords.value
    val subjectStats = viewModel.subjectStats.value

    // Calculations
    val totalDays = records.size
    val inDays = records.count { it.status == "IN" }
    val lateDays = records.count { it.status == "LATE" }
    val offDays = records.count { it.status == "OFF" }
    val holidayDays = records.count { it.status == "HOLIDAY" }

    // Attendance % calculation
    val divisor = totalDays - holidayDays
    val attendancePercentage = if (divisor > 0) {
        ((inDays + lateDays).toFloat() / divisor * 100)
    } else {
        100f
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.medium)
    ) {
        // Hero Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Based on $totalDays total days logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))

                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                            StatMiniCounter(label = "In", count = inDays, color = Color(0xFF10B981))
                            StatMiniCounter(label = "Late", count = lateDays, color = Color(0xFFF59E0B))
                            StatMiniCounter(label = "Absent", count = offDays, color = Color(0xFFEF4444))
                        }
                    }

                    // Canvas Circular Progress Ring
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.surfaceVariant
                        Canvas(modifier = Modifier.size(90.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = strokeColor,
                                startAngle = -90f,
                                sweepAngle = (attendancePercentage / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", attendancePercentage),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Attendance",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Monthly Breakdown Chart Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(spacing.medium)) {
                    Text(
                        text = "Monthly Logging Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))

                    val monthlyData = remember(records) { getMonthlyBreakdown(records) }
                    MonthlyBarChart(data = monthlyData)
                }
            }
        }

        // Subject-Specific Attendance
        item {
            Text(
                text = "Subject Attendance",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        if (subjectStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No subject attendance logged yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(subjectStats) { stats ->
                SubjectAttendanceRow(stats = stats, spacing = spacing)
            }
        }

        // Term Breakdowns
        item {
            Text(
                text = "Term Summaries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            val terms = remember(records) { getTermBreakdown(records) }
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                terms.forEach { term ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(term.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Logged days: ${term.totalDays}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f%%", term.percentage),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = if (term.percentage >= 90f) Color(0xFF10B981) else Color(0xFFF59E0B)
                                )
                                Text("Attendance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesTab(viewModel: AppViewModel, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val grades = viewModel.gradesList.value
    val lessons = viewModel.lessons.value

    var showAddDialog by remember { mutableStateOf(false) }
    var showMockDialog by remember { mutableStateOf(false) }

    // Simulator State: list of temporary mock grades
    val mockGrades = remember { mutableStateListOf<GradeEntry>() }
    var simulatorExpanded by remember { mutableStateOf(false) }

    // Calculation function for GPA & Averages given a list of grades
    fun calculateStats(allGrades: List<GradeEntry>): Pair<Double, Map<String, Double>> {
        val subjects = allGrades.map { it.subject }.distinct()
        val subjectAverages = subjects.associateWith { sub ->
            val subGrades = allGrades.filter { it.subject == sub }
            val weightSum = subGrades.sumOf { it.weight }
            if (weightSum > 0.0) {
                subGrades.sumOf { it.percentage * it.weight } / weightSum
            } else {
                0.0
            }
        }
        val subjectGPAs = subjectAverages.mapValues { (_, percentage) ->
            when {
                percentage >= 90.0 -> 4.0
                percentage >= 80.0 -> 3.0
                percentage >= 70.0 -> 2.0
                percentage >= 60.0 -> 1.0
                else -> 0.0
            }
        }
        val cumulativeGpa = if (subjectGPAs.isNotEmpty()) {
            subjectGPAs.values.average()
        } else {
            Double.NaN
        }
        return Pair(cumulativeGpa, subjectAverages)
    }

    // Calculate real stats
    val (realGpa, realAverages) = remember(grades) { calculateStats(grades) }

    // Combine real and mock grades for simulated stats
    val combinedGrades = remember(grades, mockGrades.toList()) {
        grades + mockGrades
    }
    val (simGpa, simAverages) = remember(combinedGrades) { calculateStats(combinedGrades) }
    val gradesBySubject = remember(grades) { grades.groupBy { it.subject } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = 100.dp, top = spacing.medium)
        ) {
            // GPA Simulator Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { simulatorExpanded = !simulatorExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Simulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "GPA & Grade Simulator",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            IconButton(onClick = { simulatorExpanded = !simulatorExpanded }) {
                                Icon(
                                    imageVector = if (simulatorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Simulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Simulator Stats Overview
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Real GPA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (realGpa.isNaN()) "—" else String.format(Locale.getDefault(), "%.2f", realGpa),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Simulated GPA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                val simGpaColor = if (simGpa > realGpa || (realGpa.isNaN() && !simGpa.isNaN())) Color(0xFF10B981) else if (simGpa < realGpa) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                Text(
                                    text = if (simGpa.isNaN()) "—" else String.format(Locale.getDefault(), "%.2f", simGpa),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = simGpaColor
                                )
                            }
                        }

                        if (simulatorExpanded) {
                            Spacer(modifier = Modifier.height(spacing.medium))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(spacing.small))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Mock Grades (${mockGrades.size})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (mockGrades.isNotEmpty()) {
                                        TextButton(onClick = { mockGrades.clear() }) {
                                            Text("Clear", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    TextButton(onClick = { showMockDialog = true }) {
                                        Text("Add Mock")
                                    }
                                }
                            }

                            if (mockGrades.isEmpty()) {
                                Text(
                                    "No mock grades added. Add mock grades to see how future scores affect your GPA.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = spacing.small)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    mockGrades.forEachIndexed { index, mock ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(mock.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text("${mock.subject} • Weight: ${mock.weight}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${String.format(Locale.getDefault(), "%.1f", mock.score)}/${String.format(Locale.getDefault(), "%.1f", mock.maxScore)} (${String.format(Locale.getDefault(), "%.1f%%", mock.percentage)})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(onClick = { mockGrades.removeAt(index) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (grades.size >= 2) {
                item {
                    GradeTrendChart(grades, spacing)
                }
            }

            // Real grades list
            if (grades.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No grades recorded yet.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))
                        Button(onClick = { showAddDialog = true }) {
                            Text("Add Your First Grade")
                        }
                    }
                }
            } else {
                gradesBySubject.forEach { (sub, subGrades) ->
                    val realAvg = realAverages[sub] ?: 0.0
                    val simAvg = simAverages[sub] ?: 0.0

                    val letterGrade = when {
                        simAvg >= 90.0 -> "A"
                        simAvg >= 80.0 -> "B"
                        simAvg >= 70.0 -> "C"
                        simAvg >= 60.0 -> "D"
                        else -> "F"
                    }

                    val lessonMatch = lessons.find { it.name.equals(sub, ignoreCase = true) }
                    val colorHex = lessonMatch?.colorHex ?: 0xFF6200EE.toInt()

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                                ) {
                                    Box(modifier = Modifier.fillMaxHeight().width(6.dp).background(Color(colorHex)))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(sub, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "Avg: ${String.format(Locale.getDefault(), "%.1f%%", realAvg)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (Math.abs(simAvg - realAvg) > 0.01) {
                                                    val changeColor = if (simAvg > realAvg) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                                                    val sign = if (simAvg > realAvg) "+" else ""
                                                    Text(
                                                        text = "Sim: ${String.format(Locale.getDefault(), "%.1f%%", simAvg)} ($sign${String.format(Locale.getDefault(), "%.1f%%", simAvg - realAvg)})",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = changeColor
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = letterGrade,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Black,
                                            color = Color(colorHex)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                                Column(modifier = Modifier.padding(horizontal = spacing.medium, vertical = spacing.small)) {
                                    subGrades.forEach { grade ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = spacing.small),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(grade.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(grade.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    if (grade.weight != 1.0) {
                                                        Text("Weight: ${grade.weight}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${String.format(Locale.getDefault(), "%.1f", grade.score)} / ${String.format(Locale.getDefault(), "%.1f", grade.maxScore)} (${String.format(Locale.getDefault(), "%.1f%%", grade.percentage)})",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                IconButton(onClick = { viewModel.deleteGrade(grade.id) }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Grade",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(spacing.medium).padding(bottom = 56.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Grade")
        }

        if (showAddDialog) {
            AddGradeDialog(
                lessons = lessons,
                onDismiss = { showAddDialog = false },
                onSave = { grade ->
                    viewModel.saveGrade(grade)
                    showAddDialog = false
                }
            )
        }

        if (showMockDialog) {
            AddGradeDialog(
                lessons = lessons,
                onDismiss = { showMockDialog = false },
                onSave = { grade ->
                    mockGrades.add(grade)
                    showMockDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGradeDialog(
    lessons: List<com.grinkware.timeplans.data.Lesson>,
    onDismiss: () -> Unit,
    onSave: (GradeEntry) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var scoreStr by remember { mutableStateOf("") }
    var maxScoreStr by remember { mutableStateOf("") }
    var weightStr by remember { mutableStateOf("1.0") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val distinctSubjects = remember(lessons) {
        lessons.filter { it.periodType == "CLASS" }.map { it.name }.distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Grade Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assignment/Exam Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (distinctSubjects.isNotEmpty()) {
                    Text("Timetable Subjects:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(distinctSubjects) { sub ->
                            AssistChip(
                                onClick = { subject = sub },
                                label = { Text(sub) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = scoreStr,
                        onValueChange = { scoreStr = it },
                        label = { Text("Score") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxScoreStr,
                        onValueChange = { maxScoreStr = it },
                        label = { Text("Max Score") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = { Text("Weight (default 1.0)") },
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage.value?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val score = scoreStr.toDoubleOrNull()
                    val maxScore = maxScoreStr.toDoubleOrNull()
                    val weight = weightStr.toDoubleOrNull() ?: 1.0

                    if (title.isBlank()) {
                        errorMessage.value = "Title cannot be empty"
                    } else if (subject.isBlank()) {
                        errorMessage.value = "Subject cannot be empty"
                    } else if (score == null || maxScore == null) {
                        errorMessage.value = "Score and Max Score must be valid numbers"
                    } else if (maxScore <= 0) {
                        errorMessage.value = "Max Score must be greater than 0"
                    } else {
                        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        onSave(
                            GradeEntry(
                                subject = subject,
                                title = title,
                                score = score,
                                maxScore = maxScore,
                                weight = weight,
                                date = currentDate
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTimerTab(viewModel: AppViewModel, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val studySessions = viewModel.studySessionsList.value
    val lessons = viewModel.lessons.value

    val distinctSubjects = remember(lessons) {
        lessons.filter { it.periodType == "CLASS" }.map { it.name }.distinct().sorted()
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todaySessions = remember(studySessions) {
        studySessions.filter { it.date == todayStr }
    }

    var selectedSubject by remember { mutableStateOf(distinctSubjects.firstOrNull() ?: "General Study") }
    var isRunning by remember { mutableStateOf(false) }
    var isFocusMode by remember { mutableStateOf(true) } // true = Focus, false = Break
    var totalSeconds by remember { mutableStateOf(25 * 60) }
    var secondsRemaining by remember { mutableStateOf(25 * 60) }
    var showSubjectDropdown by remember { mutableStateOf(false) }

    var customFocusMinutes by remember { mutableStateOf(25) }
    var customBreakMinutes by remember { mutableStateOf(5) }
    val showCustomPomodoroDialog = remember { mutableStateOf(false) }
    var isCustomPresetSelected by remember { mutableStateOf(false) }
    var selectedFocusDurationMinutes by remember { mutableStateOf(25) }

    val showReflectionDialog = remember { mutableStateOf(false) }
    var tempReflectionSubject by remember { mutableStateOf("") }
    var tempReflectionDurationMinutes by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()
    var selectedSound by remember { mutableStateOf("Off") }
    var soundVolume by remember { mutableStateOf(0.5f) }
    var showSoundDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, selectedSound) {
        if (isRunning) {
            FocusAudioSynthesizer.play(selectedSound, coroutineScope)
        } else {
            FocusAudioSynthesizer.stop()
        }
    }

    LaunchedEffect(soundVolume) {
        FocusAudioSynthesizer.setVolume(soundVolume)
    }

    DisposableEffect(Unit) {
        onDispose {
            FocusAudioSynthesizer.stop()
        }
    }

    // Tick the timer
    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            kotlinx.coroutines.delay(1000L)
            secondsRemaining--
        } else if (isRunning && secondsRemaining == 0) {
            // Timer completed!
            isRunning = false
            if (isFocusMode) {
                // Trigger reflection instead of saving immediately
                tempReflectionSubject = selectedSubject
                tempReflectionDurationMinutes = totalSeconds / 60
                showReflectionDialog.value = true

                // Switch to Break Mode
                isFocusMode = false
                totalSeconds = if (isCustomPresetSelected) customBreakMinutes * 60 else 5 * 60
                secondsRemaining = if (isCustomPresetSelected) customBreakMinutes * 60 else 5 * 60
            } else {
                // Switch to Focus Mode
                isFocusMode = true
                totalSeconds = if (isCustomPresetSelected) customFocusMinutes * 60 else selectedFocusDurationMinutes * 60
                secondsRemaining = if (isCustomPresetSelected) customFocusMinutes * 60 else selectedFocusDurationMinutes * 60
            }
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeText = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    val progress = if (totalSeconds > 0) secondsRemaining.toFloat() / totalSeconds else 0f

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.medium)
    ) {
        // Tag Subject Dropdown/Selector
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isFocusMode) "Tag Study Subject" else "Break Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocusMode) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isFocusMode) {
                    Box {
                        Button(
                            onClick = { showSubjectDropdown = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text(selectedSubject)
                        }
                        DropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            distinctSubjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub) },
                                    onClick = {
                                        selectedSubject = sub
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("General Study") },
                                onClick = {
                                    selectedSubject = "General Study"
                                    showSubjectDropdown = false
                                }
                            )
                        }
                    }
                } else {
                    Text("Take a well-deserved break!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Circular Timer Display
        item {
            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                val strokeColor = if (isFocusMode) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = strokeColor,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isFocusMode) "FOCUS SESSION" else "REST BREAK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFocusMode) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                    )
                }
            }
        }

        // Timer Preset Quick Controls
        if (!isRunning) {
            item {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val presets = listOf(15, 25, 45, 60)
                    presets.forEach { mins ->
                        val totalSecs = mins * 60
                        FilterChip(
                            selected = !isCustomPresetSelected && totalSeconds == totalSecs,
                            onClick = {
                                isCustomPresetSelected = false
                                selectedFocusDurationMinutes = mins
                                totalSeconds = totalSecs
                                secondsRemaining = totalSecs
                            },
                            label = { Text("${mins}m") }
                        )
                    }

                    FilterChip(
                        selected = isCustomPresetSelected,
                        onClick = {
                            showCustomPomodoroDialog.value = true
                        },
                        label = { Text(if (isCustomPresetSelected) "Custom: ${customFocusMinutes}m/${customBreakMinutes}m" else "Custom") },
                        enabled = !isRunning
                    )
                }
            }
        }

        // Play / Pause / Reset Control Buttons
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button
                Button(
                    onClick = { isRunning = !isRunning },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Pause" else "Start"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "Pause" else "Start")
                }

                // Reset Button
                IconButton(
                    onClick = {
                        isRunning = false
                        secondsRemaining = totalSeconds
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer"
                    )
                }
            }
        }

        // Soundscape Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        "Ambient Soundscape",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Synthesized real-time frequencies to aid deep focus.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Selected Sound", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Box {
                            Button(
                                onClick = { showSoundDropdown = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(selectedSound)
                            }
                            DropdownMenu(
                                expanded = showSoundDropdown,
                                onDismissRequest = { showSoundDropdown = false }
                            ) {
                                val sounds = listOf("Off", "White Noise", "Pink Noise", "Brown Noise", "Binaural Focus", "Binaural Deep Work")
                                sounds.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text(s) },
                                        onClick = {
                                            selectedSound = s
                                            showSoundDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (selectedSound != "Off") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Volume", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(60.dp))
                            Slider(
                                value = soundVolume,
                                onValueChange = { soundVolume = it },
                                valueRange = 0f..1f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        item {
            FocusMilestonesCard(studySessions, spacing)
        }

        item {
            Spacer(modifier = Modifier.height(spacing.medium))
            Text(
                "Today's Study Sessions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (todaySessions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                        Text("No sessions completed today yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(todaySessions) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Completed: ${session.durationMinutes} minutes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            if (session.rating > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⭐".repeat(session.rating),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            
                            if (session.reflection.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "\"${session.reflection}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.deleteStudySession(session.id) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Session",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReflectionDialog.value) {
        FocusSessionReflectionDialog(
            subject = tempReflectionSubject,
            durationMinutes = tempReflectionDurationMinutes,
            onDismiss = {
                showReflectionDialog.value = false
            },
            onSave = { rating, reflection ->
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                viewModel.saveStudySession(
                    StudySession(
                        subject = tempReflectionSubject,
                        durationMinutes = tempReflectionDurationMinutes,
                        date = currentDate,
                        rating = rating,
                        reflection = reflection
                    )
                )
                showReflectionDialog.value = false
            }
        )
    }

    if (showCustomPomodoroDialog.value) {
        var tempFocusMinutes by remember { mutableStateOf(customFocusMinutes) }
        var tempBreakMinutes by remember { mutableStateOf(customBreakMinutes) }

        AlertDialog(
            onDismissRequest = { showCustomPomodoroDialog.value = false },
            title = {
                Text(
                    text = "Custom Pomodoro Timer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Configure durations for your study and rest periods.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Focus duration slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Focus Duration",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${tempFocusMinutes} min",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = tempFocusMinutes.toFloat(),
                            onValueChange = { tempFocusMinutes = it.toInt() },
                            valueRange = 5f..120f,
                            steps = 114
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("5m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("120m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Break duration slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Break Duration",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${tempBreakMinutes} min",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                        Slider(
                            value = tempBreakMinutes.toFloat(),
                            onValueChange = { tempBreakMinutes = it.toInt() },
                            valueRange = 1f..30f,
                            steps = 28
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("30m", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customFocusMinutes = tempFocusMinutes
                        customBreakMinutes = tempBreakMinutes
                        isCustomPresetSelected = true
                        selectedFocusDurationMinutes = tempFocusMinutes
                        if (isFocusMode) {
                            totalSeconds = tempFocusMinutes * 60
                            secondsRemaining = tempFocusMinutes * 60
                        } else {
                            totalSeconds = tempBreakMinutes * 60
                            secondsRemaining = tempBreakMinutes * 60
                        }
                        showCustomPomodoroDialog.value = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPomodoroDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatMiniCounter(label: String, count: Int, color: Color) {
    Column {
        Text(
            text = count.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubjectAttendanceRow(stats: SubjectStats, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    val total = stats.present + stats.absent + stats.late
    val percentage = if (total > 0) {
        (stats.present + stats.late).toFloat() / total * 100
    } else {
        100f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(Color(stats.color))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(spacing.medium),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stats.subjectName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", percentage),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Black,
                        color = Color(stats.color)
                    )
                }

                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    color = Color(stats.color),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp)
                )

                Text(
                    text = "Present: ${stats.present} • Late: ${stats.late} • Absent: ${stats.absent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MonthlyBarChart(data: List<MonthData>) {
    val maxCount = data.maxOfOrNull { it.total } ?: 5
    val targetMax = if (maxCount > 0) maxCount else 5

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { month ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.85f)
                        .width(12.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        val remaining = targetMax - month.total
                        if (remaining > 0) {
                            Spacer(modifier = Modifier.weight(remaining.toFloat()))
                        }
                        if (month.absentCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.absentCount.toFloat())
                                    .background(Color(0xFFEF4444), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                        }
                        if (month.lateCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.lateCount.toFloat())
                                    .background(Color(0xFFF59E0B))
                            )
                        }
                        if (month.presentCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(month.presentCount.toFloat())
                                    .background(Color(0xFF10B981), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = month.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

data class MonthData(
    val name: String,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int
) {
    val total: Int get() = presentCount + absentCount + lateCount
}

fun getMonthlyBreakdown(records: List<AttendanceRecord>): List<MonthData> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val order = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val map = mutableMapOf<String, MonthData>()
    
    records.forEach { record ->
        try {
            val date = sdf.parse(record.date)
            if (date != null) {
                val mName = monthFormat.format(date)
                val existing = map[mName] ?: MonthData(mName, 0, 0, 0)
                val newObj = when (record.status) {
                    "IN" -> existing.copy(presentCount = existing.presentCount + 1)
                    "LATE" -> existing.copy(lateCount = existing.lateCount + 1)
                    "OFF" -> existing.copy(absentCount = existing.absentCount + 1)
                    "SICK" -> existing.copy(absentCount = existing.absentCount + 1)
                    else -> existing
                }
                map[mName] = newObj
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    val result = mutableListOf<MonthData>()
    order.forEach { m ->
        val item = map[m]
        if (item != null) {
            result.add(item)
        }
    }
    
    if (result.isEmpty()) {
        result.add(MonthData("May", 0, 0, 0))
        result.add(MonthData("Jun", 0, 0, 0))
    }
    
    return result
}

data class TermData(
    val name: String,
    val totalDays: Int,
    val percentage: Float
)

fun getTermBreakdown(records: List<AttendanceRecord>): List<TermData> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    var autumnIn = 0
    var autumnTotal = 0
    var springIn = 0
    var springTotal = 0
    var summerIn = 0
    var summerTotal = 0

    records.forEach { record ->
        try {
            val date = sdf.parse(record.date)
            if (date != null) {
                val cal = Calendar.getInstance().apply { time = date }
                val month = cal.get(Calendar.MONTH)
                
                when (month) {
                    Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER, Calendar.DECEMBER -> {
                        if (record.status != "HOLIDAY") {
                            autumnTotal++
                            if (record.status == "IN" || record.status == "LATE") autumnIn++
                        }
                    }
                    Calendar.JANUARY, Calendar.FEBRUARY, Calendar.MARCH, Calendar.APRIL -> {
                        if (record.status != "HOLIDAY") {
                            springTotal++
                            if (record.status == "IN" || record.status == "LATE") springIn++
                        }
                    }
                    Calendar.MAY, Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> {
                        if (record.status != "HOLIDAY") {
                            summerTotal++
                            if (record.status == "IN" || record.status == "LATE") summerIn++
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    return listOf(
        TermData("Autumn Term (Sep - Dec)", autumnTotal, if (autumnTotal > 0) (autumnIn.toFloat() / autumnTotal * 100) else 100f),
        TermData("Spring Term (Jan - Apr)", springTotal, if (springTotal > 0) (springIn.toFloat() / springTotal * 100) else 100f),
        TermData("Summer Term (May - Aug)", summerTotal, if (summerTotal > 0) (summerIn.toFloat() / summerTotal * 100) else 100f)
    )
}

// Spaced Repetition Flashcards & Weekly revision goals helper functions

fun isDateInCurrentWeek(dateStr: String): Boolean {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val date = sdf.parse(dateStr) ?: return false
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)
        
        cal.time = date
        val dateWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val dateYear = cal.get(Calendar.YEAR)
        
        currentWeek == dateWeek && currentYear == dateYear
    } catch (_: Exception) {
        false
    }
}

@Composable
fun EditTargetsDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val lessons = viewModel.lessons.value
    val distinctSubjects = remember(lessons) {
        lessons.filter { it.periodType == "CLASS" }.map { it.name }.distinct().sorted()
    }
    val existingTargets = viewModel.studyTargetsList.value
    
    val targetMap = remember {
        mutableStateMapOf<String, String>().apply {
            distinctSubjects.forEach { sub ->
                val targetVal = existingTargets.find { it.subject.equals(sub, ignoreCase = true) }?.targetMinutes ?: 0
                put(sub, if (targetVal > 0) targetVal.toString() else "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Weekly Study Goals") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set target study minutes for each subject per week. Enter 0 or leave blank to disable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)
                ) {
                    items(distinctSubjects) { sub ->
                        val lessonMatch = lessons.find { it.name.equals(sub, ignoreCase = true) }
                        val colorHex = lessonMatch?.colorHex ?: 0xFF6200EE.toInt()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.size(12.dp).background(Color(colorHex), shape = CircleShape))
                            Text(sub, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            OutlinedTextField(
                                value = targetMap[sub] ?: "",
                                onValueChange = { newValue ->
                                    if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                                        targetMap[sub] = newValue
                                    }
                                },
                                label = { Text("Mins") },
                                modifier = Modifier.width(90.dp),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    distinctSubjects.forEach { sub ->
                        val enteredVal = targetMap[sub]?.toIntOrNull() ?: 0
                        val existing = existingTargets.find { it.subject.equals(sub, ignoreCase = true) }
                        if (enteredVal > 0) {
                            viewModel.saveStudyTarget(
                                StudyTarget(
                                    id = existing?.id ?: 0,
                                    subject = sub,
                                    targetMinutes = enteredVal
                                )
                            )
                        } else {
                            viewModel.deleteStudyTarget(sub)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun isFlashcardDue(card: Flashcard): Boolean {
    if (card.lastReviewed.isEmpty()) return true
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return try {
        val lastDate = sdf.parse(card.lastReviewed) ?: return true
        val cal = Calendar.getInstance()
        val today = cal.time
        
        val diffMs = today.time - lastDate.time
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        
        val requiredInterval = when (card.box) {
            1 -> 0
            2 -> 2
            3 -> 4
            4 -> 7
            5 -> 14
            else -> 0
        }
        diffDays >= requiredInterval
    } catch (_: Exception) {
        true
    }
}

@Composable
fun FlashcardsTab(
    viewModel: AppViewModel,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val flashcards = viewModel.flashcardsList.value
    var currentMode by remember { mutableStateOf(0) } // 0 = Review, 1 = Manage
    val showAddDialog = remember { mutableStateOf(false) }

    val dueFlashcards = remember(flashcards) {
        flashcards.filter { isFlashcardDue(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.medium)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = currentMode == 0,
                    onClick = { currentMode = 0 },
                    label = { Text("Review (${dueFlashcards.size})") }
                )
                FilterChip(
                    selected = currentMode == 1,
                    onClick = { currentMode = 1 },
                    label = { Text("Manage (${flashcards.size})") }
                )
            }
        }

        item {
            LeitnerAdvisorStreakWidget(flashcards, spacing)
        }

        item {
            LeitnerBoxSummary(flashcards, spacing)
        }

        if (currentMode == 0) {
            if (dueFlashcards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.medium),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA))
                    ) {
                        Column(
                            modifier = Modifier.padding(spacing.large),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "All Caught Up! 🎉",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF137333)
                            )
                            Text(
                                "No flashcards are due for review right now. Add new cards or check back later!",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF137333).copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                val currentCard = dueFlashcards.first()
                item {
                    Text(
                        "Reviewing ${dueFlashcards.size} due cards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                item {
                    FlashcardWidget(
                        flashcard = currentCard,
                        onCorrect = {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            val nextBox = (currentCard.box + 1).coerceAtMost(5)
                            viewModel.saveFlashcard(currentCard.copy(box = nextBox, lastReviewed = today))
                        },
                        onIncorrect = {
                            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            viewModel.saveFlashcard(currentCard.copy(box = 1, lastReviewed = today))
                        },
                        spacing = spacing
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "My Flashcards",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddDialog.value = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Flashcard")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add New")
                    }
                }
            }

            if (flashcards.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(spacing.medium), contentAlignment = Alignment.Center) {
                            Text(
                                "No flashcards created yet. Click 'Add New' to create one!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val groupedCards = flashcards.groupBy { it.subject }
                groupedCards.forEach { (subject, cards) ->
                    item {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = spacing.small)
                        )
                    }

                    items(cards) { card ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(spacing.medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.front,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = card.back,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text("Box ${card.box}") }
                                        )
                                        if (card.lastReviewed.isNotEmpty()) {
                                            Text(
                                                text = "Reviewed: ${card.lastReviewed}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteFlashcard(card.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Flashcard",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog.value) {
        AddFlashcardDialog(
            lessons = viewModel.lessons.value,
            onDismiss = { showAddDialog.value = false },
            onSave = { card ->
                viewModel.saveFlashcard(card)
                showAddDialog.value = false
            }
        )
    }
}

@Composable
fun LeitnerBoxSummary(
    flashcards: List<Flashcard>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            val totalCards = flashcards.size
            val averageMastery = if (totalCards > 0) {
                (flashcards.sumOf { it.box }.toFloat() / (5 * totalCards)) * 100
            } else {
                0f
            }

            Text(
                "Leitner Box Distribution",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (1..5).forEach { boxNum ->
                    val count = flashcards.count { it.box == boxNum }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "B$boxNum",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = when (boxNum) {
                                        1 -> MaterialTheme.colorScheme.primaryContainer
                                        2 -> MaterialTheme.colorScheme.secondaryContainer
                                        3 -> MaterialTheme.colorScheme.tertiaryContainer
                                        4 -> Color(0xFFE6F4EA)
                                        else -> Color(0xFFFFF4E5)
                                    },
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = when (boxNum) {
                                    1 -> MaterialTheme.colorScheme.onPrimaryContainer
                                    2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    3 -> MaterialTheme.colorScheme.onTertiaryContainer
                                    4 -> Color(0xFF137333)
                                    else -> Color(0xFFB06000)
                                }
                            )
                        }
                    }
                }
            }

            if (totalCards > 0) {
                Spacer(modifier = Modifier.height(spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deck Mastery: ${averageMastery.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${flashcards.count { it.box == 5 }} / $totalCards Mastered (Box 5)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val boxColors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    Color(0xFFC2E7C7), // box 4 light green
                    Color(0xFFFFD8A8)  // box 5 light gold
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx())
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(0f, 0f, w, h),
                                cornerRadius = cornerRadius
                            )
                        )
                    }
                    
                    clipPath(path) {
                        var currentX = 0f
                        (1..5).forEach { boxNum ->
                            val count = flashcards.count { it.box == boxNum }
                            val segmentWidth = (count.toFloat() / totalCards) * w
                            if (segmentWidth > 0) {
                                drawRect(
                                    color = boxColors[boxNum - 1],
                                    topLeft = Offset(currentX, 0f),
                                    size = Size(segmentWidth, h)
                                )
                                currentX += segmentWidth
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardWidget(
    flashcard: Flashcard,
    onCorrect: () -> Unit,
    onIncorrect: () -> Unit,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    var rotated by remember(flashcard.id) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "cardFlip"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clickable { rotated = !rotated }
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8 * density
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (rotation <= 90f) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = flashcard.subject.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = flashcard.front,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = spacing.small)
                        )
                        Text(
                            text = "Tap to reveal answer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .padding(spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ANSWER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                        Text(
                            text = flashcard.back,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = spacing.small)
                        )
                        Text(
                            text = "Tap to show question",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        if (rotated) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Button(
                    onClick = {
                        rotated = false
                        onIncorrect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Incorrect")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Forgot 👎")
                }

                Button(
                    onClick = {
                        rotated = false
                        onCorrect()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE6F4EA),
                        contentColor = Color(0xFF137333)
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Correct")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Got it! 👍")
                }
            }
        }
    }
}

@Composable
fun AddFlashcardDialog(
    lessons: List<com.grinkware.timeplans.data.Lesson>,
    onDismiss: () -> Unit,
    onSave: (Flashcard) -> Unit
) {
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val distinctSubjects = remember(lessons) {
        lessons.filter { it.periodType == "CLASS" }.map { it.name }.distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Flashcard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (distinctSubjects.isNotEmpty()) {
                    Text("Timetable Subjects:", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(distinctSubjects) { sub ->
                            AssistChip(
                                onClick = { subject = sub },
                                label = { Text(sub) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = front,
                    onValueChange = { front = it },
                    label = { Text("Front (Question / Term)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = back,
                    onValueChange = { back = it },
                    label = { Text("Back (Answer / Definition)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                errorMessage.value?.let { msg ->
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (subject.isBlank()) {
                        errorMessage.value = "Subject cannot be empty"
                    } else if (front.isBlank()) {
                        errorMessage.value = "Front cannot be empty"
                    } else if (back.isBlank()) {
                        errorMessage.value = "Back cannot be empty"
                    } else {
                        onSave(
                            Flashcard(
                                subject = subject,
                                front = front,
                                back = back,
                                box = 1,
                                lastReviewed = ""
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

object FocusAudioSynthesizer {
    private var audioTrack: android.media.AudioTrack? = null
    private var activeJob: kotlinx.coroutines.Job? = null
    private val random = Random()

    @Volatile
    private var isPlaying = false

    @Volatile
    private var volume = 0.5f

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        audioTrack?.let {
            try {
                it.setVolume(volume)
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    fun stop() {
        isPlaying = false
        activeJob?.cancel()
        activeJob = null
        try {
            audioTrack?.apply {
                if (state == android.media.AudioTrack.STATE_INITIALIZED) {
                    stop()
                    release()
                }
            }
        } catch (_: Exception) {
            // ignore
        }
        audioTrack = null
    }

    fun play(type: String, scope: kotlinx.coroutines.CoroutineScope) {
        stop()
        if (type == "Off" || type.isEmpty()) return

        isPlaying = true
        activeJob = scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val sampleRate = 44100
            val bufferSize = android.media.AudioTrack.getMinBufferSize(
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_STEREO,
                android.media.AudioFormat.ENCODING_PCM_16BIT
            )
            
            val track = android.media.AudioTrack.Builder()
                .setAudioAttributes(
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    android.media.AudioFormat.Builder()
                        .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(android.media.AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(android.media.AudioTrack.MODE_STREAM)
                .build()
            audioTrack = track
            setVolume(volume)

            try {
                track.play()
            } catch (_: Exception) {
                isPlaying = false
                return@launch
            }

            val numSamples = bufferSize / 2
            val samples = ShortArray(numSamples)

            var phaseLeft = 0.0
            var phaseRight = 0.0
            
            val freqLeft = when (type) {
                "Binaural Focus" -> 200.0
                "Binaural Deep Work" -> 150.0
                else -> 0.0
            }
            val freqRight = when (type) {
                "Binaural Focus" -> 210.0 // 10Hz alpha (focus)
                "Binaural Deep Work" -> 156.0 // 6Hz theta (deep work)
                else -> 0.0
            }

            // Pink noise filtering state
            var b0 = 0.0
            var b1 = 0.0
            var b2 = 0.0
            var b3 = 0.0
            var b4 = 0.0
            var b5 = 0.0
            var b6 = 0.0

            // Brown noise state
            var brownAccumulator = 0.0

            while (isPlaying && track.state == android.media.AudioTrack.STATE_INITIALIZED) {
                var index = 0
                while (index < numSamples && isPlaying) {
                    when (type) {
                        "White Noise" -> {
                            val noise = (random.nextGaussian() * 0.15 * Short.MAX_VALUE).toInt()
                            val clamped = noise.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            samples[index] = clamped
                            if (index + 1 < numSamples) {
                                samples[index + 1] = clamped
                            }
                        }
                        "Pink Noise" -> {
                            val white = random.nextDouble() * 2.0 - 1.0
                            b0 = 0.99886 * b0 + white * 0.0555179
                            b1 = 0.99332 * b1 + white * 0.0750759
                            b2 = 0.96900 * b2 + white * 0.1538520
                            b3 = 0.86650 * b3 + white * 0.3104856
                            b4 = 0.55000 * b4 + white * 0.5329522
                            b5 = -0.7616 * b5 - white * 0.0168980
                            val pink = b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362
                            b6 = white * 0.115926
                            val value = (pink * 0.03 * Short.MAX_VALUE).toInt()
                            val clamped = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            samples[index] = clamped
                            if (index + 1 < numSamples) {
                                samples[index + 1] = clamped
                            }
                        }
                        "Brown Noise" -> {
                            val white = random.nextDouble() * 2.0 - 1.0
                            brownAccumulator = (brownAccumulator + (0.02 * white)) / 1.02
                            val value = (brownAccumulator * 8.0 * 0.05 * Short.MAX_VALUE).toInt()
                            val clamped = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            samples[index] = clamped
                            if (index + 1 < numSamples) {
                                samples[index + 1] = clamped
                            }
                        }
                        "Binaural Focus", "Binaural Deep Work" -> {
                            val sampleLeft = Math.sin(phaseLeft) * 0.3 * Short.MAX_VALUE
                            val sampleRight = Math.sin(phaseRight) * 0.3 * Short.MAX_VALUE

                            samples[index] = sampleLeft.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            if (index + 1 < numSamples) {
                                samples[index + 1] = sampleRight.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                            }

                            phaseLeft += 2.0 * Math.PI * freqLeft / sampleRate
                            phaseRight += 2.0 * Math.PI * freqRight / sampleRate

                            if (phaseLeft > 2.0 * Math.PI) phaseLeft -= 2.0 * Math.PI
                            if (phaseRight > 2.0 * Math.PI) phaseRight -= 2.0 * Math.PI
                        }
                    }
                    index += 2
                }
                if (isPlaying) {
                    try {
                        track.write(samples, 0, numSamples)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
        }
    }
}

@Composable
fun StudyActivityChart(
    daysData: List<Pair<String, Int>>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val maxMinutes = remember(daysData) {
        daysData.map { it.second }.maxOrNull()?.coerceAtLeast(1) ?: 60
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(
                "Study Activity (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val barColor = MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    val numBars = daysData.size
                    val spacingBetweenBars = 16.dp.toPx()
                    val totalSpacing = spacingBetweenBars * (numBars - 1)
                    val barWidth = (canvasWidth - totalSpacing) / numBars
                    
                    daysData.forEachIndexed { index, pair ->
                        val minutes = pair.second
                        val ratio = minutes.toFloat() / maxMinutes
                        val barHeight = ratio * (canvasHeight - 30.dp.toPx()) // save space for labels
                        val x = index * (barWidth + spacingBetweenBars)
                        
                        // Draw background track
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(x, 0f),
                            size = Size(barWidth, canvasHeight - 24.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                        
                        // Draw actual minutes bar
                        if (barHeight > 0) {
                            val y = canvasHeight - 24.dp.toPx() - barHeight
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                            )
                        }
                    }
                }
                
                // Labels layer
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysData.forEach { pair ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (pair.second > 0) "${pair.second}m" else "0",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pair.second > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = pair.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun SubjectDistributionDonutChart(
    distribution: List<Triple<String, Int, Color>>,
    totalMinutes: Int,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    if (distribution.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(
                "Study Share by Subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.medium))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidth = 16.dp.toPx()
                        
                        distribution.forEach { triple ->
                            val mins = triple.second
                            val color = triple.third
                            val sweepAngle = (mins.toFloat() / totalMinutes) * 360f
                            
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (totalMinutes >= 60) "${totalMinutes / 60}h" else "${totalMinutes}m",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    distribution.take(4).forEach { triple ->
                        val sub = triple.first
                        val mins = triple.second
                        val color = triple.third
                        val pct = (mins.toFloat() / totalMinutes * 100).toInt()
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Text(
                                text = "$sub ($pct%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    if (distribution.size > 4) {
                        Text(
                            text = "+ ${distribution.size - 4} more subjects",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ---------------- Premium Productivity Features ----------------

@Composable
fun GradeTrendChart(
    grades: List<GradeEntry>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val sortedGrades = remember(grades) {
        grades.sortedBy { it.date }
    }
    if (sortedGrades.size < 2) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            Text(
                "Grade Percentage Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(spacing.medium))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val lineColor = MaterialTheme.colorScheme.primary
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

                Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp)) {
                    val w = size.width
                    val h = size.height
                    val pointsCount = sortedGrades.size
                    val stepX = w / (pointsCount - 1)

                    // Draw grid lines (50%, 75%, 100%)
                    val gridPercentages = listOf(0.5f, 0.75f, 1f)
                    gridPercentages.forEach { pct ->
                        val y = h - (pct * h)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Build path
                    val path = androidx.compose.ui.graphics.Path()
                    val fillPath = androidx.compose.ui.graphics.Path()

                    sortedGrades.forEachIndexed { idx, entry ->
                        val pctValue = (entry.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                        val x = idx * stepX
                        val y = h - (pctValue * h)

                        if (idx == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, h)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }

                        if (idx == pointsCount - 1) {
                            fillPath.lineTo(x, h)
                            fillPath.close()
                        }
                    }

                    // Draw filled area under curve
                    drawPath(path = fillPath, color = fillColor)

                    // Draw trend line
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw points
                    sortedGrades.forEachIndexed { idx, entry ->
                        val pctValue = (entry.percentage / 100.0).toFloat().coerceIn(0f, 1f)
                        val x = idx * stepX
                        val y = h - (pctValue * h)

                        drawCircle(
                            color = lineColor,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(sortedGrades.first().date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (sortedGrades.size > 2) {
                    Text(sortedGrades[sortedGrades.size / 2].date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(sortedGrades.last().date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FocusMilestonesCard(
    sessions: List<StudySession>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todaySessions = remember(sessions) { sessions.filter { it.date == todayStr } }
    val totalTodayMins = remember(todaySessions) { todaySessions.sumOf { it.durationMinutes } }
    
    val last7DaysCount = remember(sessions) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dates = (0..6).map { offset ->
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -offset)
            sdf.format(c.time)
        }
        sessions.filter { it.date in dates }.map { it.date }.distinct().size
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(spacing.medium), verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                "Study Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Complete study milestones to build your revision streak.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))

            // Milestones list
            val milestones = listOf(
                Triple("First Step", "Study at least one session today", totalTodayMins > 0),
                Triple("Deep Focus", "Log a single session of 45m+", todaySessions.any { it.durationMinutes >= 45 }),
                Triple("Marathoner", "Revision total of 90m+ today", totalTodayMins >= 90),
                Triple("Habit Builder", "Study on 3+ days this week ($last7DaysCount/3)", last7DaysCount >= 3)
            )

            milestones.forEach { (title, desc, achieved) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (achieved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (achieved) Icons.Default.Check else Icons.Default.Info,
                            contentDescription = if (achieved) "Achieved" else "Pending",
                            tint = if (achieved) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (achieved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

fun calculateReviewStreak(flashcards: List<Flashcard>): Int {
    val dates = flashcards.map { it.lastReviewed }.filter { it.isNotEmpty() }.distinct().sorted()
    if (dates.isEmpty()) return 0

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = Calendar.getInstance()
    today.set(Calendar.HOUR_OF_DAY, 0)
    today.set(Calendar.MINUTE, 0)
    today.set(Calendar.SECOND, 0)
    today.set(Calendar.MILLISECOND, 0)

    val todayTime = today.timeInMillis
    val oneDayMs = 24 * 60 * 60 * 1000L

    var streak = 0
    var expectedTime = todayTime

    val todayStr = sdf.format(today.time)
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val yesterdayStr = sdf.format(yesterday.time)

    if (todayStr !in dates && yesterdayStr !in dates) {
        return 0
    }

    if (yesterdayStr in dates && todayStr !in dates) {
        expectedTime = yesterday.timeInMillis
    }

    while (true) {
        val dateToCheckStr = sdf.format(Date(expectedTime))
        if (dateToCheckStr in dates) {
            streak++
            expectedTime -= oneDayMs
        } else {
            break
        }
    }
    return streak
}

@Composable
fun LeitnerAdvisorStreakWidget(
    flashcards: List<Flashcard>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing
) {
    val streak = remember(flashcards) { calculateReviewStreak(flashcards) }
    val total = flashcards.size

    val advice = remember(flashcards) {
        val box1 = flashcards.count { it.box == 1 }
        val box5 = flashcards.count { it.box == 5 }
        
        when {
            total == 0 -> "Create some flashcards to start learning with active recall!"
            box1.toFloat() / total > 0.4f -> "Box 1 has over 40% of your cards. Review them today to move them to Box 2."
            box5.toFloat() / total > 0.5f -> "Superb! More than half of your cards are fully mastered in Box 5."
            else -> "Mix up your review schedule to target boxes that feel difficult."
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color(0xFFFFECE5), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔥", fontSize = 18.sp)
                    Text("$streak d", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Review Streak: $streak Day" + (if (streak == 1) "" else "s"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = advice,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSessionReflectionDialog(
    subject: String,
    durationMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (rating: Int, reflection: String) -> Unit
) {
    var rating by remember { mutableStateOf(5) }
    var reflectionText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Focus Session Reflection", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Nice job! You completed a $durationMinutes-minute focus session for $subject. Take a moment to reflect on your focus level.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Focus Rating",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        (1..5).forEach { index ->
                            val isSelected = index <= rating
                            Text(
                                text = if (isSelected) "★" else "☆",
                                fontSize = 32.sp,
                                color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clickable { rating = index }
                                    .padding(horizontal = 6.dp)
                            )
                        }
                    }
                    Text(
                        text = when (rating) {
                            1 -> "Very Distracted"
                            2 -> "Somewhat Distracted"
                            3 -> "Moderate Focus"
                            4 -> "Good Focus"
                            5 -> "Deep Focus"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it },
                    label = { Text("What went well or distracted you?") },
                    placeholder = { Text("Write notes about your focus...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(rating, reflectionText) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onSave(0, "") }
            ) {
                Text("Skip")
            }
        }
    )
}



