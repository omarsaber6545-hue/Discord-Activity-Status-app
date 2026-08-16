import time
import logging
import asyncio
import concurrent.futures
from typing import Optional, Dict, Any, List

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

try:
    from pypresence import Presence, PyPresenceException
except ImportError:
    Presence = None
    PyPresenceException = Exception


class DiscordRPCManager:
    """Manages the connection and activity updates for Discord Rich Presence with non-blocking timeouts."""

    def __init__(self, client_id: Optional[str] = None):
        self.client_id = client_id
        self.rpc: Optional[Any] = None
        self.is_connected = False
        self.start_time: Optional[float] = time.time()
        self.last_error: str = ""

    def connect(self, client_id: Optional[str] = None, timeout: float = 4.0) -> bool:
        """Establishes connection with local Discord client using Client ID with timeout."""
        if Presence is None:
            self.last_error = "pypresence library is not installed."
            logging.error(self.last_error)
            return False

        if client_id:
            self.client_id = str(client_id).strip()

        if not self.client_id or not self.client_id.isdigit():
            self.last_error = "Application Client ID غير صحيح! يجب أن يتكون من أرقام فقط (18-19 رقم)."
            logging.error(self.last_error)
            return False

        # Ensure active asyncio loop in non-main threads
        try:
            asyncio.get_event_loop()
        except RuntimeError:
            asyncio.set_event_loop(asyncio.new_event_loop())

        def _raw_connect():
            if self.is_connected:
                self.disconnect()
            self.rpc = Presence(self.client_id)
            self.rpc.connect()

        try:
            with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                future = executor.submit(_raw_connect)
                future.result(timeout=timeout)

            self.is_connected = True
            self.start_time = time.time()
            self.last_error = ""
            logging.info(f"Successfully connected to Discord RPC with Client ID: {self.client_id}")
            return True

        except concurrent.futures.TimeoutError:
            self.is_connected = False
            self.last_error = "⏰ انتهى وقت محاولة الاتصال بديسكورد. تأكد من فتح برنامج ديسكورد الرسمي على جهازك!"
            logging.error(self.last_error)
            self.disconnect()
            return False
        except PyPresenceException as e:
            self.is_connected = False
            err_str = str(e)
            if "4000" in err_str or "Invalid" in err_str:
                self.last_error = f"Client ID غير موجود على ديسكورد ({self.client_id}). قم بإنشاء تطبيق في Discord Developer Portal واستخدم ID الخاص بك!"
            else:
                self.last_error = f"خطأ في الاتصال بديسكورد: {err_str}"
            logging.error(self.last_error)
            return False
        except Exception as e:
            self.is_connected = False
            self.last_error = f"تأكد من فتح تطبيق ديسكورد على جهازك أولاً! ({str(e)})"
            logging.error(self.last_error)
            return False

    def update_presence(
        self,
        details: str = "",
        state: str = "",
        large_image: str = "",
        large_text: str = "",
        small_image: str = "",
        small_text: str = "",
        show_timer: bool = True,
        button1_label: str = "",
        button1_url: str = "",
        button2_label: str = "",
        button2_url: str = "",
        start_time: Optional[float] = None,
        timeout: float = 3.0
    ) -> bool:
        """Updates the current active Discord Rich Presence card with non-blocking execution."""
        if not self.is_connected or not self.rpc:
            self.last_error = "تطبيق ديسكورد غير متصل."
            return False

        kwargs: Dict[str, Any] = {}

        if details.strip():
            kwargs["details"] = details.strip()
        if state.strip():
            kwargs["state"] = state.strip()

        if large_image.strip():
            kwargs["large_image"] = large_image.strip()
            if large_text.strip():
                kwargs["large_text"] = large_text.strip()

        if small_image.strip():
            kwargs["small_image"] = small_image.strip()
            if small_text.strip():
                kwargs["small_text"] = small_text.strip()

        if show_timer:
            if start_time is not None:
                self.start_time = start_time
            elif self.start_time is None:
                self.start_time = time.time()
            kwargs["start"] = int(self.start_time)
        else:
            self.start_time = None

        # Process custom interactive buttons
        buttons: List[Dict[str, str]] = []
        if button1_label.strip() and button1_url.strip():
            url1 = button1_url.strip()
            if not url1.startswith(("http://", "https://")):
                url1 = "https://" + url1
            buttons.append({"label": button1_label.strip()[:32], "url": url1})

        if button2_label.strip() and button2_url.strip():
            url2 = button2_url.strip()
            if not url2.startswith(("http://", "https://")):
                url2 = "https://" + url2
            buttons.append({"label": button2_label.strip()[:32], "url": url2})

        if buttons:
            kwargs["buttons"] = buttons

        def _raw_update():
            self.rpc.update(**kwargs)

        try:
            with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                future = executor.submit(_raw_update)
                future.result(timeout=timeout)
            logging.info("Discord Rich Presence updated successfully.")
            self.last_error = ""
            return True
        except concurrent.futures.TimeoutError:
            self.last_error = "⏰ انتهى وقت تحديث الحالة. يبدو أن ديسكورد بطيء أو غير مستجيب."
            logging.error(self.last_error)
            return False
        except PyPresenceException as e:
            self.last_error = f"فشل تحديث الحالة: {str(e)}"
            logging.error(self.last_error)
            return False
        except Exception as e:
            self.last_error = f"خطأ التحديث: {str(e)}"
            logging.error(self.last_error)
            return False

    def disconnect(self) -> None:
        """Safely disconnects from Discord RPC without hanging."""
        if self.rpc and self.is_connected:
            def _raw_close():
                try:
                    self.rpc.close()
                except Exception:
                    pass

            try:
                with concurrent.futures.ThreadPoolExecutor(max_workers=1) as executor:
                    future = executor.submit(_raw_close)
                    future.result(timeout=1.5)
            except Exception:
                pass

        self.is_connected = False
        self.rpc = None
        self.start_time = None
        logging.info("Disconnected from Discord RPC.")
