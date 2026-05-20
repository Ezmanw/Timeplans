package com.grinkware.timeplans.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grinkware.timeplans.ui.screens.*
import com.grinkware.timeplans.ui.theme.LocalSpacing

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Today", Icons.Default.Home)
    object Timetable : Screen("timetable", "Timetable", Icons.Default.DateRange)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.Check)
    object Attendance : Screen("attendance", "Stats", Icons.Default.Star)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: AppViewModel) {
    val settingsState = viewModel.settings.value

    if (!settingsState.onboardingCompleted) {
        OnboardingScreen(viewModel)
        return
    }

    val currentScreen = remember { mutableStateOf<Screen>(Screen.Dashboard) }
    val showSearchDialog = remember { mutableStateOf(false) }
    val spacing = LocalSpacing.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (currentScreen.value == Screen.Dashboard) "TimePlans" else currentScreen.value.title,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    if (currentScreen.value == Screen.Dashboard || currentScreen.value == Screen.Timetable) {
                        IconButton(onClick = { showSearchDialog.value = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Lessons"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = spacing.extraSmall
            ) {
                val screens = listOf(
                    Screen.Dashboard,
                    Screen.Timetable,
                    Screen.Tasks,
                    Screen.Attendance,
                    Screen.Settings
                )
                screens.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen.value.route == screen.route,
                        onClick = { currentScreen.value = screen },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(targetState = currentScreen.value) { screen ->
                when (screen) {
                    Screen.Dashboard -> DashboardScreen(viewModel)
                    Screen.Timetable -> TimetableScreen(viewModel)
                    Screen.Tasks -> TasksScreen(viewModel)
                    Screen.Attendance -> AttendanceScreen(viewModel)
                    Screen.Settings -> SettingsScreen(viewModel)
                }
            }
        }
    }

    if (showSearchDialog.value) {
        SearchDialog(
            viewModel = viewModel,
            onDismiss = { showSearchDialog.value = false }
        )
    }
}
