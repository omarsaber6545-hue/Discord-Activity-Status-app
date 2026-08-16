package com.omardev.discordactivity.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omardev.discordactivity.App
import com.omardev.discordactivity.MainActivity
import com.omardev.discordactivity.R
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.NotificationLevel
import com.omardev.discordactivity.network.DiscordGatewayClient
import com.omardev.discordactivity.network.GatewayConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

class DiscordPresenceService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): DiscordPresenceService = this@DiscordPresenceService
    }

    private val binder = LocalBinder()
    private var gatewayClient: DiscordGatewayClient? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_PRESENCE = "ACTION_UPDATE_PRESENCE"

        val connectionState = MutableStateFlow(GatewayConnectionState.DISCONNECTED)
        val notificationsLog = MutableStateFlow<List<AppNotification>>(emptyList())

        private val _toastEvents = MutableSharedFlow<AppNotification>()
        val toastEvents = _toastEvents.asSharedFlow()

        fun startService(context: Context) {
            val intent = Intent(context, DiscordPresenceService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DiscordPresenceService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundWithNotification()
                connectGateway()
            }
            ACTION_STOP -> {
                disconnectGateway()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_PRESENCE -> {
                val prefs = (application as App).preferencesManager
                gatewayClient?.updatePresence(prefs.presence)
                updateNotification("Active: Playing ${prefs.presence.gameName}")
            }
        }
        return START_STICKY
    }

    private fun connectGateway() {
        val prefs = (application as App).preferencesManager
        val token = prefs.token
        val isUserToken = prefs.isUserToken
        val platform = prefs.devicePlatform
        val presence = prefs.presence

        gatewayClient?.disconnect()
        gatewayClient = DiscordGatewayClient(
            token = token,
            isUserToken = isUserToken,
            platform = platform,
            currentPresence = presence,
            voiceChannelId = prefs.voiceChannelId,
            voiceMute = prefs.voiceMute,
            voiceDeaf = prefs.voiceDeaf,
            onStateChanged = { state ->
                connectionState.value = state
                val stateText = when (state) {
                    GatewayConnectionState.IDENTIFIED -> "Online: Playing ${presence.gameName}"
                    GatewayConnectionState.CONNECTING -> "Connecting to Discord..."
                    GatewayConnectionState.CONNECTED -> "Handshake complete..."
                    GatewayConnectionState.ERROR -> "Connection Error"
                    GatewayConnectionState.DISCONNECTED -> "Disconnected"
                }
                updateNotification(stateText)
            },
            onLog = { notification ->
                val current = notificationsLog.value.toMutableList()
                current.add(0, notification)
                if (current.size > 100) current.removeAt(current.lastIndex)
                notificationsLog.value = current
            }
        )

        gatewayClient?.connect()
    }

    private fun disconnectGateway() {
        gatewayClient?.disconnect()
        gatewayClient = null
        connectionState.value = GatewayConnectionState.DISCONNECTED
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification("Connecting to Discord Gateway...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle("omar dev - Discord Activity")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        disconnectGateway()
        super.onDestroy()
    }
}
