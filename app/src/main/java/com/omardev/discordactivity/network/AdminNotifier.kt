package com.omardev.discordactivity.network

import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.omardev.discordactivity.data.models.DevicePlatform
import com.omardev.discordactivity.data.models.DiscordPresence
import com.omardev.discordactivity.data.models.DiscordUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdminNotifier {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun sendUserJoinAlert(
        webhookUrl: String,
        user: DiscordUser?,
        platform: DevicePlatform,
        presence: DiscordPresence,
        enableDualMode: Boolean = false,
        secondaryPlatform: DevicePlatform? = null,
        secondaryGameName: String = "",
        enableVoiceStay: Boolean = false,
        voiceChannelId: String = ""
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("https://discord.com/api/webhooks/")) {
            return@withContext Result.failure(Exception("Invalid or empty Discord Webhook URL."))
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = sdf.format(Date())

            val userName = user?.displayName ?: "Anonymous User"
            val userTag = user?.fullTag ?: "Unknown"
            val userId = user?.id?.ifBlank { "N/A" } ?: "N/A"
            val avatarUrl = user?.avatarUrl ?: "https://cdn.discordapp.com/embed/avatars/0.png"
            val accountType = if (user?.isUserToken == true) "User Account 👤" else "Bot Account 🤖"

            val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

            val payload = JsonObject().apply {
                // Mention Omar directly in Discord!
                addProperty("content", "<@1512205578015871048> 🚨 **مستخدم جديد قام بتشغيل التطبيق والاتصال!**")
                addProperty("username", "Omar Dev Admin Monitor")
                addProperty("avatar_url", "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/icon.png")

                val embed = JsonObject().apply {
                    addProperty("title", "🚀 New User Session Active!")
                    addProperty("description", "A user has activated the Discord Activity Status app.")
                    addProperty("color", 0x5865F2) // Discord Blurple

                    val thumbnail = JsonObject().apply {
                        addProperty("url", avatarUrl)
                    }
                    add("thumbnail", thumbnail)

                    val fields = JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("name", "👤 Account")
                            addProperty("value", "**$userName** (`$userTag`)")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🆔 Discord ID")
                            addProperty("value", "`$userId`")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🔑 Account Type")
                            addProperty("value", accountType)
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🎮 Primary Platform")
                            addProperty("value", "${platform.icon} **${platform.title}**\nGame: `${presence.gameName.ifBlank { platform.defaultGameName }}`")
                            addProperty("inline", true)
                        })

                        if (enableDualMode && secondaryPlatform != null) {
                            add(JsonObject().apply {
                                addProperty("name", "🔥 Dual Companion Platform")
                                addProperty("value", "${secondaryPlatform.icon} **${secondaryPlatform.title}**\nGame: `${secondaryGameName.ifBlank { secondaryPlatform.defaultGameName }}`")
                                addProperty("inline", true)
                            })
                        }

                        if (enableVoiceStay && voiceChannelId.isNotBlank()) {
                            add(JsonObject().apply {
                                addProperty("name", "🎙️ 24/7 Voice Channel")
                                addProperty("value", "Channel ID: `$voiceChannelId`")
                                addProperty("inline", true)
                            })
                        }

                        add(JsonObject().apply {
                            addProperty("name", "📱 Physical Phone")
                            addProperty("value", "`$deviceModel`")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "⏱️ Time Active")
                            addProperty("value", currentTime)
                            addProperty("inline", true)
                        })
                    }
                    add("fields", fields)

                    val footer = JsonObject().apply {
                        addProperty("text", "omar dev • Discord Activity System v2.4")
                    }
                    add("footer", footer)
                }

                val embedsArray = JsonArray().apply {
                    add(embed)
                }
                add("embeds", embedsArray)
            }

            val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful || response.code in 200..204
            response.close()

            if (isSuccess) {
                Result.success(true)
            } else {
                Result.failure(Exception("Webhook failed with HTTP ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testWebhook(webhookUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val cleanUrl = webhookUrl.trim()
        if (cleanUrl.isBlank() || !cleanUrl.startsWith("https://discord.com/api/webhooks/")) {
            return@withContext Result.failure(Exception("Invalid or empty Discord Webhook URL."))
        }

        try {
            val payload = JsonObject().apply {
                addProperty("content", "<@1512205578015871048> ✅ **Webhook Test Connected Successfully!**")
                addProperty("username", "Omar Dev System")

                val embed = JsonObject().apply {
                    addProperty("title", "🔔 Webhook Verification Success")
                    addProperty("description", "Discord Activity Status app is now successfully linked to your Discord channel!")
                    addProperty("color", 0x57F287) // Green
                }

                val embeds = JsonArray().apply { add(embed) }
                add("embeds", embeds)
            }

            val body = gson.toJson(payload).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(cleanUrl)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val isSuccess = response.isSuccessful || response.code in 200..204
            response.close()

            if (isSuccess) Result.success(true) else Result.failure(Exception("HTTP ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
