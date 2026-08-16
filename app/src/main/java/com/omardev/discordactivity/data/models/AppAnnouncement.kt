package com.omardev.discordactivity.data.models

import com.google.gson.annotations.SerializedName

data class AppAnnouncement(
    @SerializedName("id") val id: String = System.currentTimeMillis().toString(),
    @SerializedName("title") val title: String = "📢 New Update Available!",
    @SerializedName("message") val message: String = "Version v2.4 is now available with new features and enhancements.",
    @SerializedName("version") val version: String = "2.4.0",
    @SerializedName("target_platform") val targetPlatform: String = "ALL", // "ALL", "VR", "PS5", "XBOX", "MOBILE"
    @SerializedName("target_user_id") val targetUserId: String = "", // empty means for everyone
    @SerializedName("button_label") val buttonLabel: String = "Download Update 🚀",
    @SerializedName("button_url") val buttonUrl: String = "https://github.com/omarsaber6545-hue/Discord-Activity-Status-app/releases",
    @SerializedName("is_important") val isImportant: Boolean = true,
    @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
)
