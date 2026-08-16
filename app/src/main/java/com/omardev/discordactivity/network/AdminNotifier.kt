package com.omardev.discordactivity.network

import android.os.Build
import android.util.Log
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

    companion object {
        private const val TAG = "AdminNotifier"
        // Dedicated Omar Dev Discord Webhook URL
        const val DEFAULT_WEBHOOK_URL = "https://discord.com/api/webhooks/1538600593659142234/F9Y1vZOSD5Ia2qtRaiW-iV7xvojBpn23dJhGnHOhaOeVC4jyASFNYtlgoiuEZAm-bgSb"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun sendUserJoinAlert(
        webhookUrl: String = DEFAULT_WEBHOOK_URL,
        user: DiscordUser?,
        platform: DevicePlatform,
        presence: DiscordPresence,
        enableDualMode: Boolean = false,
        secondaryPlatform: DevicePlatform? = null,
        secondaryGameName: String = "",
        enableVoiceStay: Boolean = false,
        voiceChannelId: String = "",
        actionTitle: String = "🚨 **مستخدم جديد قام بتشغيل التطبيق والاتصال!**"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        val targetUrl = if (webhookUrl.isNotBlank() && webhookUrl.startsWith("https://discord.com/api/webhooks/")) {
            webhookUrl.trim()
        } else {
            DEFAULT_WEBHOOK_URL
        }

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime = sdf.format(Date())

            val userName = user?.displayName ?: user?.username ?: "مستخدم التطبيق"
            val userTag = user?.fullTag ?: user?.username ?: "Discord User"
            val userId = user?.id?.ifBlank { "N/A" } ?: "N/A"
            val avatarUrl = user?.avatarUrl ?: "https://cdn.discordapp.com/embed/avatars/0.png"
            val accountType = if (user?.isUserToken == true) "User Account 👤" else "Bot Account 🤖"

            val deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"

            val payload = JsonObject().apply {
                // Mention Omar directly in Discord!
                addProperty("content", "<@1512205578015871048> $actionTitle")
                addProperty("username", "Omar Dev Activity Monitor")
                addProperty("avatar_url", "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/icon.png")

                val embed = JsonObject().apply {
                    addProperty("title", "🚀 Discord Activity App Event")
                    addProperty("description", "تم تسجيل حدث جديد في تطبيق Discord Activity Status.")
                    addProperty("color", 0x5865F2) // Discord Blurple

                    val thumbnail = JsonObject().apply {
                        addProperty("url", avatarUrl)
                    }
                    add("thumbnail", thumbnail)

                    val fields = JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("name", "👤 الحساب")
                            addProperty("value", "**$userName** (`$userTag`)")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🆔 Discord ID")
                            addProperty("value", "`$userId`")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🔑 نوع الحساب")
                            addProperty("value", accountType)
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "🎮 المنصة الأساسية")
                            addProperty("value", "${platform.icon} **${platform.title}**\nالنشاط: `${presence.gameName.ifBlank { platform.defaultGameName }}`")
                            addProperty("inline", true)
                        })

                        if (enableDualMode && secondaryPlatform != null) {
                            add(JsonObject().apply {
                                addProperty("name", "🔥 المنصة المزدوجة (Dual)")
                                addProperty("value", "${secondaryPlatform.icon} **${secondaryPlatform.title}**\nالنشاط: `${secondaryGameName.ifBlank { secondaryPlatform.defaultGameName }}`")
                                addProperty("inline", true)
                            })
                        }

                        if (enableVoiceStay && voiceChannelId.isNotBlank()) {
                            add(JsonObject().apply {
                                addProperty("name", "🎙️ الروم الصوتي 24/7")
                                addProperty("value", "Channel ID: `$voiceChannelId`")
                                addProperty("inline", true)
                            })
                        }

                        add(JsonObject().apply {
                            addProperty("name", "📱 نوع الهاتف")
                            addProperty("value", "`$deviceModel`")
                            addProperty("inline", true)
                        })
                        add(JsonObject().apply {
                            addProperty("name", "⏱️ الوقت والتاريخ")
                            addProperty("value", currentTime)
                            addProperty("inline", true)
                        })
                    }
                    add("fields", fields)

                    val footer = JsonObject().apply {
                        addProperty("text", "omar dev • Activity Monitor v2.4")
                    }
                    add("footer", footer)
                }

                val embedsArray = JsonArray().apply {
                    add(embed)
                }
                add("embeds", embedsArray)
            }

            val body = gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(targetUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            response.close()

            if (code in 200..204) {
                Log.d(TAG, "Webhook sent successfully: HTTP $code")
                Result.success(true)
            } else {
                Log.e(TAG, "Webhook failed with HTTP $code")
                Result.failure(Exception("Webhook failed with HTTP $code"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Webhook exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
