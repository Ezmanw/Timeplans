package com.grinkware.timeplans

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.grinkware.timeplans.ui.AppViewModel
import com.grinkware.timeplans.ui.MainApp
import com.grinkware.timeplans.ui.theme.TimeplansTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS for Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Initialize AppViewModel
        val viewModel = ViewModelProvider(this)[AppViewModel::class.java]

        setContent {
            val settings = viewModel.settings.value
            TimeplansTheme(
                darkMode = settings.darkMode,
                amoledMode = settings.amoledMode,
                dynamicColor = settings.dynamicTheme,
                density = settings.density,
                fontStyle = settings.fontStyle
            ) {
                MainApp(viewModel = viewModel)
            }
        }
    }
}