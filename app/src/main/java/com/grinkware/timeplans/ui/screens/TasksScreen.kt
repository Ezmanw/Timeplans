package com.grinkware.timeplans.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.ExamItem
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.data.TaskItem
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: AppViewModel) {
    val tasks = viewModel.tasks.value
    val exams = viewModel.exams.value
    val lessons = viewModel.lessons.value
    val spacing = LocalSpacing.current

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Homework", "Exams", "Revision")

    // Dialog state
    val showAddHomeworkDialog = remember { mutableStateOf(false) }
    val showAddExamDialog = remember { mutableStateOf(false) }
    val showAddRevisionDialog = remember { mutableStateOf(false) }

    // Sorting state
    var sortBy by remember(viewModel.settings.value.tasksSortBy) { mutableStateOf(viewModel.settings.value.tasksSortBy) }
    val priorityWeight = mapOf("HIGH" to 0, "MEDIUM" to 1, "LOW" to 2)

    val filteredSortedTasks = remember(tasks, sortBy) {
        when (sortBy) {
            "DUE_DESC" -> tasks.sortedByDescending { it.dueDate }
            "PRIORITY" -> tasks.sortedWith(compareBy({ priorityWeight[it.priority] ?: 1 }, { it.dueDate }))
            else -> tasks.sortedBy { it.dueDate }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> showAddHomeworkDialog.value = true
                        1 -> showAddExamDialog.value = true
                        2 -> showAddRevisionDialog.value = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTabIndex == 0 || selectedTabIndex == 2) {
                val currentTaskType = if (selectedTabIndex == 0) "HOMEWORK" else "REVISION"
                val hasCompleted = filteredSortedTasks.any { it.taskType == currentTaskType && it.isCompleted }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.medium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sort:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        listOf("DUE_ASC" to "Due date (Soonest)", "DUE_DESC" to "Due date (Latest)", "PRIORITY" to "Priority").forEach { (option, label) ->
                            FilterChip(
                                selected = sortBy == option,
                                onClick = {
                                    sortBy = option
                                    viewModel.updateSetting("tasksSortBy", option)
                                },
                                label = { Text(label, fontSize = 10.sp) }
                            )
                        }
                    }

                    if (hasCompleted) {
                        TextButton(
                            onClick = { viewModel.clearCompletedTasks(currentTaskType) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.padding(start = spacing.small)
                        ) {
                            Text("Clear Done", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = spacing.medium)
            ) {
                when (selectedTabIndex) {
                    0 -> HomeworkList(
                        tasks = filteredSortedTasks.filter { it.taskType == "HOMEWORK" },
                        lessons = lessons,
                        spacing = spacing,
                        onToggle = { viewModel.toggleTask(it) },
                        onDelete = { viewModel.deleteTask(it.id) }
                    )
                    1 -> ExamList(
                        exams = exams,
                        spacing = spacing,
                        onDelete = { viewModel.deleteExam(it) }
                    )
                    2 -> RevisionList(
                        tasks = filteredSortedTasks.filter { it.taskType == "REVISION" },
                        spacing = spacing,
                        onToggle = { viewModel.toggleTask(it) },
                        onDelete = { viewModel.deleteTask(it.id) }
                    )
                }
            }
        }
    }

    // Homework Dialog
    if (showAddHomeworkDialog.value) {
        AddHomeworkDialog(
            lessons = lessons,
            onDismiss = { showAddHomeworkDialog.value = false },
            onSave = { title, desc, date, lessonId, priority ->
                viewModel.addTask(
                    TaskItem(
                        title = title,
                        description = desc,
                        dueDate = date,
                        lessonId = lessonId,
                        taskType = "HOMEWORK",
                        priority = priority
                    )
                )
                showAddHomeworkDialog.value = false
            }
        )
    }

    // Exam Dialog
    if (showAddExamDialog.value) {
        AddExamDialog(
            onDismiss = { showAddExamDialog.value = false },
            onSave = { subject, date, time, room, notes ->
                viewModel.addExam(
                    ExamItem(
                        subject = subject,
                        date = date,
                        time = time,
                        room = room,
                        notes = notes
                    )
                )
                showAddExamDialog.value = false
            }
        )
    }

    // Revision Dialog
    if (showAddRevisionDialog.value) {
        AddRevisionDialog(
            onDismiss = { showAddRevisionDialog.value = false },
            onSave = { topic, desc, date, priority ->
                viewModel.addTask(
                    TaskItem(
                        title = topic,
                        description = desc,
                        dueDate = date,
                        taskType = "REVISION",
                        priority = priority
                    )
                )
                showAddRevisionDialog.value = false
            }
        )
    }
}

// --- SUB-SCREEN LISTS ---

@Composable
fun HomeworkList(
    tasks: List<TaskItem>,
    lessons: List<Lesson>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(text = "No pending homework tasks", spacing = spacing)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = 80.dp, top = spacing.small),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks) { task ->
                val linkedLesson = lessons.find { it.id == task.lessonId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggle(task) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (task.description.isNotEmpty()) {
                                Text(
                                    text = task.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Due: ${task.dueDate}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val priorityColor = when (task.priority) {
                                    "HIGH" -> Color(0xFFD32F2F)
                                    "MEDIUM" -> Color(0xFFF57C00)
                                    "LOW" -> Color(0xFF388E3C)
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(priorityColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.priority,
                                        fontSize = 10.sp,
                                        color = priorityColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (linkedLesson != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(linkedLesson.colorHex).copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = linkedLesson.name,
                                            fontSize = 10.sp,
                                            color = Color(linkedLesson.colorHex),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { onDelete(task) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExamList(
    exams: List<ExamItem>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing,
    onDelete: (Long) -> Unit
) {
    if (exams.isEmpty()) {
        EmptyState(text = "No upcoming exams scheduled", spacing = spacing)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = 80.dp, top = spacing.small),
            modifier = Modifier.fillMaxSize()
        ) {
            items(exams) { exam ->
                ExamCard(exam = exam, spacing = spacing, onDelete = { onDelete(exam.id) })
            }
        }
    }
}

@Composable
fun ExamCard(
    exam: ExamItem,
    spacing: com.grinkware.timeplans.ui.theme.Spacing,
    onDelete: () -> Unit
) {
    var countdownText by remember { mutableStateOf("") }

    // Live countdown updates every second
    LaunchedEffect(exam) {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val examDateStr = "${exam.date} ${exam.time}"
        while (true) {
            try {
                val examDate = format.parse(examDateStr)
                if (examDate != null) {
                    val now = Date()
                    val diff = examDate.time - now.time
                    if (diff <= 0) {
                        countdownText = "Started / Finished"
                    } else {
                        val days = diff / (1000 * 60 * 60 * 24)
                        val hours = (diff / (1000 * 60 * 60)) % 24
                        val mins = (diff / (1000 * 60)) % 60
                        val secs = (diff / 1000) % 60
                        countdownText = "${days}d ${hours}h ${mins}m ${secs}s"
                    }
                }
            } catch (_: Exception) {
                countdownText = "Error"
            }
            delay(1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                    text = exam.subject,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                Column {
                    Text("Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(exam.date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(exam.time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                if (exam.room.isNotEmpty()) {
                    Column {
                        Text("Room", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(exam.room, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (exam.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${exam.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(vertical = spacing.small),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = countdownText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RevisionList(
    tasks: List<TaskItem>,
    spacing: com.grinkware.timeplans.ui.theme.Spacing,
    onToggle: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(text = "No revision schedules created", spacing = spacing)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = 80.dp, top = spacing.small),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tasks) { task ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (task.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        Checkbox(
                            checked = task.isCompleted,
                            onCheckedChange = { onToggle(task) }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (task.description.isNotEmpty()) {
                                Text(
                                    text = "Goal: ${task.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Date: ${task.dueDate}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                val priorityColor = when (task.priority) {
                                    "HIGH" -> Color(0xFFD32F2F)
                                    "MEDIUM" -> Color(0xFFF57C00)
                                    "LOW" -> Color(0xFF388E3C)
                                    else -> Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(priorityColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.priority,
                                        fontSize = 10.sp,
                                        color = priorityColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { onDelete(task) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(text: String, spacing: com.grinkware.timeplans.ui.theme.Spacing) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "No items",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- DIALOGS ---

@Composable
fun AddHomeworkDialog(
    lessons: List<Lesson>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?, String) -> Unit
) {
    val spacing = LocalSpacing.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val defaultDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var date by remember { mutableStateOf(defaultDateStr) }
    var linkedLessonId by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var expanded by remember { mutableStateOf(false) }

    val error = remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Homework") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (error.value.isNotEmpty()) {
                    Text(error.value, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Due Date (YYYY-MM-DD)*") },
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { level ->
                        val isSelected = priority == level
                        val color = when (level) {
                            "HIGH" -> Color(0xFFD32F2F)
                            "MEDIUM" -> Color(0xFFF57C00)
                            "LOW" -> Color(0xFF388E3C)
                            else -> Color.Gray
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = level },
                            label = { Text(level, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                                selectedLeadingIconColor = color
                            )
                        )
                    }
                }

                // Lesson Link Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val activeLessonName = lessons.find { it.id == linkedLessonId }?.name ?: "Link to Subject/Lesson"
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text(activeLessonName)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                  linkedLessonId = null
                                  expanded = false
                            }
                        )
                        lessons.distinctBy { it.name }.forEach { lesson ->
                            DropdownMenuItem(
                                text = { Text(lesson.name) },
                                onClick = {
                                    linkedLessonId = lesson.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().isEmpty()) {
                    error.value = "Title is required"
                    return@TextButton
                }
                onSave(title, desc, date, linkedLessonId, priority)
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

@Composable
fun AddExamDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    val spacing = LocalSpacing.current
    var subject by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-06-15") }
    var time by remember { mutableStateOf("09:00") }
    var room by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val error = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Exam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (error.value.isNotEmpty()) {
                    Text(error.value, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Exam Date (YYYY-MM-DD)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (HH:MM)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Room") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Topics covered") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (subject.trim().isEmpty()) {
                    error.value = "Subject is required"
                    return@TextButton
                }
                onSave(subject, date, time, room, notes)
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

@Composable
fun AddRevisionDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    val spacing = LocalSpacing.current
    var topic by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("2026-05-20") }
    var priority by remember { mutableStateOf("MEDIUM") }

    val error = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Revision Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                if (error.value.isNotEmpty()) {
                    Text(error.value, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Revision Topic*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Study Goals / Chapter reference") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Revision Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.small),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { level ->
                        val isSelected = priority == level
                        val color = when (level) {
                            "HIGH" -> Color(0xFFD32F2F)
                            "MEDIUM" -> Color(0xFFF57C00)
                            "LOW" -> Color(0xFF388E3C)
                            else -> Color.Gray
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = level },
                            label = { Text(level, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                                selectedLeadingIconColor = color
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (topic.trim().isEmpty()) {
                    error.value = "Topic is required"
                    return@TextButton
                }
                onSave(topic, desc, date, priority)
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
