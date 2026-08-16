package com.omardev.discordactivity.data.models

import com.google.gson.annotations.SerializedName

data class DiscordUser(
    @SerializedName("id") val id: String = "",
    @SerializedName("username") val username: String = "",
    @SerializedName("global_name") val globalName: String? = null,
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("discriminator") val discriminator: String = "0",
    @SerializedName("bot") val isBot: Boolean = false,
    val isUserToken: Boolean = false
) {
    val displayName: String
        get() = globalName?.ifBlank { null } ?: username

    val fullTag: String
        get() = if (discriminator != "0" && discriminator.isNotBlank()) "$username#$discriminator" else "@$username"

    val avatarUrl: String
        get() = if (!avatar.isNullOrBlank()) {
            "https://cdn.discordapp.com/avatars/$id/$avatar.png?size=256"
        } else {
            "https://cdn.discordapp.com/embed/avatars/0.png"
        }
}
