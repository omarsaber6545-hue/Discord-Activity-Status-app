import random
from typing import Dict, Any, List

RANDOM_GAMES_DATABASE: List[Dict[str, Any]] = [
    {
        "game_name": "Valorant",
        "details_pool": [
            "Competitive Match (Ascendant II) 🔥",
            "Ranked Grind to Radiant 🏆",
            "Unrated with the Squad 🎮",
            "Deathmatch Warmup (Headshots Only) 🎯"
        ],
        "state_pool": [
            "Playing Jett on Ascent (11-9) ⚡",
            "Reyna Carry Mode Active 👑",
            "Clutching 1v3 on Haven 💣",
            "Omen Smoke & Teleport Plays 🌌"
        ],
        "large_image": "valorant",
        "large_text": "Valorant - Riot Games",
        "small_image": "verified",
        "small_text": "Ascendant Rank",
        "theme": {
            "name": "Crimson Strike (أحمر ناري)",
            "accent": "#ff4655",
            "icon": "🎯"
        }
    },
    {
        "game_name": "Cyberpunk 2077",
        "details_pool": [
            "Exploring Night City 🌃",
            "Phantom Liberty DLC Campaign 🕶️",
            "Netrunner Stealth Infiltration 💾",
            "Cruising in Quadra Turbo-R 🚗"
        ],
        "state_pool": [
            "Level 50 Sandevistan Build ⚡",
            "Overriding Arasaka Mainframe 💻",
            "Hanging out with Johnny Silverhand 🎸",
            "Completing High-Threat Gig 💰"
        ],
        "large_image": "cyberpunk",
        "large_text": "Cyberpunk 2077 - Night City",
        "small_image": "verified",
        "small_text": "Street Cred 50",
        "theme": {
            "name": "Neon Night City (سيان ونيون)",
            "accent": "#00f0ff",
            "icon": "⚡"
        }
    },
    {
        "game_name": "Minecraft",
        "details_pool": [
            "Building Mega Medieval Castle 🏰",
            "Hardcore Survival World (Day 520) 💀",
            "Automated Redstone Megafarm ⚙️",
            "Nether Fortress & Bastion Raid 🌋"
        ],
        "state_pool": [
            "Full Netherite Armor & Enchantments 🛡️",
            "Mining for Ancient Debris in Nether ⛏️",
            "Defeating the Ender Dragon 🐉",
            "Exploring Deep Dark Ancient City 🌌"
        ],
        "large_image": "minecraft",
        "large_text": "Minecraft - Java Edition",
        "small_image": "verified",
        "small_text": "Hardcore Survior",
        "theme": {
            "name": "Emerald Wilds (أخضر إميرالد)",
            "accent": "#2ecc71",
            "icon": "⛏️"
        }
    },
    {
        "game_name": "Grand Theft Auto V",
        "details_pool": [
            "Los Santos Diamond Casino Heist 💎",
            "GTA Online - Cayo Perico Grind 🏝️",
            "High-Speed Freeway Chase 🚓",
            "Custom Car Meet at LS Car Meet 🏎️"
        ],
        "state_pool": [
            "Cruising in Grotti Turismo Omaggio 🚗",
            "Managing Nightclub & Bunker Business 💰",
            "5 Stars Wanted Level Survival ⭐⭐⭐⭐⭐",
            "Flying Oppressor Mk II across map 🚀"
        ],
        "large_image": "gta5",
        "large_text": "Grand Theft Auto Online",
        "small_image": "verified",
        "small_text": "Criminal Mastermind",
        "theme": {
            "name": "Sunset Gold (ذهبي برتقالي)",
            "accent": "#f39c12",
            "icon": "🏎️"
        }
    },
    {
        "game_name": "Counter-Strike 2",
        "details_pool": [
            "Premier Mode (18,500 CSR) 💣",
            "Competitive Match on Mirage 🏆",
            "Dust II Mid-Door AWP Sniping 🎯",
            "Overtime Match on Inferno (15-14) 🔥"
        ],
        "state_pool": [
            "Score 12-10 (Clutch Ace Moment) 👑",
            "Defusing the Bomb on Site B ⏱️",
            "AWP 1v4 Retake in progress 🎯",
            "Insane One-Taps with AK-47 💥"
        ],
        "large_image": "cs2",
        "large_text": "Counter-Strike 2 - Valve",
        "small_image": "verified",
        "small_text": "Global Elite Tier",
        "theme": {
            "name": "Desert Storm (عاصفة برتقالية)",
            "accent": "#e67e22",
            "icon": "🔫"
        }
    },
    {
        "game_name": "Elden Ring",
        "details_pool": [
            "Shadow of the Erdtree DLC 🌳",
            "Facing Malenia, Blade of Miquella 🗡️",
            "Journey 4 (New Game +++) 💀",
            "Exploring the Lands Between 🌌"
        ],
        "state_pool": [
            "No Hit Boss Fight Attempt #38 ⚔️",
            "Dual Bleed Katana Build 🔥",
            "Defeating Promised Consort Radahn 👑",
            "Collecting all Legendary Sorceries ✨"
        ],
        "large_image": "elden_ring",
        "large_text": "Elden Ring - FromSoftware",
        "small_image": "verified",
        "small_text": "Elden Lord",
        "theme": {
            "name": "Erdtree Gold (ذهب الإردتري)",
            "accent": "#ffd700",
            "icon": "🗡️"
        }
    },
    {
        "game_name": "Visual Studio Code",
        "details_pool": [
            "Developing Discord Rich Presence 💻",
            "Refactoring Asynchronous Core Engine ⚡",
            "Building Modern CustomTkinter UI 🎨",
            "Debugging Python Gateway WebSockets 🐛"
        ],
        "state_pool": [
            "Writing Clean Python Code 🚀",
            "Zero Errors & Clean Architecture 🌟",
            "Compiling High-Performance Scripts ⚙️",
            "Git Commit & Push in progress 📦"
        ],
        "large_image": "vscode",
        "large_text": "Visual Studio Code - Pro Dev",
        "small_image": "verified",
        "small_text": "Senior Developer",
        "theme": {
            "name": "Matrix Developer (أزرق برمجي)",
            "accent": "#5865f2",
            "icon": "💻"
        }
    },
    {
        "game_name": "League of Legends",
        "details_pool": [
            "Ranked Solo/Duo (Master Tier) 🏆",
            "Climbing to Grandmaster 🔥",
            "Summoner's Rift Mid Lane Domination ⚔️",
            "Full 5-Man Clash Tournament 🛡️"
        ],
        "state_pool": [
            "Playing Yasuo (14/2/8 KDA) 🌪️",
            "Baron Nashor Steal & Pentakill 💥",
            "Zed Shadow Assassin Outplays 🌑",
            "Destroying Enemy Nexus (GG WP) 🏰"
        ],
        "large_image": "lol",
        "large_text": "League of Legends - Riot Games",
        "small_image": "verified",
        "small_text": "Challenger Ranked",
        "theme": {
            "name": "Hextech Blue (هكستيك أزرق)",
            "accent": "#0ac8b9",
            "icon": "⚔️"
        }
    },
    {
        "game_name": "Fortnite",
        "details_pool": [
            "Battle Royale - Ranked Unreal 🔥",
            "Victory Royale Grind (12 Kills) 👑",
            "Custom Zone Wars & Box Fights 🧱",
            "Festival Main Stage Guitar Solo 🎸"
        ],
        "state_pool": [
            "Final 2 Players Moving Zone 🌀",
            "Piece Control & Triple Edit 💥",
            "Sniper 250m Headshot Elimination 🎯",
            "Claiming the Crown Victory Royale 🏆"
        ],
        "large_image": "fortnite",
        "large_text": "Fortnite - Epic Games",
        "small_image": "verified",
        "small_text": "Unreal Rank #1",
        "theme": {
            "name": "Royal Purple (بنفسجي ملكي)",
            "accent": "#9b59b6",
            "icon": "👑"
        }
    },
    {
        "game_name": "Spotify / Listening",
        "details_pool": [
            "Listening to Cyberpunk Synthwave 🎧",
            "Deep Focus & Coding Beats 🎵",
            "Phonk & Gaming Workout Mix ⚡",
            "Night Drives & Lofi Chill 🌙"
        ],
        "state_pool": [
            "Volume at 100% (Bass Boosted) 🔊",
            "Shuffle Play on Favorite Playlist 🔁",
            "Jamming with Friends on Spotify 🎶",
            "Next: The Perfect Girl (Retrowave) 🌟"
        ],
        "large_image": "spotify",
        "large_text": "Spotify Music - Premium",
        "small_image": "verified",
        "small_text": "Audio HQ 320kbps",
        "theme": {
            "name": "Spotify Neon (أخضر سبوتيفاي)",
            "accent": "#1db954",
            "icon": "🎧"
        }
    }
]


def generate_random_presence() -> Dict[str, Any]:
    """Randomly selects a game, details, state, and stylish theme."""
    choice = random.choice(RANDOM_GAMES_DATABASE)
    details = random.choice(choice["details_pool"])
    state = random.choice(choice["state_pool"])
    theme_info = choice["theme"]

    return {
        "game_name": choice["game_name"],
        "details": details,
        "state": state,
        "large_image": choice["large_image"],
        "large_text": choice["large_text"],
        "small_image": choice["small_image"],
        "small_text": choice["small_text"],
        "theme_name": theme_info["name"],
        "theme_accent": theme_info["accent"],
        "theme_icon": theme_info["icon"]
    }
