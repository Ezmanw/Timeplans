package com.grinkware.timeplans.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.grinkware.timeplans.data.Lesson
import com.grinkware.timeplans.data.TaskItem
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: AppViewModel) {
    var currentStep by remember { mutableStateOf(0) }
    val spacing = LocalSpacing.current
    val focusManager = LocalFocusManager.current

    // Onboarding preferences state
    var yearName by remember { mutableStateOf("") }
    var hasTwoWeeks by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(true) }
    var alarmLeadMinutes by remember { mutableStateOf(10) }
    val tempLessons = remember { mutableStateListOf<Lesson>() }
    val tempTasks = remember { mutableStateListOf<TaskItem>() }

    val stepsCount = 7

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(spacing.medium),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Logo and step indicators
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = spacing.medium)
            ) {
                Text(
                    text = "TimePlans",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-1).sp
                )
                
                Spacer(modifier = Modifier.height(spacing.small))
                
                // Dot indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(stepsCount) { index ->
                        val isSelected = currentStep == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }

            // Step Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = spacing.medium),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Crossfade(
                    targetState = currentStep,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.medium)
                ) { step ->
                    when (step) {
                        0 -> OnboardingWelcome()
                        1 -> OnboardingAcademicYear(
                            yearName = yearName,
                            onYearChange = { yearName = it },
                            hasTwoWeeks = hasTwoWeeks,
                            onTwoWeeksChange = { hasTwoWeeks = it },
                            onDoneKeyboard = { focusManager.clearFocus() }
                        )
                        2 -> OnboardingNotifications(
                            showNotifs = showNotifs,
                            onShowNotifsChange = { showNotifs = it },
                            alarmLeadMinutes = alarmLeadMinutes,
                            onLeadChange = { alarmLeadMinutes = it }
                        )
                        3 -> {
                            val settingsState = viewModel.settings.value
                            OnboardingAesthetics(
                                darkModeSetting = settingsState.darkMode,
                                onDarkModeChange = { viewModel.updateSetting("darkMode", it) },
                                amoledMode = settingsState.amoledMode,
                                onAmoledChange = { viewModel.updateSetting("amoledMode", it.toString()) },
                                densitySetting = settingsState.density,
                                onDensityChange = { viewModel.updateSetting("density", it) },
                                fontStyleSetting = settingsState.fontStyle,
                                onFontChange = { viewModel.updateSetting("fontStyle", it) }
                            )
                        }
                        4 -> OnboardingLessons(
                            hasTwoWeeks = hasTwoWeeks,
                            lessons = tempLessons,
                            onAddLesson = { tempLessons.add(it) },
                            onDeleteLesson = { tempLessons.remove(it) }
                        )
                        5 -> OnboardingHomework(
                            lessons = tempLessons,
                            tasks = tempTasks,
                            onAddTask = { tempTasks.add(it) },
                            onDeleteTask = { tempTasks.remove(it) }
                        )
                        6 -> OnboardingFinish()
                    }
                }
            }

            // Bottom Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    TextButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Back", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                Button(
                    onClick = {
                        if (currentStep == stepsCount - 1) {
                            val finalYearName = if (yearName.isBlank()) "Academic Year" else yearName
                            val finalSettings = viewModel.settings.value
                            viewModel.completeOnboarding(
                                yearName = finalYearName,
                                hasTwoWeeks = hasTwoWeeks,
                                showNotifications = showNotifs,
                                alarmLeadMinutes = alarmLeadMinutes,
                                darkMode = finalSettings.darkMode,
                                amoledMode = finalSettings.amoledMode,
                                density = finalSettings.density,
                                fontStyle = finalSettings.fontStyle,
                                initialLessons = tempLessons,
                                initialTasks = tempTasks
                            )
                        } else {
                            currentStep++
                        }
                    },
                    enabled = currentStep != 1 || yearName.isNotBlank(), // require year name to proceed
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(
                        text = if (currentStep == stepsCount - 1) "Finish Setup & Launch" else "Continue",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (currentStep == stepsCount - 1) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingWelcome() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = com.grinkware.timeplans.R.mipmap.timeplans_mono),
                contentDescription = "App Logo",
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to TimePlans",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your school timetable, attendance logs, and productivity tasks organized in one clean, premium workspace.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Dynamic Timetables", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Quickly add classes, subject colors, notes, and room numbers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Smart Attendance Logs", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Keep record of your status and view statistics breakdowns.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Homework & Exam Alarms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Track due items and get alerts before a class starts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun OnboardingAcademicYear(
    yearName: String,
    onYearChange: (String) -> Unit,
    hasTwoWeeks: Boolean,
    onTwoWeeksChange: (Boolean) -> Unit,
    onDoneKeyboard: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Academic Year Setup",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Let's configure your starting school year or semester name.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Current Year or Grade Name", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = yearName,
            onValueChange = onYearChange,
            placeholder = { Text("e.g. Year 10, Grade 11, Fall 2026") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDoneKeyboard() }),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Two-Week Timetable Cycle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Toggles between Week A and Week B alternates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = hasTwoWeeks,
                    onCheckedChange = onTwoWeeksChange
                )
            }
        }
    }
}

@Composable
fun OnboardingNotifications(
    showNotifs: Boolean,
    onShowNotifsChange: (Boolean) -> Unit,
    alarmLeadMinutes: Int,
    onLeadChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Class Reminders",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "TimePlans can remind you before a scheduled class period begins.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Enable Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text("Send alerts before classes start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = showNotifs,
                onCheckedChange = onShowNotifsChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = showNotifs,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lead Time buffer: $alarmLeadMinutes minutes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "How early you want to receive notifications before class starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = alarmLeadMinutes.toFloat(),
                    onValueChange = { onLeadChange(it.toInt()) },
                    valueRange = 0f..30f,
                    steps = 5,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingAesthetics(
    darkModeSetting: String,
    onDarkModeChange: (String) -> Unit,
    amoledMode: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    densitySetting: String,
    onDensityChange: (String) -> Unit,
    fontStyleSetting: String,
    onFontChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Personalize Styling",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Wrap layout customizations inside a small scrollable or standard container
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Theme controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Force Dark Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Force dark appearance overrides", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = darkModeSetting == "DARK",
                    onCheckedChange = { checked ->
                        onDarkModeChange(if (checked) "DARK" else "LIGHT")
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AMOLED Black Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text("Pitch black backgrounds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = amoledMode,
                    onCheckedChange = onAmoledChange
                )
            }

            Divider()

            // Density chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Layout Density", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("COMPACT", "NORMAL", "SPACIOUS").forEach { density ->
                        FilterChip(
                            selected = densitySetting == density,
                            onClick = { onDensityChange(density) },
                            label = { Text(density, fontSize = 10.sp) }
                        )
                    }
                }
            }

            Divider()

            // Font chips (Using wrapped FlowRow to avoid squishing)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Font Style", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SYSTEM", "GOOGLE_SANS", "INTER", "MONOSPACE", "SANS", "SERIF").forEach { font ->
                        FilterChip(
                            selected = fontStyleSetting == font,
                            onClick = { onFontChange(font) },
                            label = { Text(font.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingFinish() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Setup Completed!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Everything is ready! Navigate to the 'Timetable' section at the bottom to configure class hours, teacher assignments, and homework details. Tapping cards will let you add class overrides.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun OnboardingLessons(
    hasTwoWeeks: Boolean,
    lessons: List<Lesson>,
    onAddLesson: (Lesson) -> Unit,
    onDeleteLesson: (Lesson) -> Unit
) {
    val spacing = LocalSpacing.current
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDayFilter by remember { mutableStateOf(1) } // 1 = Mon, ..., 7 = Sun

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Set Up Your Classes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add classes to your timetable below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Days Selector Tab Row
        val daysList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        ScrollableTabRow(
            selectedTabIndex = selectedDayFilter - 1,
            containerColor = Color.Transparent,
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            daysList.forEachIndexed { index, dayName ->
                Tab(
                    selected = selectedDayFilter == index + 1,
                    onClick = { selectedDayFilter = index + 1 },
                    text = { Text(dayName, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredLessons = lessons.filter { it.dayOfWeek == selectedDayFilter }

        if (filteredLessons.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No classes for ${daysList[selectedDayFilter - 1]}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap the button below to add classes on this day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLessons) { lesson ->
                    val dayStr = when (lesson.dayOfWeek) {
                        1 -> "Monday"
                        2 -> "Tuesday"
                        3 -> "Wednesday"
                        4 -> "Thursday"
                        5 -> "Friday"
                        6 -> "Saturday"
                        7 -> "Sunday"
                        else -> "Monday"
                    }
                    val weekStr = if (hasTwoWeeks) {
                        when (lesson.weekType) {
                            "A" -> " (Week A)"
                            "B" -> " (Week B)"
                            else -> " (Both Weeks)"
                        }
                    } else ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isAcademic = lesson.periodType == "CLASS" || lesson.periodType == "ASSEMBLY"
                                val displayName = when (lesson.periodType) {
                                    "BREAK" -> if (lesson.name.equals("Break", ignoreCase = true)) "☕ Break" else "☕ ${lesson.name}"
                                    "LUNCH" -> if (lesson.name.equals("Lunch", ignoreCase = true)) "🍴 Lunch" else "🍴 ${lesson.name}"
                                    "ASSEMBLY" -> if (lesson.name.equals("Assembly", ignoreCase = true)) "📢 Assembly" else "📢 ${lesson.name}"
                                    else -> lesson.name
                                }
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(lesson.colorHex))
                                )
                                Column {
                                    Text(
                                        text = displayName,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "$dayStr$weekStr • ${lesson.startTimeString} - ${lesson.endTimeString}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isAcademic && (lesson.room.isNotEmpty() || lesson.teacher.isNotEmpty())) {
                                        val details = buildString {
                                            if (lesson.room.isNotEmpty()) append("Room ${lesson.room}")
                                            if (lesson.room.isNotEmpty() && lesson.teacher.isNotEmpty()) append(" • ")
                                            if (lesson.teacher.isNotEmpty()) append(lesson.teacher)
                                        }
                                        Text(
                                            text = details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            IconButton(onClick = { onDeleteLesson(lesson) }) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Class Period")
        }
    }

    if (showAddDialog) {
        LessonAddEditDialog(
            timetableId = 0L,
            lesson = null,
            hasDoubleWeek = hasTwoWeeks,
            selectedDay = selectedDayFilter,
            onDismiss = { showAddDialog = false },
            onSave = {
                onAddLesson(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun OnboardingHomework(
    lessons: List<Lesson>,
    tasks: List<TaskItem>,
    onAddTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit
) {
    val spacing = LocalSpacing.current
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Add Upcoming Homework",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Add any homework that you have already been assigned to keep track of deadlines.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(spacing.medium),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No homework added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Optional: Add assignments to get reminders before they are due.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    val linkedLessonName = task.lessonId?.let { tempIndex ->
                        lessons.getOrNull(tempIndex.toInt())?.name
                    } ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.medium),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (task.description.isNotEmpty()) {
                                    Text(
                                        text = task.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val subText = if (linkedLessonName.isNotEmpty()) {
                                    "$linkedLessonName • Due ${task.dueDate}"
                                } else {
                                    "Due ${task.dueDate}"
                                }
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { onDeleteTask(task) }) {
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

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Homework Assignment")
        }
    }

    if (showAddDialog) {
        OnboardingAddHomeworkDialog(
            lessons = lessons,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, date, tempLessonIndex ->
                onAddTask(
                    TaskItem(
                        id = 0L,
                        lessonId = tempLessonIndex,
                        title = title,
                        description = desc,
                        dueDate = date,
                        isCompleted = false,
                        taskType = "HOMEWORK"
                    )
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun OnboardingAddHomeworkDialog(
    lessons: List<Lesson>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?) -> Unit
) {
    val spacing = LocalSpacing.current
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    
    val defaultDateStr = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    var date by remember { mutableStateOf(defaultDateStr) }
    var tempLessonIndex by remember { mutableStateOf<Long?>(null) }
    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
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
                if (error.isNotEmpty()) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Homework Title*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Notes / Description") },
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

                // Lesson Link Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    val activeLessonName = tempLessonIndex?.let { index ->
                        lessons.getOrNull(index.toInt())?.name
                    } ?: "Link to Subject/Class (Optional)"
                    
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
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
                                tempLessonIndex = null
                                expanded = false
                            }
                        )
                        lessons.forEachIndexed { index, lesson ->
                            DropdownMenuItem(
                                text = { Text("${lesson.name} (${when (lesson.dayOfWeek) {
                                    1 -> "Mon"
                                    2 -> "Tue"
                                    3 -> "Wed"
                                    4 -> "Thu"
                                    5 -> "Fri"
                                    6 -> "Sat"
                                    7 -> "Sun"
                                    else -> "Mon"
                                }})") },
                                onClick = {
                                    tempLessonIndex = index.toLong()
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
                    error = "Title is required"
                    return@TextButton
                }
                onSave(title, desc, date, tempLessonIndex)
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

