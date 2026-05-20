package com.grinkware.timeplans.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import com.grinkware.timeplans.ui.theme.SubjectColors

@Composable
fun TimetableScreen(viewModel: AppViewModel) {
    val activeYear = viewModel.activeTimetable.value
    val allLessons = viewModel.lessons.value
    val spacing = LocalSpacing.current

    // Dialog flags
    val showAddEditDialog = remember { mutableStateOf(false) }
    val selectedLessonForEdit = remember { mutableStateOf<Lesson?>(null) }
    val showActionDialog = remember { mutableStateOf(false) }
    val selectedLessonForActions = remember { mutableStateOf<Lesson?>(null) }
    val showTemporaryOverrideDialog = remember { mutableStateOf(false) }

    // Tab categories
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDayIndex by viewModel.selectedDay

    // Week A / Week B Selection
    var manualWeek by viewModel.manualWeekOverride

    // Active double week mode
    val hasDoubleWeek = activeYear?.hasTwoWeeks == true

    Scaffold(
        floatingActionButton = {
            if (activeYear != null) {
                FloatingActionButton(
                    onClick = {
                        selectedLessonForEdit.value = null
                        showAddEditDialog.value = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Class")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            // Day selector tabs
            ScrollableTabRow(
                selectedTabIndex = selectedDayIndex - 1,
                containerColor = MaterialTheme.colorScheme.background,
                edgePadding = spacing.medium
            ) {
                days.forEachIndexed { index, name ->
                    Tab(
                        selected = selectedDayIndex == index + 1,
                        onClick = { selectedDayIndex = index + 1 },
                        text = { Text(name, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Week A / Week B buttons if double week is enabled
            if (hasDoubleWeek) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Week: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(spacing.small))

                    val activeWeekLabel = viewModel.activeWeek.value
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp)
                    ) {
                        listOf("A", "B").forEach { w ->
                            val isSelected = (manualWeek == w) || (manualWeek == null && activeWeekLabel == w)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        manualWeek = w
                                        viewModel.loadAllData() // reload calculations
                                    }
                                    .padding(horizontal = spacing.medium, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Week $w",
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Filter lessons for display on selected day
            val dayLessonsFiltered = allLessons.filter { it.dayOfWeek == selectedDayIndex }
            val weekLessonsFiltered = if (hasDoubleWeek) {
                val currentW = manualWeek ?: viewModel.activeWeek.value
                dayLessonsFiltered.filter { it.weekType == "BOTH" || it.weekType == currentW }
            } else {
                dayLessonsFiltered
            }

            if (activeYear == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Create a school year in Settings first!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (weekLessonsFiltered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No classes",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Text(
                            text = "No classes scheduled for this day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                    contentPadding = PaddingValues(bottom = 80.dp, top = spacing.small)
                ) {
                    items(weekLessonsFiltered) { lesson ->
                        LessonTimetableCard(
                            lesson = lesson,
                            spacing = spacing,
                            onClick = {
                                selectedLessonForActions.value = lesson
                                showActionDialog.value = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Lesson Add / Edit Dialog
    if (showAddEditDialog.value) {
        LessonAddEditDialog(
            timetableId = activeYear?.id ?: 0L,
            lesson = selectedLessonForEdit.value,
            hasDoubleWeek = hasDoubleWeek,
            selectedDay = selectedDayIndex,
            onDismiss = { showAddEditDialog.value = false },
            onSave = { updatedLesson ->
                if (updatedLesson.id == 0L) {
                    viewModel.addLesson(updatedLesson)
                } else {
                    viewModel.editLesson(updatedLesson)
                }
                showAddEditDialog.value = false
            }
        )
    }

    // Action Selection Dialog (Edit, Delete, Override)
    if (showActionDialog.value && selectedLessonForActions.value != null) {
        val currentLesson = selectedLessonForActions.value!!
        AlertDialog(
            onDismissRequest = { showActionDialog.value = false },
            title = { Text(currentLesson.name) },
            text = { Text("Choose an action for this class.") },
            confirmButton = {
                TextButton(onClick = {
                    showActionDialog.value = false
                    selectedLessonForEdit.value = currentLesson
                    showAddEditDialog.value = true
                }) {
                    Text("Edit Properties")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showActionDialog.value = false
                        viewModel.deleteLesson(currentLesson.id)
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = {
                        showActionDialog.value = false
                        showTemporaryOverrideDialog.value = true
                    }) {
                        Text("Temporary Edit")
                    }
                }
            }
        )
    }

    // Temporary Override Dialog
    if (showTemporaryOverrideDialog.value && selectedLessonForActions.value != null) {
        val currentLesson = selectedLessonForActions.value!!
        val todayStr = viewModel.getTodayDateString()
        val override = viewModel.overrides.value.find { it.lessonId == currentLesson.id }

        var isCancelled by remember { mutableStateOf(override?.isCancelled == true) }
        var newRoom by remember { mutableStateOf(override?.newRoom ?: "") }
        var newTeacher by remember { mutableStateOf(override?.newTeacher ?: "") }

        AlertDialog(
            onDismissRequest = { showTemporaryOverrideDialog.value = false },
            title = { Text("Today's Overrides") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = "Modify details for today (${todayStr}) only. These reset tomorrow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cancel Class Today")
                        Switch(
                            checked = isCancelled,
                            onCheckedChange = { isCancelled = it }
                        )
                    }

                    if (!isCancelled) {
                        OutlinedTextField(
                            value = newRoom,
                            onValueChange = { newRoom = it },
                            label = { Text("Temporary Room") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newTeacher,
                            onValueChange = { newTeacher = it },
                            label = { Text("Temporary Teacher") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isCancelled) {
                        viewModel.cancelLessonForToday(currentLesson.id)
                    } else {
                        viewModel.changeRoomOrTeacherForToday(currentLesson.id, newRoom, newTeacher)
                    }
                    showTemporaryOverrideDialog.value = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Row {
                    if (override != null) {
                        TextButton(onClick = {
                            viewModel.clearOverridesForLessonToday(currentLesson.id)
                            showTemporaryOverrideDialog.value = false
                        }) {
                            Text("Reset Today", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showTemporaryOverrideDialog.value = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun LessonTimetableCard(
    lesson: Lesson,
    spacing: com.grinkware.timeplans.ui.theme.Spacing,
    onClick: () -> Unit
) {
    val subjectColor = Color(lesson.colorHex)
    val isAcademic = lesson.periodType == "CLASS" || lesson.periodType == "ASSEMBLY"
    val displayName = when (lesson.periodType) {
        "BREAK" -> if (lesson.name.equals("Break", ignoreCase = true)) "☕ Break" else "☕ ${lesson.name}"
        "LUNCH" -> if (lesson.name.equals("Lunch", ignoreCase = true)) "🍴 Lunch" else "🍴 ${lesson.name}"
        "ASSEMBLY" -> if (lesson.name.equals("Assembly", ignoreCase = true)) "📢 Assembly" else "📢 ${lesson.name}"
        else -> lesson.name
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(subjectColor)
            )

            Column(
                modifier = Modifier
                    .padding(spacing.medium)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${lesson.startTimeString} - ${lesson.endTimeString}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (lesson.notes.isNotEmpty()) {
                    Text(
                        text = "Notes: ${lesson.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                if (lesson.periodType == "CLASS" && lesson.homeworkLink.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Homework",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Homework: ${lesson.homeworkLink}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LessonAddEditDialog(
    timetableId: Long,
    lesson: Lesson?,
    hasDoubleWeek: Boolean,
    selectedDay: Int,
    onDismiss: () -> Unit,
    onSave: (Lesson) -> Unit
) {
    val spacing = LocalSpacing.current
    var name by remember { mutableStateOf(lesson?.name ?: "") }
    var teacher by remember { mutableStateOf(lesson?.teacher ?: "") }
    var room by remember { mutableStateOf(lesson?.room ?: "") }
    var dayOfWeek by remember { mutableStateOf(lesson?.dayOfWeek ?: selectedDay) }
    var startStr by remember { mutableStateOf(lesson?.startTimeString ?: "09:00") }
    var endStr by remember { mutableStateOf(lesson?.endTimeString ?: "10:00") }
    var notes by remember { mutableStateOf(lesson?.notes ?: "") }
    var homeworkLink by remember { mutableStateOf(lesson?.homeworkLink ?: "") }
    var selectedColor by remember { mutableStateOf(lesson?.colorHex ?: SubjectColors.first().hashCode()) }
    var weekType by remember { mutableStateOf(lesson?.weekType ?: "BOTH") }
    var periodType by remember { mutableStateOf(lesson?.periodType ?: "CLASS") }

    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (lesson == null) "Add Period" else "Edit Period") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.small),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    if (error.isNotEmpty()) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    Text("Period Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        listOf(
                            "CLASS" to "Class",
                            "BREAK" to "Break",
                            "LUNCH" to "Lunch",
                            "ASSEMBLY" to "Assembly"
                        ).forEach { (typeKey, label) ->
                            FilterChip(
                                selected = periodType == typeKey,
                                onClick = { 
                                    periodType = typeKey
                                    if (name.trim().isEmpty() || name == "Break" || name == "Lunch" || name == "Assembly") {
                                        name = when (typeKey) {
                                            "BREAK" -> "Break"
                                            "LUNCH" -> "Lunch"
                                            "ASSEMBLY" -> "Assembly"
                                            else -> ""
                                        }
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (periodType == "CLASS") "Lesson Name*" else "Period Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (periodType == "CLASS" || periodType == "ASSEMBLY") {
                    item {
                        OutlinedTextField(
                            value = teacher,
                            onValueChange = { teacher = it },
                            label = { Text("Teacher Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = room,
                            onValueChange = { room = it },
                            label = { Text("Room / Class Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                        OutlinedTextField(
                            value = startStr,
                            onValueChange = { startStr = it },
                            label = { Text("Start (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endStr,
                            onValueChange = { endStr = it },
                            label = { Text("End (HH:MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Select Subject Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubjectColors.take(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color.hashCode() }
                                    .border(
                                        width = if (selectedColor == color.hashCode()) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SubjectColors.drop(5).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color.hashCode() }
                                    .border(
                                        width = if (selectedColor == color.hashCode()) 2.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                if (hasDoubleWeek) {
                    item {
                        Text("Double Week Type", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                            listOf("BOTH", "A", "B").forEach { opt ->
                                FilterChip(
                                    selected = weekType == opt,
                                    onClick = { weekType = opt },
                                    label = { Text(opt) }
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (periodType == "CLASS") {
                    item {
                        OutlinedTextField(
                            value = homeworkLink,
                            onValueChange = { homeworkLink = it },
                            label = { Text("Homework Name / Details") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().isEmpty()) {
                    error = "Name is required"
                    return@TextButton
                }
                val startMins = parseTimeToMinutes(startStr)
                val endMins = parseTimeToMinutes(endStr)
                if (startMins == null || endMins == null) {
                    error = "Timings must be in HH:MM format"
                    return@TextButton
                }
                if (startMins >= endMins) {
                    error = "End time must be after start time"
                    return@TextButton
                }

                val finalTeacher = if (periodType == "CLASS" || periodType == "ASSEMBLY") teacher else ""
                val finalRoom = if (periodType == "CLASS" || periodType == "ASSEMBLY") room else ""
                val finalHomework = if (periodType == "CLASS") homeworkLink else ""

                onSave(
                    Lesson(
                        id = lesson?.id ?: 0,
                        timetableId = timetableId,
                        name = name,
                        teacher = finalTeacher,
                        room = finalRoom,
                        dayOfWeek = dayOfWeek,
                        startTimeMinutes = startMins,
                        endTimeMinutes = endMins,
                        colorHex = selectedColor,
                        notes = notes,
                        homeworkLink = finalHomework,
                        weekType = weekType,
                        periodType = periodType
                    )
                )
            }) {
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

// Convert HH:MM to minutes since midnight
fun parseTimeToMinutes(timeStr: String): Int? {
    val parts = timeStr.split(":")
    if (parts.size != 2) return null
    val hrs = parts[0].toIntOrNull() ?: return null
    val mins = parts[1].toIntOrNull() ?: return null
    if (hrs !in 0..23 || mins !in 0..59) return null
    return hrs * 60 + mins
}
