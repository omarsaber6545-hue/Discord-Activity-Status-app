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
    private val platform: DevicePlatform,
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
        if (token.isBlank()) {
            val error = AppNotification(
                level = NotificationLevel.ERROR,
                title = "Authentication Error",
                message = "Discord Bot Token is empty. Please enter your bot token in Settings."
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
                message = "Connecting to Discord Gateway v10 (${platform.title})..."
            )
        )

        val request = Request.Builder()
            .url("wss://gateway.discord.gg/?v=10&encoding=json")
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

    fun updatePresence(newPresence: DiscordPresence) {
        this.currentPresence = newPresence
        if (connectionState == GatewayConnectionState.IDENTIFIED) {
            sendPresenceUpdate(newPresence)
            onLog(
                AppNotification(
                    level = NotificationLevel.SUCCESS,
                    title = "Presence Updated",
                    message = "Active Status: Playing ${newPresence.gameName}"
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
                message = "Socket connection established. Waiting for Hello (OP 10)..."
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
                    // Heartbeat acknowledged by Discord
                }
                GatewayOpCodes.DISPATCH -> {
                    val eventType = json.get("t")?.asString
                    if (eventType == "READY") {
                        val botUser = json.getAsJsonObject("d").getAsJsonObject("user")
                        val username = botUser.get("username")?.asString ?: "Discord Bot"
                        connectionState = GatewayConnectionState.IDENTIFIED
                        onLog(
                            AppNotification(
                                level = NotificationLevel.SUCCESS,
                                title = "Identified Successfully",
                                message = "Logged in as $username with spoofing: ${platform.title} 🎮"
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
                            message = "Session was invalidated. Please verify your Bot Token and Intents."
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

    private fun sendIdentify() {
        val cleanToken = if (token.startsWith("Bot ")) token else "Bot $token"

        val activity = ActivityData(
            name = currentPresence.gameName,
            type = 0,
            details = currentPresence.details.ifBlank { null },
            state = currentPresence.state.ifBlank { null },
            timestamps = if (currentPresence.showTimer) ActivityTimestamps(start = currentPresence.startTimestamp) else null,
            assets = ActivityAssets(
                largeImage = currentPresence.largeImage.ifBlank { null },
                largeText = currentPresence.largeText.ifBlank { null },
                smallImage = currentPresence.smallImage.ifBlank { null },
                smallText = currentPresence.smallText.ifBlank { null }
            ),
            buttons = listOfNotNull(
                currentPresence.button1Label.ifBlank { null },
                currentPresence.button2Label.ifBlank { null }
            ).ifEmpty { null }
        )

        val identifyData = IdentifyData(
            token = cleanToken,
            properties = IdentifyProperties(
                os = platform.osName,
                browser = platform.browserName,
                device = platform.title
            ),
            presence = PresenceUpdateData(
                since = 0,
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
        val activity = ActivityData(
            name = presence.gameName,
            type = 0,
            details = presence.details.ifBlank { null },
            state = presence.state.ifBlank { null },
            timestamps = if (presence.showTimer) ActivityTimestamps(start = presence.startTimestamp) else null,
            assets = ActivityAssets(
                largeImage = presence.largeImage.ifBlank { null },
                largeText = presence.largeText.ifBlank { null },
                smallImage = presence.smallImage.ifBlank { null },
                smallText = presence.smallText.ifBlank { null }
            ),
            buttons = listOfNotNull(
                presence.button1Label.ifBlank { null },
                presence.button2Label.ifBlank { null }
            ).ifEmpty { null }
        )

        val updateData = PresenceUpdateData(
            since = 0,
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
