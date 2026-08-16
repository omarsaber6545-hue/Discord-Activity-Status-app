package com.omardev.discordactivity.data.models

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NotificationLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class AppNotification(
    val id: Long = System.currentTimeMillis(),
    val level: NotificationLevel,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
