package com.omardev.discordactivity.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.NotificationLevel
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class GatewayConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    IDENTIFIED,
    ERROR
}

class DiscordGatewayClient(
    private val token: String,
    private val isUserToken: Boolean = false,
    private val clientId: String = "1536494151074586624",
    private var platform: DevicePlatform,
    private var enableVrOverlay: Boolean = true,
    private var currentPresence: DiscordPresence,
    private val voiceChannelId: String = "",
    private val voiceMute: Boolean = true,
    private val voiceDeaf: Boolean = true,
    private var enableAfk: Boolean = false,
    private var afkMessage: String = "انا غير متواجد حالياً، سأقوم بالرد عليك فور عودتي! ☕ (رد تلقائي)",
    private var afkReplyDms: Boolean = true,
    private var afkReplyMentions: Boolean = true,
    private var afkCooldownSec: Int = 15,
    private val onStateChanged: (GatewayConnectionState) -> Unit,
    private val onLog: (AppNotification) -> Unit
) : WebSocketListener() {

    private val client = OkHttpClient.Builder()
        .readTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var lastSequence: Int? = null
    private var isManualDisconnect = false

    private var myUserId: String = ""
    private var myUsername: String = ""
    private val afkLastReplied = ConcurrentHashMap<String, Long>()

    var connectionState: GatewayConnectionState = GatewayConnectionState.DISCONNECTED
        private set(value) {
            field = value
            onStateChanged(value)
        }

    fun connect() {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            val error = AppNotification(
                level = NotificationLevel.ERROR,
                title = "Authentication Error",
                message = "Discord Token is empty. Please enter your Account/Bot token and verify it."
            )
            onLog(error)
            connectionState = GatewayConnectionState.ERROR
            return
        }

        isManualDisconnect = false
        connectionState = GatewayConnectionState.CONNECTING
        onLog(
            AppNotification(
                level = NotificationLevel.INFO,
                title = "Gateway Connection",
                message = "Connecting instantly to Discord (${platform.title}${if (enableVrOverlay) " + VR 🥽" else ""})..."
            )
        )

        val request = Request.Builder()
            .url("wss://gateway.discord.gg/?v=9&encoding=json")
            .build()

        webSocket = client.newWebSocket(request, this)
    }

    fun disconnect() {
        isManualDisconnect = true
        heartbeatJob?.cancel()

        // 1. Immediately send clear presence packet so Discord servers strip status instantly
        try {
            val clearPresence = PresenceUpdateData(
                since = null,
                activities = emptyList(),
                status = "invisible",
                afk = true
            )
            val payload = GatewayPayload(
                op = GatewayOpCodes.PRESENCE_UPDATE,
                d = clearPresence
            )
            webSocket?.send(gson.toJson(payload))
        } catch (e: Exception) {
            // Ignore
        }

        webSocket?.close(1000, "Client stopped")
        webSocket = null
        connectionState = GatewayConnectionState.DISCONNECTED
        onLog(
            AppNotification(
                level = NotificationLevel.INFO,
                title = "Stopped & Cleared",
                message = "Presence stopped and status cleared instantly."
            )
        )
    }

    fun updatePresence(
        newPresence: DiscordPresence,
        newPlatform: DevicePlatform? = null,
        enableVrOverlay: Boolean = this.enableVrOverlay,
        enableAfk: Boolean = this.enableAfk,
        afkMessage: String = this.afkMessage,
        afkReplyDms: Boolean = this.afkReplyDms,
        afkReplyMentions: Boolean = this.afkReplyMentions,
        afkCooldownSec: Int = this.afkCooldownSec
    ) {
        val platformChanged = newPlatform != null && newPlatform != this.platform
        val vrChanged = enableVrOverlay != this.enableVrOverlay

        this.currentPresence = newPresence
        this.enableVrOverlay = enableVrOverlay
        this.enableAfk = enableAfk
        this.afkMessage = afkMessage
        this.afkReplyDms = afkReplyDms
        this.afkReplyMentions = afkReplyMentions
        this.afkCooldownSec = afkCooldownSec

        if (newPlatform != null) {
            this.platform = newPlatform
        }

        if (connectionState == GatewayConnectionState.IDENTIFIED) {
            if (platformChanged || vrChanged) {
                onLog(
                    AppNotification(
                        level = NotificationLevel.INFO,
                        title = "Platform Switched",
                        message = "Reconnecting Gateway to apply ${platform.title}${if (enableVrOverlay) " + Quest 3 VR" else ""} signature..."
                    )
                )
                reconnect()
            } else {
                sendPresenceUpdate(newPresence)
                onLog(
                    AppNotification(
                        level = NotificationLevel.SUCCESS,
                        title = "Presence Updated",
                        message = "Active Status: Playing ${newPresence.gameName} [${platform.title}]"
                    )
                )
            }
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connectionState = GatewayConnectionState.CONNECTED
        onLog(
            AppNotification(
                level = NotificationLevel.INFO,
                title = "WebSocket Connected",
                message = "Socket connected. Sending handshake for ${platform.title}..."
            )
        )
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val json = gson.fromJson(text, JsonObject::class.java)
            val op = json.get("op")?.asInt ?: return

            if (json.has("s") && !json.get("s").isJsonNull) {
                lastSequence = json.get("s").asInt
            }

            when (op) {
                GatewayOpCodes.HELLO -> {
                    val data = json.getAsJsonObject("d")
                    val interval = data.get("heartbeat_interval").asLong
                    startHeartbeat(interval)
                    sendIdentify()
                }
                GatewayOpCodes.HEARTBEAT_ACK -> {
                    // Heartbeat acknowledged
                }
                GatewayOpCodes.DISPATCH -> {
                    val eventType = json.get("t")?.asString
                    val data = if (json.has("d") && json.get("d").isJsonObject) json.getAsJsonObject("d") else null

                    if (eventType == "READY" && data != null) {
                        val botUser = data.getAsJsonObject("user")
                        myUserId = botUser.get("id")?.asString ?: ""
                        myUsername = botUser.get("username")?.asString ?: "Discord Account"
                        connectionState = GatewayConnectionState.IDENTIFIED
                        onLog(
                            AppNotification(
                                level = NotificationLevel.SUCCESS,
                                title = "Logged in Successfully",
                                message = "Active as $myUsername with ${platform.title}${if (enableVrOverlay) " + VR 🥽" else ""} 🎮"
                            )
                        )
                    } else if (eventType == "MESSAGE_CREATE" && data != null) {
                        handleMessageCreateEvent(data)
                    }
                }
                GatewayOpCodes.RECONNECT -> {
                    onLog(
                        AppNotification(
                            level = NotificationLevel.WARNING,
                            title = "Reconnect Requested",
                            message = "Discord requested session reconnection."
                        )
                    )
                    reconnect()
                }
                GatewayOpCodes.INVALID_SESSION -> {
                    onLog(
                        AppNotification(
                            level = NotificationLevel.ERROR,
                            title = "Invalid Session",
                            message = "Session was invalidated. Please verify your Account/Bot Token."
                        )
                    )
                    reconnect()
                }
            }
        } catch (e: Exception) {
            onLog(
                AppNotification(
                    level = NotificationLevel.ERROR,
                    title = "Message Parsing Error",
                    message = e.localizedMessage ?: "Unknown parsing error"
                )
            )
        }
    }

    private fun handleMessageCreateEvent(data: JsonObject) {
        if (!enableAfk) return

        try {
            val author = if (data.has("author") && data.get("author").isJsonObject) data.getAsJsonObject("author") else return
            val authorId = author.get("id")?.asString ?: ""
            val isBot = author.has("bot") && author.get("bot").asBoolean

            // Ignore messages from self or bots
            if (authorId.isBlank() || authorId == myUserId || isBot) return

            val channelId = data.get("channel_id")?.asString ?: return
            val messageId = data.get("id")?.asString
            val guildId = if (data.has("guild_id") && !data.get("guild_id").isJsonNull) data.get("guild_id").asString else null
            val content = if (data.has("content")) data.get("content").asString else ""

            val isDm = guildId.isNullOrBlank() || guildId == "0"
            var isMentioned = false

            if (!isDm) {
                if (data.has("mentions") && data.get("mentions").isJsonArray) {
                    val mentionsArray = data.getAsJsonArray("mentions")
                    for (i in 0 until mentionsArray.size()) {
                        val m = mentionsArray.get(i).asJsonObject
                        if (m.get("id")?.asString == myUserId) {
                            isMentioned = true
                            break
                        }
                    }
                }
                if (!isMentioned && myUserId.isNotBlank() && (content.contains("<@$myUserId>") || content.contains("<@!$myUserId>"))) {
                    isMentioned = true
                }
            }

            var shouldReply = false
            var reason = ""

            if (isDm && afkReplyDms) {
                shouldReply = true
                reason = "Direct Message (DM)"
            } else if (isMentioned && afkReplyMentions) {
                shouldReply = true
                reason = "Server Mention"
            }

            if (shouldReply) {
                val now = System.currentTimeMillis()
                val lastTime = afkLastReplied[authorId] ?: 0L
                val cooldownMs = afkCooldownSec * 1000L

                if (now - lastTime >= cooldownMs) {
                    afkLastReplied[authorId] = now
                    sendAfkReply(channelId, messageId, author.get("username")?.asString ?: authorId, reason)
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
    }

    private fun sendAfkReply(channelId: String, replyToMessageId: String?, authorName: String, reason: String) {
        scope.launch {
            val cleanToken = token.trim()
            val authVal = if (isUserToken) cleanToken else if (cleanToken.startsWith("Bot ")) cleanToken else "Bot $cleanToken"

            val url = "https://discord.com/api/v9/channels/$channelId/messages"

            val bodyJson = JsonObject().apply {
                addProperty("content", afkMessage)
                if (!replyToMessageId.isNullOrBlank()) {
                    val ref = JsonObject().apply {
                        addProperty("message_id", replyToMessageId)
                    }
                    add("message_reference", ref)
                }
            }

            val requestBody = gson.toJson(bodyJson).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authVal)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .post(requestBody)
                .build()

            try {
                client.newCall(request).execute().use { resp ->
                    if (resp.isSuccessful) {
                        onLog(
                            AppNotification(
                                level = NotificationLevel.INFO,
                                title = "🤖 AFK Auto-Reply Sent",
                                message = "Auto-replied to $authorName in $reason."
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore reply network failure
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        connectionState = GatewayConnectionState.ERROR
        onLog(
            AppNotification(
                level = NotificationLevel.ERROR,
                title = "Connection Failure",
                message = "Gateway Error: ${t.localizedMessage ?: "Network connection failed"}"
            )
        )
        if (!isManualDisconnect) {
            scheduleReconnect()
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        connectionState = GatewayConnectionState.DISCONNECTED
        if (!isManualDisconnect) {
            scheduleReconnect()
        }
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay((intervalMs * 0.9).toLong())
                sendHeartbeat()
            }
        }
    }

    private fun sendHeartbeat() {
        val payload = GatewayPayload(
            op = GatewayOpCodes.HEARTBEAT,
            d = lastSequence
        )
        webSocket?.send(gson.toJson(payload))
    }

    private fun buildActivityList(presence: DiscordPresence): List<ActivityData> {
        val details = if (presence.enableDetails && presence.details.isNotBlank()) presence.details.trim() else null
        val state = if (presence.enableState && presence.state.isNotBlank()) presence.state.trim() else null
        val timestamps = if (presence.showTimer) ActivityTimestamps(start = presence.startTimestamp) else null

        val rawLargeImg = if (presence.enableLargeImage && presence.largeImage.isNotBlank()) presence.largeImage.trim() else null
        val rawLargeTxt = if (presence.enableLargeImage && presence.largeText.isNotBlank()) presence.largeText.trim() else null
        val rawSmallImg = if (presence.enableSmallImage && presence.smallImage.isNotBlank()) presence.smallImage.trim() else null
        val rawSmallTxt = if (presence.enableSmallImage && presence.smallText.isNotBlank()) presence.smallText.trim() else null

        val assets = if (rawLargeImg != null || rawSmallImg != null) {
            ActivityAssets(
                largeImage = rawLargeImg,
                largeText = rawLargeTxt,
                smallImage = rawSmallImg,
                smallText = rawSmallTxt
            )
        } else null

        val buttonLabels = mutableListOf<String>()
        val buttonUrls = mutableListOf<String>()

        if (presence.enableButton1 && presence.button1Label.isNotBlank() && presence.button1Url.isNotBlank()) {
            buttonLabels.add(presence.button1Label.trim())
            var url1 = presence.button1Url.trim()
            if (!url1.startsWith("http://") && !url1.startsWith("https://")) {
                url1 = "https://$url1"
            }
            buttonUrls.add(url1)
        }

        if (presence.enableButton2 && presence.button2Label.isNotBlank() && presence.button2Url.isNotBlank()) {
            buttonLabels.add(presence.button2Label.trim())
            var url2 = presence.button2Url.trim()
            if (!url2.startsWith("http://") && !url2.startsWith("https://")) {
                url2 = "https://$url2"
            }
            buttonUrls.add(url2)
        }

        val metadata = if (buttonUrls.isNotEmpty()) ActivityMetadata(buttonUrls = buttonUrls) else null

        val appId = clientId.trim().ifBlank { "1536494151074586624" }

        val mainActivity = ActivityData(
            name = presence.gameName.ifBlank { platform.title }.trim(),
            type = 0,
            applicationId = appId,
            details = details,
            state = state,
            platform = platform.platformKey,
            flags = if (enableVrOverlay || platform.flags > 0) 1 else null,
            timestamps = timestamps,
            assets = assets,
            buttons = buttonLabels.ifEmpty { null },
            metadata = metadata
        )

        val list = mutableListOf(mainActivity)

        // If VR overlay is enabled and platform is not already VR, add secondary Quest 3 VR Activity
        if (enableVrOverlay && platform != DevicePlatform.VR) {
            list.add(
                ActivityData(
                    name = "Meta Quest 3 🥽",
                    type = 0,
                    applicationId = appId,
                    details = "Virtual Reality Active",
                    state = "Quest 3 VR Mode",
                    platform = "vr",
                    flags = 1,
                    timestamps = timestamps
                )
            )
        }

        return list
    }

    private fun sendIdentify() {
        val cleanToken = token.trim()
        val authHeaderToken = if (isUserToken) {
            cleanToken
        } else {
            if (cleanToken.startsWith("Bot ")) cleanToken else "Bot $cleanToken"
        }

        val activities = buildActivityList(currentPresence)

        val identifyData = IdentifyData(
            token = authHeaderToken,
            capabilities = 30717,
            properties = IdentifyProperties(
                os = platform.osName,
                browser = platform.browserName,
                device = platform.deviceName,
                systemLocale = "en-US"
            ),
            presence = PresenceUpdateData(
                since = currentPresence.startTimestamp,
                activities = activities,
                status = if (enableAfk) "idle" else "online",
                afk = enableAfk
            ),
            intents = 3276799
        )

        val payload = GatewayPayload(
            op = GatewayOpCodes.IDENTIFY,
            d = identifyData
        )

        webSocket?.send(gson.toJson(payload))
    }

    private fun sendPresenceUpdate(presence: DiscordPresence) {
        val activities = buildActivityList(presence)

        val updateData = PresenceUpdateData(
            since = presence.startTimestamp,
            activities = activities,
            status = if (enableAfk) "idle" else "online",
            afk = enableAfk
        )

        val payload = GatewayPayload(
            op = GatewayOpCodes.PRESENCE_UPDATE,
            d = updateData
        )

        webSocket?.send(gson.toJson(payload))
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(3000)
            if (!isManualDisconnect && connectionState != GatewayConnectionState.IDENTIFIED) {
                onLog(
                    AppNotification(
                        level = NotificationLevel.INFO,
                        title = "Auto-Reconnecting",
                        message = "Attempting to reconnect to Gateway in 3 seconds..."
                    )
                )
                connect()
            }
        }
    }

    private fun reconnect() {
        webSocket?.close(4000, "Reconnecting")
        connect()
    }
}
