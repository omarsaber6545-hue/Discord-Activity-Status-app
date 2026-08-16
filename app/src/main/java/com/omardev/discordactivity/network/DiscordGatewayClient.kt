package com.omardev.discordactivity.network

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.omardev.discordactivity.data.models.AppNotification
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.NotificationLevel
import kotlinx.coroutines.*
import okhttp3.*
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
    private var platform: DevicePlatform,
    private var currentPresence: DiscordPresence,
    private val voiceChannelId: String = "",
    private val voiceMute: Boolean = true,
    private val voiceDeaf: Boolean = true,
    private val onStateChanged: (GatewayConnectionState) -> Unit,
    private val onLog: (AppNotification) -> Unit
) : WebSocketListener() {

    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var lastSequence: Int? = null
    private var isManualDisconnect = false

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
                message = "Connecting to Discord Gateway v9/v10 (${platform.title})..."
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
        webSocket?.close(1000, "Client closed")
        webSocket = null
        connectionState = GatewayConnectionState.DISCONNECTED
        onLog(
            AppNotification(
                level = NotificationLevel.INFO,
                title = "Disconnected",
                message = "Gateway disconnected safely."
            )
        )
    }

    fun updatePresence(newPresence: DiscordPresence, newPlatform: DevicePlatform? = null) {
        this.currentPresence = newPresence
        if (newPlatform != null) {
            this.platform = newPlatform
        }
        if (connectionState == GatewayConnectionState.IDENTIFIED) {
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

    override fun onOpen(webSocket: WebSocket, response: Response) {
        connectionState = GatewayConnectionState.CONNECTED
        onLog(
            AppNotification(
                level = NotificationLevel.INFO,
                title = "WebSocket Connected",
                message = "Socket connection established with platform signature: ${platform.title}."
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
                    if (eventType == "READY") {
                        val botUser = json.getAsJsonObject("d").getAsJsonObject("user")
                        val username = botUser.get("username")?.asString ?: "Discord Account"
                        connectionState = GatewayConnectionState.IDENTIFIED
                        onLog(
                            AppNotification(
                                level = NotificationLevel.SUCCESS,
                                title = "Logged in Successfully",
                                message = "Active as $username with spoofing: ${platform.title} 🎮"
                            )
                        )
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

    private fun buildActivityData(presence: DiscordPresence): ActivityData {
        val details = if (presence.enableDetails && presence.details.isNotBlank()) presence.details else null
        val state = if (presence.enableState && presence.state.isNotBlank()) presence.state else null
        val timestamps = if (presence.showTimer) ActivityTimestamps(start = presence.startTimestamp) else null

        val largeImg = if (presence.enableLargeImage && presence.largeImage.isNotBlank()) presence.largeImage else null
        val largeTxt = if (presence.enableLargeImage && presence.largeText.isNotBlank()) presence.largeText else null
        val smallImg = if (presence.enableSmallImage && presence.smallImage.isNotBlank()) presence.smallImage else null
        val smallTxt = if (presence.enableSmallImage && presence.smallText.isNotBlank()) presence.smallText else null

        val assets = if (largeImg != null || smallImg != null) {
            ActivityAssets(
                largeImage = largeImg,
                largeText = largeTxt,
                smallImage = smallImg,
                smallText = smallTxt
            )
        } else null

        val buttonLabels = mutableListOf<String>()
        if (presence.enableButton1 && presence.button1Label.isNotBlank()) {
            buttonLabels.add(presence.button1Label)
        }
        if (presence.enableButton2 && presence.button2Label.isNotBlank()) {
            buttonLabels.add(presence.button2Label)
        }

        return ActivityData(
            name = presence.gameName.ifBlank { platform.title },
            type = 0,
            details = details,
            state = state,
            platform = platform.platformKey,
            flags = if (platform.flags > 0) platform.flags else null,
            timestamps = timestamps,
            assets = assets,
            buttons = buttonLabels.ifEmpty { null }
        )
    }

    private fun sendIdentify() {
        val cleanToken = token.trim()
        val authHeaderToken = if (isUserToken) {
            cleanToken
        } else {
            if (cleanToken.startsWith("Bot ")) cleanToken else "Bot $cleanToken"
        }

        val activity = buildActivityData(currentPresence)

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
                activities = listOf(activity),
                status = "online",
                afk = false
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
        val activity = buildActivityData(presence)

        val updateData = PresenceUpdateData(
            since = presence.startTimestamp,
            activities = listOf(activity),
            status = "online",
            afk = false
        )

        val payload = GatewayPayload(
            op = GatewayOpCodes.PRESENCE_UPDATE,
            d = updateData
        )

        webSocket?.send(gson.toJson(payload))
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5000)
            if (!isManualDisconnect && connectionState != GatewayConnectionState.IDENTIFIED) {
                onLog(
                    AppNotification(
                        level = NotificationLevel.INFO,
                        title = "Auto-Reconnecting",
                        message = "Attempting to reconnect to Gateway in 5 seconds..."
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
