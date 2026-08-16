package com.omardev.discordactivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.omardev.discordactivity.ui.screens.MainScreen
import com.omardev.discordactivity.ui.screens.MainViewModel
import com.omardev.discordactivity.ui.theme.DiscordActivityTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install modern Android Splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkNotificationPermission()

        setContent {
            DiscordActivityTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
