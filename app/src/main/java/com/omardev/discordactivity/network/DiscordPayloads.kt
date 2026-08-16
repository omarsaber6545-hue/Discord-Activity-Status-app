package com.omardev.discordactivity.network

import com.google.gson.annotations.SerializedName

object GatewayOpCodes {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val VOICE_STATE_UPDATE = 4
    const val RESUME = 6
    const val RECONNECT = 7
    const val REQUEST_GUILD_MEMBERS = 8
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}

data class GatewayPayload<T>(
    @SerializedName("op") val op: Int,
    @SerializedName("d") val d: T? = null,
    @SerializedName("s") val s: Int? = null,
    @SerializedName("t") val t: String? = null
)

data class HelloData(
    @SerializedName("heartbeat_interval") val heartbeatInterval: Long
)

data class IdentifyProperties(
    @SerializedName("os") val os: String,
    @SerializedName("browser") val browser: String,
    @SerializedName("device") val device: String
)

data class ActivityButton(
    @SerializedName("label") val label: String,
    @SerializedName("url") val url: String
)

data class ActivityAssets(
    @SerializedName("large_image") val largeImage: String? = null,
    @SerializedName("large_text") val largeText: String? = null,
    @SerializedName("small_image") val smallImage: String? = null,
    @SerializedName("small_text") val smallText: String? = null
)

data class ActivityTimestamps(
    @SerializedName("start") val start: Long? = null,
    @SerializedName("end") val end: Long? = null
)

data class ActivityData(
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: Int = 0, // 0 = Playing, 1 = Streaming, 2 = Listening, 3 = Watching, 4 = Custom, 5 = Competing
    @SerializedName("details") val details: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("timestamps") val timestamps: ActivityTimestamps? = null,
    @SerializedName("assets") val assets: ActivityAssets? = null,
    @SerializedName("buttons") val buttons: List<String>? = null,
    @SerializedName("url") val url: String? = null
)

data class PresenceUpdateData(
    @SerializedName("since") val since: Long? = 0,
    @SerializedName("activities") val activities: List<ActivityData>,
    @SerializedName("status") val status: String = "online",
    @SerializedName("afk") val afk: Boolean = false
)

data class IdentifyData(
    @SerializedName("token") val token: String,
    @SerializedName("properties") val properties: IdentifyProperties,
    @SerializedName("presence") val presence: PresenceUpdateData,
    @SerializedName("intents") val intents: Long = 3276799
)

data class VoiceStateUpdateData(
    @SerializedName("guild_id") val guildId: String?,
    @SerializedName("channel_id") val channelId: String?,
    @SerializedName("self_mute") val selfMute: Boolean,
    @SerializedName("self_deaf") val selfDeaf: Boolean
)
