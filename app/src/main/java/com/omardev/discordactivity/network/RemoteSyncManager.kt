package com.omardev.discordactivity.network

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.omardev.discordactivity.App
import com.omardev.discordactivity.MainActivity
import com.omardev.discordactivity.R
import com.omardev.discordactivity.data.models.AppAnnouncement
import com.omardev.discordactivity.data.models.AnnouncementType
import com.omardev.discordactivity.data.preferences.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class RemoteSyncManager(private val context: Context) {

    private val prefs = PreferencesManager(context)
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun triggerSystemNotification(announcement: AppAnnouncement) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_announcement", true)
            putExtra("announcement_id", announcement.id)
            putExtra("announcement_title", announcement.title)
            putExtra("announcement_message", announcement.message)
            putExtra("announcement_author", announcement.author)
            putExtra("announcement_timestamp", announcement.timestamp)
            putExtra("announcement_type", announcement.type.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            announcement.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, App.CHANNEL_ANNOUNCEMENTS_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📢 ${announcement.title}")
            .setContentText(announcement.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(announcement.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(announcement.id.hashCode(), builder.build())
    }

    suspend fun checkRemoteSync(syncChannelIdOrUrl: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val target = syncChannelIdOrUrl.trim()
        if (target.isBlank()) {
            return@withContext Result.success(false)
        }

        try {
            val url = if (target.startsWith("http")) target else "https://discord.com/api/v9/channels/$target/messages?limit=10"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("Sync HTTP ${resp.code}"))
                val body = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))

                val jsonArray = try {
                    gson.fromJson(body, JsonArray::class.java)
                } catch (e: Exception) {
                    null
                } ?: return@withContext Result.success(false)

                val myUserId = prefs.verifiedUser?.id?.trim() ?: ""

                for (i in 0 until jsonArray.size()) {
                    val msgObj = jsonArray.get(i).asJsonObject
                    val content = if (msgObj.has("content")) msgObj.get("content").asString else ""

                    // 1. Check for remote Ban / Wipe command: {"action":"ban","user_id":"123","reason":"..."}
                    if (content.contains("\"action\":\"ban\"") || content.contains("\"action\":\"wipe\"")) {
                        try {
                            val cmd = gson.fromJson(content, JsonObject::class.java)
                            val action = cmd.get("action")?.asString
                            val targetId = cmd.get("user_id")?.asString?.trim()

                            if (myUserId.isNotBlank() && targetId == myUserId) {
                                if (action == "ban") {
                                    val reason = cmd.get("reason")?.asString ?: "تم حظرك من التطبيق بواسطة الإدارة 🚫"
                                    prefs.isBanned = true
                                    prefs.banReason = reason
                                } else if (action == "wipe") {
                                    prefs.wipeUserData()
                                }
                            }
                        } catch (e: Exception) {}
                    }

                    // 2. Check for remote broadcast announcement: {"action":"broadcast","id":"...","title":"...","message":"..."}
                    if (content.contains("\"action\":\"broadcast\"")) {
                        try {
                            val cmd = gson.fromJson(content, JsonObject::class.java)
                            val id = cmd.get("id")?.asString ?: msgObj.get("id")?.asString ?: System.currentTimeMillis().toString()
                            val title = cmd.get("title")?.asString ?: "تنبيه هام من المطور Omar Dev"
                            val message = cmd.get("message")?.asString ?: ""
                            val author = cmd.get("author")?.asString ?: "Omar Dev (Owner)"
                            val targetUser = if (cmd.has("target_user_id")) cmd.get("target_user_id").asString else null

                            // Check if this broadcast is for everyone OR specifically for me
                            if (targetUser == null || targetUser.isBlank() || targetUser == myUserId) {
                                if (prefs.lastReadAnnouncementId != id) {
                                    val announcement = AppAnnouncement(
                                        id = id,
                                        title = title,
                                        message = message,
                                        author = author,
                                        type = AnnouncementType.UPDATE,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    prefs.activeAnnouncement = announcement
                                    triggerSystemNotification(announcement)
                                }
                            }
                        } catch (e: Exception) {}
                    }
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
