package com.grinkware.timeplans.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Suppress("DEPRECATION")
fun SettingsScreen(viewModel: AppViewModel) {
    val settingsState by viewModel.settings
    val timetables = viewModel.timetables.value
    val activeTimetable = viewModel.activeTimetable.value
    val spacing = LocalSpacing.current

    // Dialog flags
    val showCreateYearDialog = remember { mutableStateOf(false) }
    val showDuplicateYearDialog = remember { mutableStateOf(false) }
    val showPromoteYearDialog = remember { mutableStateOf(false) }
    val showExportDialog = remember { mutableStateOf(false) }
    val showImportDialog = remember { mutableStateOf(false) }
    var exportShareCode by remember { mutableStateOf("") }
    var importShareCode by remember { mutableStateOf("") }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    val showFullExportDialog = remember { mutableStateOf(false) }
    val showFullRestoreDialog = remember { mutableStateOf(false) }
    var fullExportCode by remember { mutableStateOf("") }
    var fullRestoreCode by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
        contentPadding = PaddingValues(bottom = 100.dp, top = spacing.small)
    ) {
        // --- 1. THEME & DISPLAY CUSTOMIZATION ---
        item {
            Text(
                text = "Display Customization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    // Dark Mode Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Force Dark Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Overrides system theme settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settingsState.darkMode == "DARK",
                            onCheckedChange = { checked ->
                                viewModel.updateSetting("darkMode", if (checked) "DARK" else "LIGHT")
                            }
                        )
                    }

                    HorizontalDivider()

                    // AMOLED Black Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AMOLED Pitch Black Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Saves battery on OLED panels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settingsState.amoledMode,
                            onCheckedChange = { viewModel.updateSetting("amoledMode", it.toString()) }
                        )
                    }

                    HorizontalDivider()

                    // Density Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Layout Density", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Size spacing of cards and list paddings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            listOf("COMPACT", "NORMAL", "SPACIOUS").forEach { density ->
                                val isSelected = settingsState.density == density
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSetting("density", density) },
                                    label = { Text(density, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Font Family Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("App Typography Font", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        
                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            verticalArrangement = Arrangement.spacedBy(spacing.small)
                        ) {
                            listOf("SYSTEM", "GOOGLE_SANS", "INTER", "MONOSPACE", "SANS", "SERIF").forEach { font ->
                                val isSelected = settingsState.fontStyle == font
                                val displayName = font.replace("_", " ")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateSetting("fontStyle", font) },
                                    label = { Text(displayName, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 1.5 TODAY SCREEN CUSTOMIZATION ---
        item {
            Text(
                text = "Today Screen Customization",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Text(
                        text = "Customize the order and visibility of sections on the Today dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val orderList = remember(settingsState.todayWidgetOrder) {
                        settingsState.todayWidgetOrder.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toMutableList()
                    }
                    val visibilitySet = remember(settingsState.todayWidgetVisibility) {
                        settingsState.todayWidgetVisibility.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                    }

                    val widgetLabels = mapOf(
                        "TIMELINE" to "Today's Timeline",
                        "COUNTDOWN" to "School Year Countdown",
                        "HERO" to "Today's Status Hero",
                        "ATTENDANCE" to "Daily Attendance Updater",
                        "INSIGHTS" to "Progress & Insights",
                        "DEADLINES" to "Upcoming Deadlines"
                    )

                    orderList.forEachIndexed { index, widgetKey ->
                        val label = widgetLabels[widgetKey] ?: widgetKey
                        val isVisible = visibilitySet.contains(widgetKey)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Up button
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val temp = orderList[index]
                                            orderList[index] = orderList[index - 1]
                                            orderList[index - 1] = temp
                                            viewModel.updateSetting("todayWidgetOrder", orderList.joinToString(","))
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up"
                                    )
                                }

                                // Down button
                                IconButton(
                                    onClick = {
                                        if (index < orderList.size - 1) {
                                            val temp = orderList[index]
                                            orderList[index] = orderList[index + 1]
                                            orderList[index + 1] = temp
                                            viewModel.updateSetting("todayWidgetOrder", orderList.joinToString(","))
                                        }
                                    },
                                    enabled = index < orderList.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down"
                                    )
                                }

                                Switch(
                                    checked = isVisible,
                                    onCheckedChange = { checked ->
                                        val newVisibility = if (checked) {
                                            visibilitySet + widgetKey
                                        } else {
                                            visibilitySet - widgetKey
                                        }
                                        viewModel.updateSetting("todayWidgetVisibility", newVisibility.joinToString(","))
                                    }
                                )
                            }
                        }
                        if (index < orderList.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // --- 2. SCHOOL YEAR MANAGEMENT ---
        item {
            Text(
                text = "Timetables & School Years",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    // Active school year picker
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Switch Active School Year", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { yearDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(activeTimetable?.yearName ?: "Select Timetable Year")
                            }

                            DropdownMenu(
                                expanded = yearDropdownExpanded,
                                onDismissRequest = { yearDropdownExpanded = false }
                            ) {
                                timetables.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr.yearName) },
                                        onClick = {
                                            viewModel.setSchoolYearActive(yr.id)
                                            yearDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // DB operations buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = { showCreateYearDialog.value = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Year", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showDuplicateYearDialog.value = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duplicate", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = { showPromoteYearDialog.value = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Promote Year", fontSize = 11.sp)
                        }

                        if (timetables.size > 1 && activeTimetable != null) {
                            Button(
                                onClick = {
                                    viewModel.deleteSchoolYear(activeTimetable.id)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete Active", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 3. NOTIFICATION & CALENDAR PREFERENCES ---
        item {
            Text(
                text = "Preferences & Calendar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    // Enable Notifications switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Class Reminders",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Receive push alerts before your classes start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settingsState.showNotifications,
                            onCheckedChange = {
                                viewModel.updateSetting("showNotifications", it.toString())
                            }
                        )
                    }

                    // Show slider only if enabled
                    AnimatedVisibility(
                        visible = settingsState.showNotifications,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Notification Lead Time: ${settingsState.alarmLeadMinutes} minutes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Remind me this many minutes before class begins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = settingsState.alarmLeadMinutes.toFloat(),
                                onValueChange = {
                                    viewModel.updateSetting("alarmLeadMinutes", it.toInt().toString())
                                },
                                valueRange = 0f..30f,
                                steps = 5
                            )
                        }
                    }

                    HorizontalDivider()

                    // End of year date
                    var endYearInput by remember { mutableStateOf(settingsState.endOfYearDate) }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    val calendar = Calendar.getInstance()
                    val datePickerDialog = remember {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                endYearInput = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("End of Current School Year Date", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Calculates the countdown metric displayed on Dashboard.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.small),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = endYearInput,
                                onValueChange = { endYearInput = it },
                                placeholder = { Text("YYYY-MM-DD") },
                                singleLine = true,
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { datePickerDialog.show() }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    viewModel.updateSetting("endOfYearDate", endYearInput)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Set")
                            }
                        }
                    }
                }
            }
        }

        // --- 4. BACKUP & SHARING ---
        item {
            Text(
                text = "Backup & Sharing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Text(
                        text = "Transfer and share your school year timetables with classmate devices instantly using share codes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = {
                                val code = viewModel.exportActiveTimetable()
                                if (code != null) {
                                    exportShareCode = code
                                    showExportDialog.value = true
                                } else {
                                    Toast.makeText(context, "Failed to export active timetable.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = activeTimetable != null
                        ) {
                            Text("Export Active", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                importShareCode = ""
                                showImportDialog.value = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Import Code", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- 5. FULL DATABASE BACKUP & RESTORE ---
        item {
            Text(
                text = "Full App Database Backup & Restore",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Text(
                        text = "Generate a single shareable text code containing your entire database (timetables, tasks, grades, flashcards, focus records, study targets, and settings) to easily move between devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.small)
                    ) {
                        Button(
                            onClick = {
                                val code = viewModel.exportFullBackup()
                                if (code != null) {
                                    fullExportCode = code
                                    showFullExportDialog.value = true
                                } else {
                                    Toast.makeText(context, "Failed to export data.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Export Backup", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                fullRestoreCode = ""
                                showFullRestoreDialog.value = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Restore Backup", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // --- 6. LOCAL RESTORE POINTS (DATA SAFETY HISTORY) ---
        item {
            Text(
                text = "Local Restore Points",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = spacing.small)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(spacing.medium)
                ) {
                    Text(
                        text = "Create quick local recovery checkpoints of your database. Up to 3 historical states are automatically saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            viewModel.createLocalRestorePoint()
                            Toast.makeText(context, "Local checkpoint created!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Checkpoint")
                    }

                    val restorePoints = viewModel.localRestorePoints.value
                    if (restorePoints.isEmpty()) {
                        Text(
                            text = "No local checkpoints saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                            restorePoints.forEachIndexed { index, point ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = spacing.medium, vertical = spacing.small),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Checkpoint #${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = point.first,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.restoreLocalPoint(index)
                                            Toast.makeText(context, "Checkpoint restored successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text("Restore", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- CREATE YEAR DIALOG ---
    if (showCreateYearDialog.value) {
        var newName by remember { mutableStateOf("") }
        var doubleWeek by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showCreateYearDialog.value = false },
            title = { Text("Create School Year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name (e.g. Year 10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Double Week Cycle", fontWeight = FontWeight.Bold)
                            Text("Enables Week A & Week B split", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = doubleWeek, onCheckedChange = { doubleWeek = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.trim().isNotEmpty()) {
                        viewModel.createSchoolYear(newName, doubleWeek)
                        showCreateYearDialog.value = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateYearDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DUPLICATE YEAR DIALOG ---
    if (showDuplicateYearDialog.value && activeTimetable != null) {
        var duplicateName by remember { mutableStateOf("${activeTimetable.yearName} (Copy)") }
        AlertDialog(
            onDismissRequest = { showDuplicateYearDialog.value = false },
            title = { Text("Duplicate Timetable Structure") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text("Copy all classes, timings, and details from '${activeTimetable.yearName}' into a new year model.")
                    OutlinedTextField(
                        value = duplicateName,
                        onValueChange = { duplicateName = it },
                        label = { Text("New Year Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (duplicateName.trim().isNotEmpty()) {
                        viewModel.duplicateSchoolYear(activeTimetable.id, duplicateName)
                        showDuplicateYearDialog.value = false
                    }
                }) {
                    Text("Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateYearDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- PROMOTE YEAR DIALOG ---
    if (showPromoteYearDialog.value && activeTimetable != null) {
        var newYearName by remember { mutableStateOf("Year 11 (Promoted)") }
        AlertDialog(
            onDismissRequest = { showPromoteYearDialog.value = false },
            title = { Text("Promote Year") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text("Promote timetable to the next grade/year. Copies the core structure (subject names, timings, colors) to clean, unlogged database rows.", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = newYearName,
                        onValueChange = { newYearName = it },
                        label = { Text("Promoted Year Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newYearName.trim().isNotEmpty()) {
                        viewModel.promoteSchoolYear(activeTimetable.id, newYearName)
                        showPromoteYearDialog.value = false
                    }
                }) {
                    Text("Promote")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPromoteYearDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- EXPORT TIMETABLE DIALOG ---
    if (showExportDialog.value) {
        AlertDialog(
            onDismissRequest = { showExportDialog.value = false },
            title = { Text("Export Timetable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text("Copy this share code and send it to other students. They can paste this code in their settings to load your timetable classes, times, and colors.")
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = exportShareCode,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(exportShareCode))
                    Toast.makeText(context, "Share code copied to clipboard", Toast.LENGTH_SHORT).show()
                    showExportDialog.value = false
                }) {
                    Text("Copy Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog.value = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- IMPORT TIMETABLE DIALOG ---
    if (showImportDialog.value) {
        val importError = remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showImportDialog.value = false },
            title = { Text("Import Timetable") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text("Paste the timetable share code below to import all lessons and settings.")
                    
                    OutlinedTextField(
                        value = importShareCode,
                        onValueChange = { 
                            importShareCode = it
                            importError.value = false
                        },
                        label = { Text("Share Code") },
                        placeholder = { Text("Paste code here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        isError = importError.value,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    if (importError.value) {
                        Text(
                            text = "Invalid share code. Please check and try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (importShareCode.trim().isNotEmpty()) {
                        val success = viewModel.importTimetable(importShareCode.trim())
                        if (success) {
                            Toast.makeText(context, "Timetable imported successfully!", Toast.LENGTH_SHORT).show()
                            showImportDialog.value = false
                        } else {
                            importError.value = true
                        }
                    }
                }) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- FULL EXPORT DIALOG ---
    if (showFullExportDialog.value) {
        AlertDialog(
            onDismissRequest = { showFullExportDialog.value = false },
            title = { Text("Full App Data Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text("Copy this backup code. Save it in a safe place. You can paste it on another device to restore all timetables, logs, flashcards, grades, and settings.")
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = fullExportCode,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(fullExportCode))
                    Toast.makeText(context, "Backup code copied to clipboard", Toast.LENGTH_SHORT).show()
                    showFullExportDialog.value = false
                }) {
                    Text("Copy Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullExportDialog.value = false }) {
                    Text("Close")
                }
            }
        )
    }

    // --- FULL RESTORE DIALOG ---
    if (showFullRestoreDialog.value) {
        val importError = remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showFullRestoreDialog.value = false },
            title = { Text("Restore Full App Data") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Text(
                        text = "WARNING: Restoring will overwrite all existing data in this app. This action is permanent and cannot be undone!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Paste the full backup code below:")
                    
                    OutlinedTextField(
                        value = fullRestoreCode,
                        onValueChange = { 
                            fullRestoreCode = it
                            importError.value = false
                        },
                        label = { Text("Backup Code") },
                        placeholder = { Text("Paste code here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        isError = importError.value,
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(8.dp)
                    )
                    
                    if (importError.value) {
                        Text(
                            text = "Invalid backup code. Please check and try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (fullRestoreCode.trim().isNotEmpty()) {
                            val success = viewModel.importFullBackup(fullRestoreCode.trim())
                            if (success) {
                                Toast.makeText(context, "Full app data restored successfully!", Toast.LENGTH_SHORT).show()
                                showFullRestoreDialog.value = false
                            } else {
                                importError.value = true
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Overwrite & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFullRestoreDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
