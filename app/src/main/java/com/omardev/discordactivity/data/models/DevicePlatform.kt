package com.omardev.discordactivity.data.models

enum class DevicePlatform(
    val id: String,
    val title: String,
    val icon: String,
    val osName: String,
    val browserName: String,
    val deviceName: String,
    val platformKey: String,
    val flags: Int = 0
) {
    VR("vr", "Meta Quest 3 (VR)", "🥽", "Android", "Discord VR", "Quest 3", "vr", 1),
    PS5("ps5", "PlayStation 5", "🎮", "PS5", "Discord Client", "PlayStation 5", "ps5", 0),
    XBOX("xbox", "Xbox Series X", "🟩", "Xbox", "Discord Client", "Xbox Series X", "xbox", 0),
    MOBILE("mobile", "Mobile Phone", "📱", "Android", "Discord Android", "Samsung Galaxy S24", "mobile", 0),
    DESKTOP("desktop", "Desktop PC", "💻", "Windows", "Discord Client", "Desktop PC", "desktop", 0)
}
