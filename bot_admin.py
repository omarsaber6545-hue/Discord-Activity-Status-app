"""
=============================================================================
  👑 OMAR DEV - DISCORD ACTIVITY STATUS ADMIN BOT
  تحكم كامل في تطبيق الأندرويد وإرسال إشعارات وحظر ومسح الحسابات عن بعد
=============================================================================
"""

import discord
from discord import app_commands
from discord.ext import commands
import json
import time
import base64
import sys

# --- إعدادات البوت والمالك ---
_B64_KEY = b"TVRVek9ETTJOVFkyTnpZek9Ua3lNel15Tnc9PS5HMTVDeHguUER5VXBkSTl2MUpNSzVmUThmMHZDcUF3LTYwd2lWanFiWVhQT3c="
BOT_TOKEN = base64.b64decode(_B64_KEY).decode("utf-8").replace("==", "")

OWNER_ID = 1512205578015871048           # أيدي حسابك (rip_luufy25100)
SYNC_CHANNEL_ID = 1538588035749384222     # أيدي روم الإشعارات والتحكم بالتطبيق

intents = discord.Intents.default()
intents.message_content = True

bot = commands.Bot(command_prefix="!", intents=intents, help_command=None)

@bot.event
async def on_ready():
    try:
        synced = await bot.tree.sync()
        print("==================================================")
        print(f"🔥 Logged in as: {bot.user} (ID: {bot.user.id})")
        print(f"⚡ Synced {len(synced)} Slash Commands successfully!")
        print(f"👑 Admin Owner ID: {OWNER_ID}")
        print(f"📢 Control Channel ID: {SYNC_CHANNEL_ID}")
        print("==================================================")
        print("🚀 البوت جاهز وشغال 100%! يمكنك كتابة الأوامر بالسلاش (/) أو بعلامة التعجب (!).")
        print("💡 الأوامر المتاحة:")
        print("   /broadcast [عنوان] [رسالة]  أو  !broadcast عنوان | رسالة")
        print("   /ban [user_id] [سبب]        أو  !ban [user_id] [سبب]")
        print("   /wipe [user_id]             أو  !wipe [user_id]")
        print("   /unban [user_id]            أو  !unban [user_id]")
        print("   /status                     أو  !status")
        print("==================================================")
    except Exception as e:
        print(f"⚠️ Note during sync: {e}")

def is_owner(user_id: int) -> bool:
    return user_id == OWNER_ID

# -------------------------------------------------------------
# 1. إرسال إشعار فوري لجميع مستخدمي التطبيق (/broadcast & !broadcast)
# -------------------------------------------------------------
@bot.tree.command(name="broadcast", description="📢 إرسال إشعار وتنبيه فوري لجميع مستخدمي التطبيق (حتى والتطبيق مغلق)")
@app_commands.describe(
    title="عنوان الإشعار",
    message="نص الرسالة / الإعلان",
    target_user_id="اختياري: إرسال لمستخدم محدد فقط عن طريق الآيدي بتاعه"
)
async def broadcast_slash(interaction: discord.Interaction, title: str, message: str, target_user_id: str = None):
    if not is_owner(interaction.user.id):
        await interaction.response.send_message("❌ هذا الأمر خاص بالمطور Omar Dev فقط!", ephemeral=True)
        return

    await interaction.response.defer(ephemeral=False)
    await send_broadcast_payload(interaction.channel, title, message, f"Omar Dev ({interaction.user.name})", target_user_id)
    embed = discord.Embed(
        title="📢 تم إرسال الإشعار لجميع مستخدمي التطبيق بنجاح!",
        description=f"**العنوان:** {title}\n**الرسالة:** {message}",
        color=0x5865F2
    )
    if target_user_id:
        embed.add_field(name="🎯 المستلم المحدد", value=f"`{target_user_id}`", inline=True)
    else:
        embed.add_field(name="👥 المستلمون", value="جميع مستخدمي التطبيق (عام)", inline=True)
    embed.set_footer(text="سيصل للمستخدمين إشعار أندرويد حقيقي ونافذة منبثقة عند فتح التطبيق 📲")
    await interaction.followup.send(embed=embed)

@bot.command(name="broadcast")
async def broadcast_prefix(ctx, *, args: str = None):
    if not is_owner(ctx.author.id):
        await ctx.send("❌ هذا الأمر خاص بالمطور Omar Dev فقط!")
        return

    if not args or "|" not in args:
        await ctx.send("⚠️ طريقة الاستخدام:\n`!broadcast العنوان | نص الرسالة`\nأو استخدام السلاش: `/broadcast`")
        return

    parts = args.split("|", 1)
    title = parts[0].strip()
    message = parts[1].strip()

    await send_broadcast_payload(ctx.channel, title, message, f"Omar Dev ({ctx.author.name})", None)
    embed = discord.Embed(
        title="📢 تم إرسال الإشعار لجميع مستخدمي التطبيق بنجاح!",
        description=f"**العنوان:** {title}\n**الرسالة:** {message}",
        color=0x5865F2
    )
    embed.set_footer(text="سيصل للمستخدمين إشعار أندرويد حقيقي ونافذة منبثقة عند فتح التطبيق 📲")
    await ctx.send(embed=embed)

async def send_broadcast_payload(source_channel, title, message, author, target_user_id):
    payload_data = {
        "action": "broadcast",
        "id": str(int(time.time() * 1000)),
        "title": title,
        "message": message,
        "author": author,
        "target_user_id": target_user_id.strip() if target_user_id else None,
        "timestamp": int(time.time() * 1000)
    }
    raw_json = json.dumps(payload_data)
    target_channel = bot.get_channel(SYNC_CHANNEL_ID) or source_channel
    if target_channel:
        await target_channel.send(f"```json\n{raw_json}\n```")

# -------------------------------------------------------------
# 2. حظر مستخدم عن بعد (/ban & !ban)
# -------------------------------------------------------------
@bot.tree.command(name="ban", description="🚫 حظر مستخدم معين ومنعه من تشغيل التطبيق")
@app_commands.describe(user_id="أيدي حساب المستخدم في ديسكورد", reason="سبب الحظر")
async def ban_slash(interaction: discord.Interaction, user_id: str, reason: str = "تم حظرك بواسطة المطور Omar Dev 🚫"):
    if not is_owner(interaction.user.id):
        await interaction.response.send_message("❌ هذا الأمر خاص بالمطور Omar Dev فقط!", ephemeral=True)
        return

    await interaction.response.defer()
    await send_action_payload(interaction.channel, "ban", user_id, reason)
    embed = discord.Embed(
        title="🚫 تم حظر المستخدم بنجاح!",
        description=f"تم حظر الآيدي: `{user_id}`\n**السبب:** {reason}",
        color=0xED4245
    )
    embed.set_footer(text="سيتم إيقاف التطبيق وفصل الاتصال فوراً عند هذا المستخدم.")
    await interaction.followup.send(embed=embed)

@bot.command(name="ban")
async def ban_prefix(ctx, user_id: str = None, *, reason: str = "تم حظرك بواسطة المطور Omar Dev 🚫"):
    if not is_owner(ctx.author.id):
        await ctx.send("❌ هذا الأمر خاص بالمطور Omar Dev فقط!")
        return
    if not user_id:
        await ctx.send("⚠️ طريقة الاستخدام: `!ban [user_id] [السبب]`")
        return

    await send_action_payload(ctx.channel, "ban", user_id, reason)
    embed = discord.Embed(
        title="🚫 تم حظر المستخدم بنجاح!",
        description=f"تم حظر الآيدي: `{user_id}`\n**السبب:** {reason}",
        color=0xED4245
    )
    await ctx.send(embed=embed)

# -------------------------------------------------------------
# 3. مسح بيانات مستخدم وتصفير حسابه عن بعد (/wipe & !wipe)
# -------------------------------------------------------------
@bot.tree.command(name="wipe", description="🗑️ مسح بيانات وتوكن مستخدم وطرده من التطبيق فوراً")
@app_commands.describe(user_id="أيدي حساب المستخدم في ديسكورد")
async def wipe_slash(interaction: discord.Interaction, user_id: str):
    if not is_owner(interaction.user.id):
        await interaction.response.send_message("❌ هذا الأمر خاص بالمطور Omar Dev فقط!", ephemeral=True)
        return

    await interaction.response.defer()
    await send_action_payload(interaction.channel, "wipe", user_id)
    embed = discord.Embed(
        title="🗑️ تم إرسال أمر المسح والتصفير (Wipe Data)",
        description=f"تم مسح بيانات وتوكن الآيدي: `{user_id}` بنجاح وتصفير التطبيق لديه.",
        color=0xFEE75C
    )
    await interaction.followup.send(embed=embed)

@bot.command(name="wipe")
async def wipe_prefix(ctx, user_id: str = None):
    if not is_owner(ctx.author.id):
        await ctx.send("❌ هذا الأمر خاص بالمطور Omar Dev فقط!")
        return
    if not user_id:
        await ctx.send("⚠️ طريقة الاستخدام: `!wipe [user_id]`")
        return

    await send_action_payload(ctx.channel, "wipe", user_id)
    embed = discord.Embed(
        title="🗑️ تم إرسال أمر المسح والتصفير (Wipe Data)",
        description=f"تم مسح بيانات وتوكن الآيدي: `{user_id}` بنجاح وتصفير التطبيق لديه.",
        color=0xFEE75C
    )
    await ctx.send(embed=embed)

# -------------------------------------------------------------
# 4. فك الحظر (/unban & !unban)
# -------------------------------------------------------------
@bot.tree.command(name="unban", description="🔓 فك الحظر عن مستخدم والسماح له باستخدام التطبيق")
@app_commands.describe(user_id="أيدي حساب المستخدم في ديسكورد")
async def unban_slash(interaction: discord.Interaction, user_id: str):
    if not is_owner(interaction.user.id):
        await interaction.response.send_message("❌ هذا الأمر خاص بالمطور Omar Dev فقط!", ephemeral=True)
        return

    await interaction.response.defer()
    await send_action_payload(interaction.channel, "unban", user_id)
    embed = discord.Embed(
        title="🔓 تم فك الحظر بنجاح!",
        description=f"تم فك الحظر عن الآيدي: `{user_id}`.",
        color=0x57F287
    )
    await interaction.followup.send(embed=embed)

@bot.command(name="unban")
async def unban_prefix(ctx, user_id: str = None):
    if not is_owner(ctx.author.id):
        await ctx.send("❌ هذا الأمر خاص بالمطور Omar Dev فقط!")
        return
    if not user_id:
        await ctx.send("⚠️ طريقة الاستخدام: `!unban [user_id]`")
        return

    await send_action_payload(ctx.channel, "unban", user_id)
    embed = discord.Embed(
        title="🔓 تم فك الحظر بنجاح!",
        description=f"تم فك الحظر عن الآيدي: `{user_id}`.",
        color=0x57F287
    )
    await ctx.send(embed=embed)

async def send_action_payload(source_channel, action: str, user_id: str, reason: str = None):
    payload_data = {
        "action": action,
        "user_id": user_id.strip(),
        "reason": reason,
        "timestamp": int(time.time() * 1000)
    }
    raw_json = json.dumps(payload_data)
    target_channel = bot.get_channel(SYNC_CHANNEL_ID) or source_channel
    if target_channel:
        await target_channel.send(f"```json\n{raw_json}\n```")

# -------------------------------------------------------------
# 5. أمر حالة البوت والتطبيق (/status & !status)
# -------------------------------------------------------------
@bot.tree.command(name="status", description="⚡ فحص حالة البوت والاتصال بالنظام")
async def status_slash(interaction: discord.Interaction):
    await send_status_embed(interaction.response.send_message)

@bot.command(name="status")
async def status_prefix(ctx):
    await send_status_embed(ctx.send)

async def send_status_embed(sender_func):
    embed = discord.Embed(
        title="⚡ Omar Dev App Monitor & Admin Bot",
        description="نظام التحكم والإشعارات في تطبيق ديسكورد يعمل بأعلى كفاءة 24/7!",
        color=0x5865F2
    )
    embed.add_field(name="👑 Developer", value="<@1512205578015871048> (Omar)", inline=True)
    embed.add_field(name="🤖 Bot Ping", value=f"`{round(bot.latency * 1000)}ms`", inline=True)
    embed.add_field(name="📢 Control Channel", value=f"<#{SYNC_CHANNEL_ID}>", inline=True)
    embed.add_field(name="📲 App Version", value="`v2.4 Android APK`", inline=True)
    await sender_func(embed=embed)

if __name__ == "__main__":
    bot.run(BOT_TOKEN)
