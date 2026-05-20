package com.grinkware.timeplans.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.DashboardState
import com.grinkware.timeplans.ui.TimelineItem
import com.grinkware.timeplans.ui.theme.LocalSpacing

@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val state by viewModel.dashboardState
    val spacing = LocalSpacing.current
    val activeYear = viewModel.activeTimetable.value
    val endOfYearCountdown = viewModel.getCountdownToEndOfYear()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.small)
    ) {
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

        // Today's Status Hero Card
        item {
            TodayHeroCard(state = state, spacing = spacing)
        }

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
                        progress = state.progress,
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
                    Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
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
    val startTimeStr = String.format("%02d:%02d", startMin / 60, startMin % 60)
    val endTimeStr = String.format("%02d:%02d", endMin / 60, endMin % 60)

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
