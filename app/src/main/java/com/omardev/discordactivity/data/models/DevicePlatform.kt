package com.omardev.discordactivity.data.models

enum class DevicePlatform(
    val id: String,
    val title: String,
    val icon: String,
    val osName: String,
    val browserName: String
) {
    VR("vr", "Meta Quest 3 (VR)", "🥽", "QuestOS", "Discord VR"),
    PS5("ps5", "PlayStation 5", "🎮", "PlayStation 5", "PlayStation Network"),
    XBOX("xbox", "Xbox Series X", "🟩", "XboxOS", "Xbox Live"),
    MOBILE("mobile", "Mobile Device", "📱", "Android", "Discord Android"),
    DESKTOP("desktop", "Desktop PC", "💻", "Windows", "Discord Client")
}
