package com.grinkware.timeplans.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grinkware.timeplans.data.SchoolYear
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.theme.LocalSpacing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settingsState by viewModel.settings
    val timetables = viewModel.timetables.value
    val activeTimetable = viewModel.activeTimetable.value
    val spacing = LocalSpacing.current

    // Dialog flags
    val showCreateYearDialog = remember { mutableStateOf(false) }
    val showDuplicateYearDialog = remember { mutableStateOf(false) }
    val showPromoteYearDialog = remember { mutableStateOf(false) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

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

                    Divider()

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

                    Divider()

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

                    Divider()

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

                    Divider()

                    // End of year date
                    var endYearInput by remember { mutableStateOf(settingsState.endOfYearDate) }
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
}
