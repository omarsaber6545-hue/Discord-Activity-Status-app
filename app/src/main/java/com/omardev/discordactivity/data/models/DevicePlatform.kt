package com.omardev.discordactivity.data.models

enum class DevicePlatform(
    val id: String,
    val title: String,
    val icon: String,
    val osName: String,
    val browserName: String,
    val deviceName: String,
    val platformKey: String,
    val defaultGameName: String,
    val defaultDetails: String,
    val defaultState: String,
    val flags: Int = 0
) {
    PS5("ps5", "PlayStation 5", "🎮", "PS5", "Discord Client", "PlayStation 5", "ps5", "PlayStation 5 🎮", "Playing on PlayStation 5", "PlayStation Network Active", 0),
    XBOX("xbox", "Xbox Series X", "🟩", "Xbox", "Discord Client", "Xbox Series X", "xbox", "Xbox Network 🟩", "Playing on Xbox Series X", "Xbox Live Active", 0),
    VR("vr", "Meta Quest 3 (VR)", "🥽", "Android", "Discord VR", "Quest 3", "vr", "Virtual Reality VR 🥽", "Playing in VR", "Meta Quest 3 Active", 1),
    MOBILE("mobile", "Mobile Phone", "📱", "Android", "Discord Android", "Samsung Galaxy S24", "mobile", "Discord for Mobile 📱", "Mobile Active", "Mobile Online", 0),
    DESKTOP("desktop", "Desktop PC", "💻", "Windows", "Discord Client", "Desktop PC", "desktop", "Visual Studio Code 💻", "Developing on PC", "Online", 0)
}
