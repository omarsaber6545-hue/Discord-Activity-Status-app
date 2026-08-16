package com.omardev.discordactivity.data.models

import com.google.gson.annotations.SerializedName

data class DiscordPresence(
    @SerializedName("game_name") val gameName: String = "Visual Studio Code",
    
    @SerializedName("enable_details") val enableDetails: Boolean = true,
    @SerializedName("details") val details: String = "Developing awesome apps 🚀",
    
    @SerializedName("enable_state") val enableState: Boolean = true,
    @SerializedName("state") val state: String = "In Match (Score: 12 - 10)",
    
    @SerializedName("enable_large_image") val enableLargeImage: Boolean = true,
    @SerializedName("large_image") val largeImage: String = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/vscode.png",
    @SerializedName("large_text") val largeText: String = "Omar Dev - Coding",
    
    @SerializedName("enable_small_image") val enableSmallImage: Boolean = true,
    @SerializedName("small_image") val smallImage: String = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/kotlin.png",
    @SerializedName("small_text") val smallText: String = "Kotlin 1.9.23",
    
    @SerializedName("show_timer") val showTimer: Boolean = true,
    
    @SerializedName("enable_button1") val enableButton1: Boolean = true,
    @SerializedName("button1_label") val button1Label: String = "Omar Dev Site",
    @SerializedName("button1_url") val button1Url: String = "https://omar-dev.site",
    
    @SerializedName("enable_button2") val enableButton2: Boolean = true,
    @SerializedName("button2_label") val button2Label: String = "GitHub: Omar-Dev",
    @SerializedName("button2_url") val button2Url: String = "https://github.com/omarsaber6545-hue",
    
    @SerializedName("start_timestamp") val startTimestamp: Long = System.currentTimeMillis()
)
