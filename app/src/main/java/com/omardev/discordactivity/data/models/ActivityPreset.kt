package com.omardev.discordactivity.data.models

data class ActivityPreset(
    val name: String,
    val icon: String,
    val presence: DiscordPresence
) {
    companion object {
        fun getDefaultPresets(): List<ActivityPreset> {
            return listOf(
                ActivityPreset(
                    name = "VS Code",
                    icon = "💻",
                    presence = DiscordPresence(
                        gameName = "Visual Studio Code",
                        details = "Writing Kotlin & Compose Code 💻",
                        state = "Developing Android Apps 🚀",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/vscode.png",
                        largeText = "Omar Dev - Coding",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/kotlin.png",
                        smallText = "Kotlin 1.9.23",
                        showTimer = true,
                        button1Label = "GitHub: Omar-Dev",
                        button1Url = "https://github.com/omarsaber6545-hue",
                        button2Label = "Omar Dev Site",
                        button2Url = "https://omar-dev.site"
                    )
                ),
                ActivityPreset(
                    name = "Valorant",
                    icon = "🎯",
                    presence = DiscordPresence(
                        gameName = "Valorant",
                        details = "Playing Competitive Match 🎯",
                        state = "In Match (Score: 12 - 10)",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/valorant.png",
                        largeText = "Valorant Ranked",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "Ascendant Rank",
                        showTimer = true,
                        button1Label = "Join Discord",
                        button1Url = "https://discord.gg",
                        button2Label = "Twitch Stream",
                        button2Url = "https://twitch.tv"
                    )
                ),
                ActivityPreset(
                    name = "Minecraft",
                    icon = "⛏️",
                    presence = DiscordPresence(
                        gameName = "Minecraft",
                        details = "Building Megastructure ⛏️",
                        state = "Survival World (Hardcore)",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/minecraft.png",
                        largeText = "Minecraft 1.20",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "Level 64",
                        showTimer = true,
                        button1Label = "Server Website",
                        button1Url = "https://omar-dev.site",
                        button2Label = "",
                        button2Url = ""
                    )
                ),
                ActivityPreset(
                    name = "GTA V",
                    icon = "🏎️",
                    presence = DiscordPresence(
                        gameName = "Grand Theft Auto V",
                        details = "GTA Online Heist 🏎️",
                        state = "In a Full Lobby (28/30)",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/gta5.png",
                        largeText = "GTA V Online",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "Rank 250",
                        showTimer = true,
                        button1Label = "Social Club",
                        button1Url = "https://socialclub.rockstargames.com",
                        button2Label = "",
                        button2Url = ""
                    )
                ),
                ActivityPreset(
                    name = "Counter-Strike 2",
                    icon = "🔫",
                    presence = DiscordPresence(
                        gameName = "Counter-Strike 2",
                        details = "Premier Match - Mirage 🔫",
                        state = "Match Point (Score: 12 - 11)",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/cs2.png",
                        largeText = "Counter-Strike 2",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "18,500 ELO",
                        showTimer = true,
                        button1Label = "Steam Profile",
                        button1Url = "https://steamcommunity.com",
                        button2Label = "",
                        button2Url = ""
                    )
                ),
                ActivityPreset(
                    name = "League of Legends",
                    icon = "⚔️",
                    presence = DiscordPresence(
                        gameName = "League of Legends",
                        details = "Summoner's Rift (Ranked) ⚔️",
                        state = "Playing Mid Lane (5/1/8)",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/lol.png",
                        largeText = "League of Legends",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "Diamond Tier",
                        showTimer = true,
                        button1Label = "OP.GG Profile",
                        button1Url = "https://op.gg",
                        button2Label = "",
                        button2Url = ""
                    )
                ),
                ActivityPreset(
                    name = "Roblox",
                    icon = "🧱",
                    presence = DiscordPresence(
                        gameName = "Roblox",
                        details = "Playing Custom Games 🧱",
                        state = "In VIP Server with friends",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/roblox.png",
                        largeText = "Roblox Studio",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/fire.png",
                        smallText = "Level 100",
                        showTimer = true,
                        button1Label = "Roblox Profile",
                        button1Url = "https://roblox.com",
                        button2Label = "",
                        button2Url = ""
                    )
                ),
                ActivityPreset(
                    name = "AFK / Break",
                    icon = "☕",
                    presence = DiscordPresence(
                        gameName = "AFK Mode",
                        details = "Away From Keyboard ☕",
                        state = "Taking a coffee break...",
                        largeImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/afk.png",
                        largeText = "AFK",
                        smallImage = "https://raw.githubusercontent.com/omarsaber6545-hue/assets/main/coffee.png",
                        smallText = "Back soon!",
                        showTimer = false,
                        button1Label = "",
                        button1Url = "",
                        button2Label = "",
                        button2Url = ""
                    )
                )
            )
        }
    }
}
