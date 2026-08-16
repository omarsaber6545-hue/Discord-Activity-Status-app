package com.omardev.discordactivity.network

import com.google.gson.Gson
import com.omardev.discordactivity.data.models.DiscordUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DiscordApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun verifyToken(token: String): Result<DiscordUser> = withContext(Dispatchers.IO) {
        val cleanToken = token.trim()
        if (cleanToken.isBlank()) {
            return@withContext Result.failure(Exception("Token is empty! Please enter your Token first."))
        }

        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        // 1. Try as User Token
        try {
            val userRequest = Request.Builder()
                .url("https://discord.com/api/v10/users/@me")
                .header("Authorization", cleanToken)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(userRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val user = gson.fromJson(body, DiscordUser::class.java).copy(isUserToken = true)
                        return@withContext Result.success(user)
                    }
                } else if (response.code != 401) {
                    return@withContext Result.failure(Exception("Discord API returned error code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            // Fall through to bot check
        }

        // 2. Try as Bot Token
        try {
            val botToken = if (cleanToken.startsWith("Bot ")) cleanToken else "Bot $cleanToken"
            val botRequest = Request.Builder()
                .url("https://discord.com/api/v10/users/@me")
                .header("Authorization", botToken)
                .header("User-Agent", userAgent)
                .build()

            client.newCall(botRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val botUser = gson.fromJson(body, DiscordUser::class.java).copy(isUserToken = false)
                        return@withContext Result.success(botUser)
                    }
                } else {
                    return@withContext Result.failure(Exception("Invalid Token (401 Unauthorized)! Please check your Token."))
                }
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Connection failed: ${e.localizedMessage}"))
        }

        Result.failure(Exception("Invalid Token! Please check your token and try again."))
    }
}
