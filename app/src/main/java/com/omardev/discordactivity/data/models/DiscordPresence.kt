package com.omardev.discordactivity.data.models

import com.google.gson.annotations.SerializedName

data class DiscordPresence(
    @SerializedName("game_name") val gameName: String = "omar dev",
    @SerializedName("details") val details: String = "Developing awesome apps 🚀",
    @SerializedName("state") val state: String = "In Match (Score: 12 - 10)",
    @SerializedName("large_image") val largeImage: String = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/icon.png",
    @SerializedName("large_text") val largeText: String = "Omar Dev - Verified",
    @SerializedName("small_image") val smallImage: String = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/badge.png",
    @SerializedName("small_text") val smallText: String = "Pro Level",
    @SerializedName("show_timer") val showTimer: Boolean = true,
    @SerializedName("button1_label") val button1Label: String = "Omar Dev Site",
    @SerializedName("button1_url") val button1Url: String = "https://omar-dev.site",
    @SerializedName("button2_label") val button2Label: String = "GitHub: Omar-Dev",
    @SerializedName("button2_url") val button2Url: String = "https://github.com/omarsaber6545-hue",
    @SerializedName("start_timestamp") val startTimestamp: Long = System.currentTimeMillis()
)
