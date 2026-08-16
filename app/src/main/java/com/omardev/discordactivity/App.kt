package com.omardev.discordactivity

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.omardev.discordactivity.data.preferences.PreferencesManager

class App : Application() {

    companion object {
        const val CHANNEL_ID = "discord_presence_channel"
        const val CHANNEL_ANNOUNCEMENTS_ID = "omar_dev_announcements"
        lateinit var instance: App
            private set
    }

    lateinit var preferencesManager: PreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesManager = PreferencesManager(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            // 1. Service Background Running Channel (Low Importance)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.discord_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.discord_service_channel_desc)
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            // 2. High-Priority Push Announcements & Alerts Channel (Heads-up banner + sound + vibration)
            val announcementChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS_ID,
                "Omar Dev Broadcasts & Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications from Omar Dev Admin"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            manager.createNotificationChannel(announcementChannel)
        }
    }
}
