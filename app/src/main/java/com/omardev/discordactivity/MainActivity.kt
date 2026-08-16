package com.omardev.discordactivity

import android.Manifest
import android.content.Intent
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
import com.omardev.discordactivity.data.models.AnnouncementType
import com.omardev.discordactivity.data.models.AppAnnouncement
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
        handleNotificationIntent(intent)

        setContent {
            DiscordActivityTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val openAnnouncement = intent.getBooleanExtra("open_announcement", false)
        if (openAnnouncement) {
            val id = intent.getStringExtra("announcement_id") ?: System.currentTimeMillis().toString()
            val title = intent.getStringExtra("announcement_title") ?: "تنبيه من الإدارة"
            val message = intent.getStringExtra("announcement_message") ?: ""
            val author = intent.getStringExtra("announcement_author") ?: "Omar Dev"
            val typeStr = intent.getStringExtra("announcement_type") ?: AnnouncementType.UPDATE.name
            val type = try { AnnouncementType.valueOf(typeStr) } catch (e: Exception) { AnnouncementType.UPDATE }
            val timestamp = intent.getLongExtra("announcement_timestamp", System.currentTimeMillis())

            val announcement = AppAnnouncement(
                id = id,
                title = title,
                message = message,
                author = author,
                type = type,
                timestamp = timestamp
            )
            viewModel.showDirectAnnouncement(announcement)
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
