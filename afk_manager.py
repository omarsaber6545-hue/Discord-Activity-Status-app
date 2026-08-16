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


def send_discord_reply(token: str, channel_id: str, content: str, reply_to_msg_id: str = None) -> bool:
    """
    Sends a Discord message via REST API to channel or DM.
    """
    clean_token = token.strip()
    if not clean_token or not channel_id or not content:
        return False

    auth_val = clean_token if clean_token.startswith("Bot ") else clean_token
    headers = {
        "Authorization": auth_val,
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }

    url = f"https://discord.com/api/v9/channels/{channel_id}/messages"
    payload: Dict[str, Any] = {"content": content}

    if reply_to_msg_id:
        payload["message_reference"] = {"message_id": str(reply_to_msg_id)}

    try:
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(url, data=data, headers=headers, method="POST")
        with urllib.request.urlopen(req) as resp:
            return resp.status in (200, 201)
    except urllib.error.HTTPError as e:
        # Fallback to direct content if message_reference fails (e.g. in DMs)
        if reply_to_msg_id and "message_reference" in payload:
            del payload["message_reference"]
            try:
                data = json.dumps(payload).encode("utf-8")
                req = urllib.request.Request(url, data=data, headers=headers, method="POST")
                with urllib.request.urlopen(req) as resp:
                    return resp.status in (200, 201)
            except Exception:
                pass
        if e.code == 401 and not clean_token.startswith("Bot "):
            headers["Authorization"] = f"Bot {clean_token}"
            try:
                data = json.dumps(payload).encode("utf-8")
                req_bot = urllib.request.Request(url, data=data, headers=headers, method="POST")
                with urllib.request.urlopen(req_bot) as resp:
                    return resp.status in (200, 201)
            except Exception:
                pass
        if e.code == 429:
            logging.warning(f"Rate limited by Discord (HTTP 429)! Backing off...")
            return False
        logging.error(f"Error sending AFK reply (HTTP {e.code}): {e.reason}")
        return False
    except Exception as e:
        logging.error(f"Error sending AFK reply: {e}")
        return False


class AFKResponderWorker:
    """Manages Gateway WebSocket listener for AFK Auto-Responder on Mentions & DMs."""

    def __init__(self):
        self.is_running = False
        self.token = ""
        self.afk_message = "أنا غير متواجد حالياً، سأقوم بالرد عليك فور عودتي! ☕"
        self.reply_dms = True
        self.reply_mentions = True
        self.cooldown_sec = 15  # Default 15s cooldown to protect Discord account from bans/rate limits
        self.status_message = "🔴 AFK Auto-Responder Stopped"
        self.user_id = ""
        self.user_tag = ""
        self.replies_count = 0
        self.start_time = time.time()
        self.last_replied: Dict[str, float] = {}
        self.stop_event = threading.Event()
        self.worker_thread: Optional[threading.Thread] = None

    def start(
        self,
        token: str,
        afk_message: str,
        reply_dms: bool = True,
        reply_mentions: bool = True,
        cooldown_sec: int = 15
    ) -> Tuple[bool, str]:
        """Starts AFK Auto-Responder Gateway listener in background thread."""
        if self.is_running:
            return False, "AFK Auto-Responder is already running!"

        self.token = token.strip()
        self.afk_message = afk_message.strip() if afk_message.strip() else "أنا غير متواجد حالياً! ☕"
        self.reply_dms = reply_dms
        self.reply_mentions = reply_mentions
        self.cooldown_sec = max(0, cooldown_sec)
        self.start_time = time.time()

        if not self.token:
            return False, "Please enter your Token first to enable AFK!"

        self.stop_event.clear()
        self.is_running = True
        self.replies_count = 0
        self.last_replied.clear()
        self.status_message = "🔄 Activating AFK Auto-Responder..."

        self.worker_thread = threading.Thread(target=self._run_afk_loop, daemon=True)
        self.worker_thread.start()
        return True, "🤖 AFK Auto-Responder activated successfully! Listening for DMs & Mentions."

    def _run_afk_loop(self):
        """Asyncio loop running Gateway WebSocket listener for MESSAGE_CREATE events."""
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)

        try:
            loop.run_until_complete(self._afk_gateway_session())
        except Exception as e:
            logging.error(f"AFK loop error: {e}")
            self.status_message = f"❌ AFK System Error: {e}"
        finally:
            self.is_running = False

    async def _afk_gateway_session(self):
        """Gateway WebSocket listener session with clean status (No image assets)."""
        import websockets

        auth_header = self.token if not self.token.startswith("Bot ") else self.token
        gateway_url = "wss://gateway.discord.gg/?v=9&encoding=json"

        while self.is_running and not self.stop_event.is_set():
            try:
                # Set max_size=None so websockets can receive large Discord READY payloads (e.g. 2.5 MB)
                async with websockets.connect(gateway_url, max_size=None) as ws:
                    hello_raw = await ws.recv()
                    hello = json.loads(hello_raw)
                    heartbeat_interval = hello["d"]["heartbeat_interval"] / 1000.0

                    start_ms = int(self.start_time * 1000)

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
                                "status": "idle",
                                "since": start_ms,
                                "activities": [{
                                    "name": "AFK Mode ☕",
                                    "type": 0,
                                    "details": self.afk_message,
                                    "state": "AFK Auto-Responder Active 🤖",
                                    "timestamps": {
                                        "start": start_ms
                                    }
                                }],
                                "afk": True
                            }
                        }
                    }
                    await ws.send(json.dumps(identify_payload))

                    last_heartbeat = time.time()

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
                            data = packet.get("d", {})

                            if op == 0 and event_type == "READY":
                                user_obj = data.get("user", {})
                                self.user_id = str(user_obj.get("id", ""))
                                username = user_obj.get("username", "Account")
                                self.user_tag = username
                                self.status_message = f"🟢 AFK Active ({self.user_tag}) - Replies Sent: {self.replies_count}"
                                logging.info(f"AFK Gateway READY for user {self.user_tag} ({self.user_id})")

                            elif op == 0 and event_type == "MESSAGE_CREATE":
                                self._handle_message_event(data)

                            elif op == 1:
                                await ws.send(json.dumps({"op": 1, "d": None}))

                        except asyncio.TimeoutError:
                            pass

            except Exception as e:
                logging.warning(f"AFK gateway reconnect pulse: {e}. Reconnecting in 3s...")
                self.status_message = f"🔄 Reconnecting AFK Auto-Responder..."
                await asyncio.sleep(3)

    def _handle_message_event(self, data: Dict[str, Any]):
        """Processes incoming Discord message to check for DM or Mention."""
        author = data.get("author", {})
        author_id = str(author.get("id", ""))

        # Don't auto-reply to oneself or to other bots
        if not author_id or author_id == self.user_id or author.get("bot", False):
            return

        channel_id = str(data.get("channel_id", ""))
        message_id = str(data.get("id", ""))
        guild_id = data.get("guild_id")
        mentions = data.get("mentions", [])
        content = data.get("content", "")

        is_dm = (guild_id is None or str(guild_id).strip() == "" or str(guild_id).strip() == "0")
        is_mentioned = any(str(m.get("id")) == self.user_id for m in mentions) or (self.user_id and (f"<@{self.user_id}>" in content or f"<@!{self.user_id}>" in content))

        should_reply = False
        reply_reason = ""

        if is_dm and self.reply_dms:
            should_reply = True
            reply_reason = "DM Message"
        elif is_mentioned and self.reply_mentions:
            should_reply = True
            reply_reason = "Server Mention"

        if should_reply:
            target_key = f"{author_id}"
            now = time.time()
            last_time = self.last_replied.get(target_key, 0)

            # Check if cooldown has elapsed (e.g. 15s)
            if self.cooldown_sec == 0 or (now - last_time >= self.cooldown_sec):
                self.last_replied[target_key] = now
                success = send_discord_reply(self.token, channel_id, self.afk_message, reply_to_msg_id=message_id)
                if success:
                    self.replies_count += 1
                    self.status_message = f"🟢 Auto-replied to {reply_reason}! ({self.user_tag} - Total Replies: {self.replies_count})"
                    logging.info(f"AFK auto-replied to {author_id} in channel {channel_id} ({reply_reason})")
            else:
                remaining = int(self.cooldown_sec - (now - last_time))
                logging.info(f"AFK cooldown active for user {author_id}: {remaining}s remaining before next reply (Anti-Ban protection).")

    def stop(self):
        """Stops AFK Auto-Responder."""
        self.stop_event.set()
        self.is_running = False
        self.status_message = "🔴 AFK Auto-Responder Stopped."
