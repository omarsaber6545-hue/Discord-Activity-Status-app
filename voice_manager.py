import os
import sys
import json
import time
import asyncio
import threading
import logging
import urllib.request
from typing import Optional, Tuple, Dict, Any

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")


def verify_discord_token(token: str) -> Tuple[bool, str, Dict[str, Any]]:
    """
    Verifies a Discord User Token or Bot Token via Discord API /users/@me.
    Returns (success, result_message, user_data_dict).
    """
    clean_token = token.strip()
    if not clean_token:
        return False, "Token is empty! Please enter your account or bot token.", {}

    headers = {
        "Authorization": clean_token,
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    try:
        req = urllib.request.Request("https://discord.com/api/v9/users/@me", headers=headers)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            username = data.get("username", "Unknown")
            discrim = data.get("discriminator", "0")
            tag = f"{username}#{discrim}" if discrim != "0" else username
            return True, f"✅ Account Verified: {tag}", data
    except urllib.error.HTTPError as e:
        if e.code == 401:
            headers["Authorization"] = f"Bot {clean_token}"
            try:
                req_bot = urllib.request.Request("https://discord.com/api/v9/users/@me", headers=headers)
                with urllib.request.urlopen(req_bot) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    username = data.get("username", "Unknown")
                    tag = f"{username} (Bot)"
                    return True, f"✅ Bot Account Verified: {tag}", data
            except Exception:
                return False, "❌ Invalid Token (401 Unauthorized)! Please check your Token.", {}
        return False, f"❌ Token Verification Error: {e}", {}
    except Exception as e:
        return False, f"❌ Connection Error: {e}", {}


def get_channel_details(token: str, channel_id: str) -> Tuple[bool, str, Dict[str, Any]]:
    """
    Fetches Voice Channel Details (channel name, guild_id) via Discord REST API.
    """
    clean_token = token.strip()
    clean_channel = channel_id.strip()

    if not clean_token or not clean_channel:
        return False, "Token and Channel ID are required!", {}

    headers = {
        "Authorization": clean_token,
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    url = f"https://discord.com/api/v9/channels/{clean_channel}"

    try:
        req = urllib.request.Request(url, headers=headers)
        with urllib.request.urlopen(req) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return True, "Success", data
    except urllib.error.HTTPError as e:
        if e.code == 401:
            headers["Authorization"] = f"Bot {clean_token}"
            try:
                req_bot = urllib.request.Request(url, headers=headers)
                with urllib.request.urlopen(req_bot) as resp:
                    data = json.loads(resp.read().decode("utf-8"))
                    return True, "Success", data
            except Exception:
                return False, "HTTP Error 401: Unauthorized", {}
        elif e.code == 404:
            return False, "Voice Channel ID not found (404 Not Found)! Check the Channel ID.", {}
        return False, f"HTTP Error {e.code}: {e.reason}", {}
    except Exception as e:
        return False, f"Channel Fetch Error: {e}", {}


class VoiceStayWorker:
    """Manages continuous 24/7 Voice Channel connection and auto-reconnect Gateway loop."""

    def __init__(self):
        self.is_running = False
        self.is_connected = False
        self.token = ""
        self.channel_id = ""
        self.guild_id = ""
        self.channel_name = ""
        self.self_deaf = True
        self.self_mute = True
        self.status_message = "🔴 Voice Stay Disconnected"
        self.user_tag = ""
        self.stop_event = threading.Event()
        self.worker_thread: Optional[threading.Thread] = None

    def start(self, token: str, channel_id: str, self_deaf: bool = True, self_mute: bool = True) -> Tuple[bool, str]:
        """Starts 24/7 Voice stay in background thread."""
        if self.is_running:
            return False, "Voice stay service is already running!"

        self.token = token.strip()
        self.channel_id = channel_id.strip()
        self.self_deaf = self_deaf
        self.self_mute = self_mute

        if not self.token:
            return False, "Please enter your Token first!"

        if not self.channel_id or not self.channel_id.isdigit():
            return False, "Voice Channel ID is invalid! Must contain digits only."

        # Verify token identity before starting Gateway
        valid, msg, user_data = verify_discord_token(self.token)
        if not valid:
            return False, msg

        self.user_tag = user_data.get("username", "Account")

        # Fetch Voice Channel Details (Name & Guild ID)
        ok_chan, msg_chan, chan_data = get_channel_details(self.token, self.channel_id)
        if ok_chan:
            self.guild_id = str(chan_data.get("guild_id", ""))
            self.channel_name = str(chan_data.get("name", f"Voice Room #{self.channel_id}"))
        else:
            self.guild_id = ""
            self.channel_name = f"Voice Room #{self.channel_id}"

        self.stop_event.clear()
        self.is_running = True
        self.status_message = f"🔄 Connecting to Voice Channel: 🔊 [{self.channel_name}]..."

        self.worker_thread = threading.Thread(target=self._run_voice_loop, daemon=True)
        self.worker_thread.start()
        return True, f"🚀 Joining Voice Channel: 🔊 [{self.channel_name}] ({self.user_tag})"

    def _run_voice_loop(self):
        """Asyncio loop running Gateway WebSocket voice connection with 24/7 auto-reconnect."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

        try:
            loop.run_until_complete(self._voice_gateway_session())
        except Exception as e:
            logging.error(f"Voice loop error: {e}")
            self.status_message = f"❌ Connection Error: {e}"
        finally:
            self.is_running = False
            self.is_connected = False

    async def _voice_gateway_session(self):
        """Gateway WebSocket voice stay implementation using websockets."""
        import websockets

        auth_header = self.token if not self.token.startswith("Bot ") else self.token
        gateway_url = "wss://gateway.discord.gg/?v=9&encoding=json"

        while self.is_running and not self.stop_event.is_set():
            try:
                # Set max_size=None to allow large Discord READY payloads (> 1MB / 2.5MB)
                async with websockets.connect(gateway_url, max_size=None) as ws:
                    hello_raw = await ws.recv()
                    hello = json.loads(hello_raw)
                    heartbeat_interval = hello["d"]["heartbeat_interval"] / 1000.0

                    # Identify payload
                    identify_payload = {
                        "op": 2,
                        "d": {
                            "token": auth_header,
                            "capabilities": 125,
                            "properties": {
                                "os": "Windows",
                                "browser": "Chrome",
                                "device": ""
                            },
                            "presence": {
                                "status": "online",
                                "since": 0,
                                "activities": [],
                                "afk": False
                            }
                        }
                    }
                    await ws.send(json.dumps(identify_payload))

                    last_heartbeat = time.time()
                    joined_voice = False

                    while self.is_running and not self.stop_event.is_set():
                        now = time.time()
                        if now - last_heartbeat >= heartbeat_interval:
                            await ws.send(json.dumps({"op": 1, "d": None}))
                            last_heartbeat = now

                        try:
                            msg_raw = await asyncio.wait_for(ws.recv(), timeout=1.0)
                            packet = json.loads(msg_raw)
                            op = packet.get("op")
                            event_type = packet.get("t")

                            # Send Voice State Update AFTER Gateway sends READY event
                            if (op == 0 and event_type == "READY") or not joined_voice:
                                voice_state_payload = {
                                    "op": 4,
                                    "d": {
                                        "guild_id": self.guild_id if self.guild_id else None,
                                        "channel_id": self.channel_id,
                                        "self_mute": self.self_mute,
                                        "self_deaf": self.self_deaf
                                    }
                                }
                                await ws.send(json.dumps(voice_state_payload))
                                joined_voice = True
                                self.is_connected = True
                                self.status_message = f"🟢 Connected 24/7 to Voice Channel: 🔊 [{self.channel_name}] ({self.user_tag})"
                                logging.info(f"Joined Voice Channel {self.channel_id} in Guild {self.guild_id}")

                            if op == 1:
                                await ws.send(json.dumps({"op": 1, "d": None}))

                        except asyncio.TimeoutError:
                            if not joined_voice:
                                voice_state_payload = {
                                    "op": 4,
                                    "d": {
                                        "guild_id": self.guild_id if self.guild_id else None,
                                        "channel_id": self.channel_id,
                                        "self_mute": self.self_mute,
                                        "self_deaf": self.self_deaf
                                    }
                                }
                                await ws.send(json.dumps(voice_state_payload))
                                joined_voice = True
                                self.is_connected = True
                                self.status_message = f"🟢 Connected 24/7 to Voice Channel: 🔊 [{self.channel_name}] ({self.user_tag})"

            except Exception as e:
                logging.warning(f"Voice gateway reconnect pulse: {e}. Reconnecting in 3s...")
                self.is_connected = False
                self.status_message = f"🔄 Reconnecting to Voice Channel 🔊 [{self.channel_name}]..."
                await asyncio.sleep(3)

    def stop(self):
        """Stops 24/7 Voice stay."""
        self.stop_event.set()
        self.is_running = False
        self.is_connected = False
        self.status_message = "🔴 Voice Channel Stay Stopped."
