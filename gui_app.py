import os
import sys
import json
import time
import webbrowser
import subprocess
import threading
import tkinter as tk
from typing import Dict, Any

import customtkinter as ctk
from PIL import Image, ImageTk, ImageDraw

from rpc_manager import DiscordRPCManager
from voice_manager import verify_discord_token, VoiceStayWorker
from afk_manager import AFKResponderWorker
from device_spoofer import DeviceSpooferWorker, PLATFORM_PRESETS
from notification_manager import NotificationManager, NotificationItem
from randomizer_engine import generate_random_presence

# Appearance settings
ctk.set_appearance_mode("Dark")
ctk.set_default_color_theme("blue")

CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")


def setup_entry_context_menu(entry_widget):
    """
    Attaches right-click context menu (Paste, Copy, Cut, Select All)
    and handles Ctrl+V, Ctrl+C, Ctrl+X, Ctrl+A across English & Arabic keyboard layouts.
    """
    target = getattr(entry_widget, "_entry", entry_widget)

    menu = tk.Menu(
        target,
        tearoff=0,
        bg="#2b2d31",
        fg="#ffffff",
        activebackground="#5865f2",
        activeforeground="#ffffff",
        relief="flat",
        bd=1
    )

    def _do_paste(event=None):
        try:
            clipboard_text = target.clipboard_get()
            if clipboard_text:
                try:
                    target.delete("sel.first", "sel.last")
                except tk.TclError:
                    pass
                target.insert(target.index(tk.INSERT), clipboard_text)
                target.event_generate("<<Modified>>")
        except Exception:
            pass
        return "break"

    def _do_copy(event=None):
        try:
            try:
                selected = target.selection_get()
            except tk.TclError:
                selected = ""
            if selected:
                target.clipboard_clear()
                target.clipboard_append(selected)
        except Exception:
            pass
        return "break"

    def _do_cut(event=None):
        try:
            try:
                selected = target.selection_get()
            except tk.TclError:
                selected = ""
            if selected:
                target.clipboard_clear()
                target.clipboard_append(selected)
                target.delete("sel.first", "sel.last")
                target.event_generate("<<Modified>>")
        except Exception:
            pass
        return "break"

    def _do_select_all(event=None):
        target.select_range(0, tk.END)
        target.icursor(tk.END)
        return "break"

    menu.add_command(label="📋 Paste / لصق", command=_do_paste)
    menu.add_command(label="📄 Copy / نسخ", command=_do_copy)
    menu.add_command(label="✂️ Cut / قص", command=_do_cut)
    menu.add_separator()
    menu.add_command(label="🔍 Select All / تحديد الكل", command=_do_select_all)

    def _popup_menu(event):
        try:
            menu.tk_popup(event.x_root, event.y_root)
        finally:
            menu.grab_release()

    # Bind right-click on both Tkinter inner entry and CTkEntry frame
    target.bind("<Button-3>", _popup_menu)
    if hasattr(entry_widget, "bind"):
        entry_widget.bind("<Button-3>", _popup_menu)

    def _on_key_press(event):
        if event.state & 4:
            if event.keycode == 86 or event.keysym.lower() in ("v", "r"):
                return _do_paste(event)
            elif event.keycode == 67:
                return _do_copy(event)
            elif event.keycode == 88:
                return _do_cut(event)
            elif event.keycode == 65:
                return _do_select_all(event)

    # Keyboard shortcut bindings
    for bind_target in [target, entry_widget]:
        if hasattr(bind_target, "bind"):
            bind_target.bind("<Control-v>", _do_paste)
            bind_target.bind("<Control-V>", _do_paste)
            bind_target.bind("<Control-c>", _do_copy)
            bind_target.bind("<Control-C>", _do_copy)
            bind_target.bind("<Control-x>", _do_cut)
            bind_target.bind("<Control-X>", _do_cut)
            bind_target.bind("<Control-a>", _do_select_all)
            bind_target.bind("<Control-A>", _do_select_all)
            bind_target.bind("<Key>", _on_key_press)


class NotificationCenterDialog(ctk.CTkToplevel):
    """Sleek modal dialog showing real-time event logs, errors, and system notifications."""

    def __init__(self, parent, notif_manager: NotificationManager):
        super().__init__(parent)

        self.notif_manager = notif_manager
        self.current_filter = "all"

        self.title("🔔 Notification Center — مركز الإشعارات")
        self.geometry("580x640")
        self.minsize(480, 420)
        self.configure(fg_color="#18191c")

        self.transient(parent)
        self.after(100, self.lift)

        self._build_ui()
        self._refresh_list()

    def _build_ui(self):
        # Header Container
        header_frame = ctk.CTkFrame(self, fg_color="#1e1f22", corner_radius=0, height=70)
        header_frame.pack(fill="x")

        title_lbl = ctk.CTkLabel(
            header_frame,
            text="🔔 Notification Center (مركز الإشعارات)",
            font=ctk.CTkFont(family="Segoe UI", size=17, weight="bold"),
            text_color="#ffffff"
        )
        title_lbl.pack(anchor="w", padx=18, pady=(12, 2))

        sub_lbl = ctk.CTkLabel(
            header_frame,
            text="Real-time system events, status changes & error logs / سجل الأنشطة والأخطاء",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color="#949ba4"
        )
        sub_lbl.pack(anchor="w", padx=18, pady=(0, 10))

        # Filter Category Bar
        filter_bar = ctk.CTkFrame(self, fg_color="#111214", corner_radius=0)
        filter_bar.pack(fill="x", padx=12, pady=(10, 6))

        filters = [
            ("All (الكل)", "all"),
            ("❌ Errors", "error"),
            ("🟢 Success", "success"),
            ("⚠️ Warnings", "warning"),
            ("ℹ️ Info", "info"),
        ]

        self.filter_buttons = {}
        for label, key in filters:
            btn = ctk.CTkButton(
                filter_bar,
                text=label,
                command=lambda k=key: self._set_filter(k),
                fg_color="#5865f2" if key == "all" else "#2b2d31",
                hover_color="#4752c4",
                font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
                height=28,
                corner_radius=6
            )
            btn.pack(side="left", padx=3, pady=6)
            self.filter_buttons[key] = btn

        # Scrollable Notifications Container
        self.scroll_frame = ctk.CTkScrollableFrame(self, fg_color="transparent")
        self.scroll_frame.pack(fill="both", expand=True, padx=12, pady=6)

        # Bottom Action Bar
        bottom_bar = ctk.CTkFrame(self, fg_color="#1e1f22", corner_radius=0, height=45)
        bottom_bar.pack(fill="x", side="bottom")

        btn_clear = ctk.CTkButton(
            bottom_bar,
            text="🗑️ Clear History (مسح السجل)",
            command=self._clear_all,
            fg_color="#ed4245",
            hover_color="#c03537",
            font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
            height=30,
            corner_radius=6
        )
        btn_clear.pack(side="left", padx=14, pady=8)

        btn_read = ctk.CTkButton(
            bottom_bar,
            text="✓ Mark Read (تحديد كمقروء)",
            command=self._mark_all_read,
            fg_color="#2b2d31",
            hover_color="#3c3e44",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            height=30,
            corner_radius=6
        )
        btn_read.pack(side="left", padx=4, pady=8)

        btn_close = ctk.CTkButton(
            bottom_bar,
            text="✕ Close",
            command=self.destroy,
            fg_color="#4e5058",
            hover_color="#6d6f78",
            font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
            width=70,
            height=30,
            corner_radius=6
        )
        btn_close.pack(side="right", padx=14, pady=8)

    def _set_filter(self, key: str):
        self.current_filter = key
        for k, btn in self.filter_buttons.items():
            btn.configure(fg_color="#5865f2" if k == key else "#2b2d31")
        self._refresh_list()

    def _refresh_list(self):
        for widget in self.scroll_frame.winfo_children():
            widget.destroy()

        items = self.notif_manager.get_filtered(self.current_filter)
        if not items:
            empty_lbl = ctk.CTkLabel(
                self.scroll_frame,
                text="✨ No notifications in this category!\nلا توجد إشعارات حالياً في هذا القسم",
                font=ctk.CTkFont(family="Segoe UI", size=13),
                text_color="#949ba4"
            )
            empty_lbl.pack(pady=60)
            return

        for item in items:
            card = ctk.CTkFrame(
                self.scroll_frame,
                fg_color="#232428" if item.read else "#2b2d31",
                corner_radius=8,
                border_width=1,
                border_color=item.color
            )
            card.pack(fill="x", pady=4, padx=2)

            top_row = ctk.CTkFrame(card, fg_color="transparent")
            top_row.pack(fill="x", padx=12, pady=(8, 2))

            title_txt = f"{item.icon} {item.title}"
            title_lbl = ctk.CTkLabel(
                top_row,
                text=title_txt,
                font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
                text_color=item.color
            )
            title_lbl.pack(side="left")

            time_lbl = ctk.CTkLabel(
                top_row,
                text=f"⏱️ {item.timestamp}",
                font=ctk.CTkFont(family="Segoe UI", size=10),
                text_color="#949ba4"
            )
            time_lbl.pack(side="right")

            msg_lbl = ctk.CTkLabel(
                card,
                text=item.message,
                font=ctk.CTkFont(family="Segoe UI", size=11),
                text_color="#dbdee1",
                wraplength=480,
                justify="left"
            )
            msg_lbl.pack(anchor="w", padx=12, pady=(0, 8))

    def _mark_all_read(self):
        self.notif_manager.mark_all_read()
        self._refresh_list()

    def _clear_all(self):
        self.notif_manager.clear_all()
        self._refresh_list()


class DiscordPreviewCard(ctk.CTkFrame):
    """Modern, high-fidelity visual preview of the Discord Activity Card."""

    def __init__(self, master, **kwargs):
        super().__init__(
            master,
            fg_color="#111214",
            corner_radius=14,
            border_width=1,
            border_color="#2b2d31",
            **kwargs
        )

        # Title / Activity Status Header
        self.header_frame = ctk.CTkFrame(self, fg_color="transparent")
        self.header_frame.pack(fill="x", padx=16, pady=(14, 8))

        self.header_label = ctk.CTkLabel(
            self.header_frame,
            text="⚡ LIVE DISCORD CARD PREVIEW",
            font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
            text_color="#949ba4"
        )
        self.header_label.pack(anchor="w")

        # Main Activity Card Container
        self.card_bg = ctk.CTkFrame(
            self,
            fg_color="#2b2d31",
            corner_radius=12,
            border_width=1,
            border_color="#35363c"
        )
        self.card_bg.pack(fill="both", expand=True, padx=14, pady=(0, 14))

        # Activity Title Header ("PLAYING A GAME")
        self.activity_type = ctk.CTkLabel(
            self.card_bg,
            text="🎮 PLAYING A GAME",
            font=ctk.CTkFont(family="Segoe UI", size=11, weight="bold"),
            text_color="#b5bac1"
        )
        self.activity_type.pack(anchor="w", padx=16, pady=(14, 10))

        # Horizontal Body Frame for Icon & Details
        self.body_frame = ctk.CTkFrame(self.card_bg, fg_color="transparent")
        self.body_frame.pack(fill="x", padx=16, pady=0)

        # Image Container (Large + Small Overlay)
        self.image_container = ctk.CTkFrame(self.body_frame, fg_color="transparent", width=82, height=82)
        self.image_container.pack(side="left", anchor="n", padx=(0, 14))

        # Large Image Placeholder Frame
        self.large_img_frame = ctk.CTkFrame(
            self.image_container,
            width=74,
            height=74,
            fg_color="#5865f2",
            corner_radius=14
        )
        self.large_img_frame.place(x=0, y=0)

        self.large_img_label = ctk.CTkLabel(
            self.large_img_frame,
            text="🎮",
            font=ctk.CTkFont(size=32),
            text_color="#ffffff"
        )
        self.large_img_label.place(relx=0.5, rely=0.5, anchor="center")

        # Small Image Overlay Badge
        self.small_img_frame = ctk.CTkFrame(
            self.image_container,
            width=28,
            height=28,
            fg_color="#23a55a",
            corner_radius=14,
            border_width=2,
            border_color="#2b2d31"
        )
        self.small_img_frame.place(x=52, y=52)

        self.small_img_label = ctk.CTkLabel(
            self.small_img_frame,
            text="✨",
            font=ctk.CTkFont(size=12),
            text_color="#ffffff"
        )
        self.small_img_label.place(relx=0.5, rely=0.5, anchor="center")

        # Text Details Container
        self.text_frame = ctk.CTkFrame(self.body_frame, fg_color="transparent")
        self.text_frame.pack(side="left", fill="both", expand=True)

        # App / Game Title ("omar dev")
        self.game_title = ctk.CTkLabel(
            self.text_frame,
            text="omar dev",
            font=ctk.CTkFont(family="Segoe UI", size=15, weight="bold"),
            text_color="#ffffff",
            anchor="w"
        )
        self.game_title.pack(fill="x", pady=(0, 2))

        # Details Line
        self.details_label = ctk.CTkLabel(
            self.text_frame,
            text="Writing code in VS Code",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color="#dbdee1",
            anchor="w"
        )
        self.details_label.pack(fill="x", pady=0)

        # State Line
        self.state_label = ctk.CTkLabel(
            self.text_frame,
            text="Developing awesome apps 🚀",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color="#dbdee1",
            anchor="w"
        )
        self.state_label.pack(fill="x", pady=0)

        # Timer Line
        self.timer_label = ctk.CTkLabel(
            self.text_frame,
            text="⏱️ 00:00:00 elapsed",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color="#949ba4",
            anchor="w"
        )
        self.timer_label.pack(fill="x", pady=(3, 0))

        # Interactive Buttons Frame inside Activity Card
        self.buttons_frame = ctk.CTkFrame(self.card_bg, fg_color="transparent")
        self.buttons_frame.pack(fill="x", padx=16, pady=(14, 16))

        self.btn1_preview = ctk.CTkButton(
            self.buttons_frame,
            text="🔗 GitHub: Omar-Dev",
            fg_color="#4e5058",
            hover_color="#5d6069",
            text_color="#ffffff",
            height=34,
            corner_radius=6,
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            command=lambda: self._open_url(self.btn1_url)
        )
        self.btn1_url = "https://github.com/omarsaber6545-hue"

        self.btn2_preview = ctk.CTkButton(
            self.buttons_frame,
            text="🌐 Omar Dev Site",
            fg_color="#4e5058",
            hover_color="#5d6069",
            text_color="#ffffff",
            height=34,
            corner_radius=6,
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            command=lambda: self._open_url(self.btn2_url)
        )
        self.btn2_url = ""

    def _open_url(self, url: str):
        if url and url.startswith(("http://", "https://")):
            webbrowser.open(url)

    def update_card(
        self,
        app_name: str,
        details: str,
        state: str,
        large_image: str,
        large_text: str,
        small_image: str,
        small_text: str,
        show_timer: bool,
        elapsed_str: str,
        btn1_label: str,
        btn1_url: str,
        btn2_label: str,
        btn2_url: str
    ):
        self.game_title.configure(text=app_name if app_name else "omar dev")

        if details.strip():
            self.details_label.configure(text=details.strip())
            self.details_label.pack(fill="x", pady=0)
        else:
            self.details_label.pack_forget()

        if state.strip():
            self.state_label.configure(text=state.strip())
            self.state_label.pack(fill="x", pady=0)
        else:
            self.state_label.pack_forget()

        if show_timer:
            self.timer_label.configure(text=f"⏱️ {elapsed_str} elapsed")
            self.timer_label.pack(fill="x", pady=(3, 0))
        else:
            self.timer_label.pack_forget()

        if small_image.strip():
            self.small_img_frame.place(x=52, y=52)
        else:
            self.small_img_frame.place_forget()

        self.btn1_url = btn1_url.strip()
        self.btn2_url = btn2_url.strip()

        has_b1 = bool(btn1_label.strip() and btn1_url.strip())
        has_b2 = bool(btn2_label.strip() and btn2_url.strip())

        if has_b1 or has_b2:
            self.buttons_frame.pack(fill="x", padx=16, pady=(14, 16))
        else:
            self.buttons_frame.pack_forget()

        if has_b1:
            self.btn1_preview.configure(text=f"🔗 {btn1_label.strip()}")
            self.btn1_preview.pack(fill="x", pady=(0, 6) if has_b2 else 0)
        else:
            self.btn1_preview.pack_forget()

        if has_b2:
            self.btn2_preview.configure(text=f"🌐 {btn2_label.strip()}")
            self.btn2_preview.pack(fill="x", pady=0)
        else:
            self.btn2_preview.pack_forget()


class OmarDevApp(ctk.CTk):

    def __init__(self):
        super().__init__()

        self.title("🎮 omar dev - Discord Rich Presence, VR, PS5, Voice Stay & AFK Manager")
        self.geometry("1140x880")
        self.minsize(1000, 700)

        # Managers
        self.rpc_manager = DiscordRPCManager()
        self.voice_worker = VoiceStayWorker()
        self.afk_worker = AFKResponderWorker()
        self.spoofer_worker = DeviceSpooferWorker()
        self.notif_manager = NotificationManager()
        self.notif_dialog = None
        self.toast_frame = None
        self.toast_job = None
        self.start_timestamp = time.time()

        self.notif_manager.on_notification_added = self._on_new_notification
        self.notif_manager.on_state_changed = self._update_notification_badge

        # Load Configuration
        self.config = self.load_config()

        # Observable Variables
        curr_presence = self.config.get("current_presence", {})
        self.var_client_id = ctk.StringVar(value=self.config.get("client_id", "1536494151074586624"))
        self.var_game_name = ctk.StringVar(value=curr_presence.get("game_name", "omar dev"))
        self.var_details = ctk.StringVar(value=curr_presence.get("details", "Writing code in VS Code"))
        self.var_state = ctk.StringVar(value=curr_presence.get("state", "Developing awesome apps 🚀"))
        self.var_large_img = ctk.StringVar(value=curr_presence.get("large_image", "logo"))
        self.var_large_txt = ctk.StringVar(value=curr_presence.get("large_text", "Omar Dev - Coding"))
        self.var_small_img = ctk.StringVar(value=curr_presence.get("small_image", "logo"))
        self.var_small_txt = ctk.StringVar(value=curr_presence.get("small_text", "Coding is life"))
        self.var_show_timer = ctk.BooleanVar(value=curr_presence.get("show_timer", True))

        self.var_btn1_lbl = ctk.StringVar(value=curr_presence.get("button1_label", "GitHub: Omar-Dev"))
        self.var_btn1_url = ctk.StringVar(value=curr_presence.get("button1_url", "https://github.com/omarsaber6545-hue"))
        self.var_btn2_lbl = ctk.StringVar(value=curr_presence.get("button2_label", "Omar Dev Site"))
        self.var_btn2_url = ctk.StringVar(value=curr_presence.get("button2_url", "https://omar-dev.com"))

        # Account Token & Device / Voice / AFK Variables
        self.var_user_token = ctk.StringVar(value=self.config.get("user_token", ""))
        self.var_device_platform = ctk.StringVar(value=self.config.get("device_platform", "vr"))
        self.var_device_custom_text = ctk.StringVar(value=self.config.get("device_custom_text", ""))

        self.var_voice_channel_id = ctk.StringVar(value=self.config.get("voice_channel_id", ""))
        self.var_voice_deaf = ctk.BooleanVar(value=self.config.get("voice_deaf", True))
        self.var_voice_mute = ctk.BooleanVar(value=self.config.get("voice_mute", True))

        self.var_afk_message = ctk.StringVar(value=self.config.get("afk_message", "أنا غير متواجد حالياً، سأقوم بالرد عليك فور عودتي! ☕"))
        self.var_afk_dms = ctk.BooleanVar(value=self.config.get("afk_reply_dms", True))
        self.var_afk_mentions = ctk.BooleanVar(value=self.config.get("afk_reply_mentions", True))
        self.var_afk_cooldown = ctk.StringVar(value=str(self.config.get("afk_cooldown_sec", 15)))

        # Setup GUI Grid Layout
        self.grid_columnconfigure(0, weight=3)  # Left controls
        self.grid_columnconfigure(1, weight=2)  # Right preview
        self.grid_rowconfigure(0, weight=1)
        self.grid_rowconfigure(1, weight=0)  # Status bar

        self._build_left_panel()
        self._build_right_panel()
        self._build_status_bar()

        # Attach real-time binding listeners
        for var in [
            self.var_client_id, self.var_game_name, self.var_details, self.var_state,
            self.var_large_img, self.var_large_txt, self.var_small_img,
            self.var_small_txt, self.var_btn1_lbl, self.var_btn1_url,
            self.var_btn2_lbl, self.var_btn2_url
        ]:
            var.trace_add("write", lambda *args: self.update_live_preview())

        self.var_show_timer.trace_add("write", lambda *args: self.update_live_preview())

        # Start live timer update loop
        self.update_timer_loop()
        self.update_live_preview()

    def load_config(self) -> dict:
        if os.path.exists(CONFIG_PATH):
            try:
                with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                    return json.load(f)
            except Exception as e:
                print(f"Error loading config: {e}")
        return {}

    def save_config(self):
        self.config["client_id"] = self.var_client_id.get().strip()
        self.config["game_name"] = self.var_game_name.get().strip()
        self.config["user_token"] = self.var_user_token.get().strip()
        self.config["device_platform"] = self.var_device_platform.get().strip()
        self.config["device_custom_text"] = self.var_device_custom_text.get().strip()

        self.config["voice_channel_id"] = self.var_voice_channel_id.get().strip()
        self.config["voice_deaf"] = self.var_voice_deaf.get()
        self.config["voice_mute"] = self.var_voice_mute.get()

        self.config["afk_message"] = self.var_afk_message.get().strip()
        self.config["afk_reply_dms"] = self.var_afk_dms.get()
        self.config["afk_reply_mentions"] = self.var_afk_mentions.get()
        try:
            self.config["afk_cooldown_sec"] = int(self.var_afk_cooldown.get().strip())
        except Exception:
            self.config["afk_cooldown_sec"] = 15

        self.config["current_presence"] = {
            "game_name": self.var_game_name.get().strip(),
            "details": self.var_details.get().strip(),
            "state": self.var_state.get().strip(),
            "large_image": self.var_large_img.get().strip(),
            "large_text": self.var_large_txt.get().strip(),
            "small_image": self.var_small_img.get().strip(),
            "small_text": self.var_small_txt.get().strip(),
            "show_timer": self.var_show_timer.get(),
            "button1_label": self.var_btn1_lbl.get().strip(),
            "button1_url": self.var_btn1_url.get().strip(),
            "button2_label": self.var_btn2_lbl.get().strip(),
            "button2_url": self.var_btn2_url.get().strip()
        }

        try:
            with open(CONFIG_PATH, "w", encoding="utf-8") as f:
                json.dump(self.config, f, indent=2, ensure_ascii=False)
            self.set_status("💾 Settings saved to config.json!", color="#23a55a")
        except Exception as e:
            self.set_status(f"❌ Error saving settings: {e}", color="#ed4245")

    def _build_left_panel(self):
        left_container = ctk.CTkScrollableFrame(self, fg_color="transparent")
        left_container.grid(row=0, column=0, sticky="nsew", padx=(16, 8), pady=16)

        # Header Title with Notification Center Button
        title_frame = ctk.CTkFrame(left_container, fg_color="transparent")
        title_frame.pack(fill="x", pady=(0, 14))

        title_left = ctk.CTkFrame(title_frame, fg_color="transparent")
        title_left.pack(side="left", fill="x", expand=True)

        main_header = ctk.CTkLabel(
            title_left,
            text="🎮 omar dev - Rich Presence, VR & PS5 Manager",
            font=ctk.CTkFont(family="Segoe UI", size=22, weight="bold"),
            text_color="#5865f2"
        )
        main_header.pack(anchor="w")

        sub_header = ctk.CTkLabel(
            title_left,
            text="Discord Rich Presence, VR Headset & PlayStation Badges, Voice 24/7 & AFK Manager",
            font=ctk.CTkFont(family="Segoe UI", size=12),
            text_color="#b5bac1"
        )
        sub_header.pack(anchor="w")

        self.btn_github = ctk.CTkButton(
            title_frame,
            text="🐙 GitHub (Omar-Dev)",
            command=lambda: webbrowser.open("https://github.com/omarsaber6545-hue"),
            fg_color="#24292e",
            hover_color="#2f363d",
            text_color="#ffffff",
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            height=34,
            corner_radius=8,
            border_width=1,
            border_color="#3c3e44"
        )
        self.btn_github.pack(side="right", padx=(8, 0), pady=4)

        self.btn_notif_center = ctk.CTkButton(
            title_frame,
            text="🔔 Notifications (0)",
            command=self.open_notification_center,
            fg_color="#2b2d31",
            hover_color="#35363c",
            text_color="#dbdee1",
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            height=34,
            corner_radius=8,
            border_width=1,
            border_color="#3c3e44"
        )
        self.btn_notif_center.pack(side="right", padx=(8, 0), pady=4)

        # Top Prominent Token Setup Section
        sec_token = self._create_card_section(left_container, "🔑 Account Token Setup")

        lbl_tok = ctk.CTkLabel(sec_token, text="Account / Bot Token:", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_tok.pack(anchor="w", padx=12, pady=(8, 2))

        tok_frame = ctk.CTkFrame(sec_token, fg_color="transparent")
        tok_frame.pack(fill="x", padx=12, pady=(0, 8))

        self.entry_token = ctk.CTkEntry(tok_frame, textvariable=self.var_user_token, show="*", placeholder_text="Paste your Account or Bot Token here...")
        self.entry_token.pack(side="left", fill="x", expand=True, padx=(0, 6))
        setup_entry_context_menu(self.entry_token)

        self.btn_toggle_token = ctk.CTkButton(
            tok_frame,
            text="👁️ Show",
            width=65,
            fg_color="#35363c",
            hover_color="#4e5058",
            command=self.toggle_token_visibility
        )
        self.btn_toggle_token.pack(side="right")

        btn_verify_tok = ctk.CTkButton(
            sec_token,
            text="🔐 Verify Account Token",
            fg_color="#35363c",
            hover_color="#4e5058",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.verify_account_token
        )
        btn_verify_tok.pack(fill="x", padx=12, pady=(0, 10))

        # Section: Device Platform Spoofer (VR & PlayStation Badges)
        sec_spoofer = self._create_card_section(left_container, "🥽 VR Headset & PlayStation Device Badges")

        lbl_plat = ctk.CTkLabel(sec_spoofer, text="Device Platform / نوع الجهاز المنصة:", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_plat.pack(anchor="w", padx=12, pady=(8, 2))

        plat_map = {
            "🥽 VR Headset (Meta Quest 3)": "vr",
            "🎮 PlayStation 5": "ps5",
            "📱 Mobile Phone (iPhone / Android)": "mobile",
            "🟩 Xbox Series X": "xbox"
        }

        current_key = self.var_device_platform.get()
        default_val = "🥽 VR Headset (Meta Quest 3)"
        for k, v in plat_map.items():
            if v == current_key:
                default_val = k
                break

        self.spoofer_dropdown = ctk.CTkOptionMenu(
            sec_spoofer,
            values=list(plat_map.keys()),
            fg_color="#5865f2",
            button_color="#4752c4",
            button_hover_color="#3c45a5",
            font=ctk.CTkFont(family="Segoe UI", size=13, weight="bold"),
            command=lambda selected: self.var_device_platform.set(plat_map.get(selected, "vr"))
        )
        self.spoofer_dropdown.set(default_val)
        self.spoofer_dropdown.pack(fill="x", padx=12, pady=(0, 8))

        lbl_sp_det = ctk.CTkLabel(sec_spoofer, text="Custom Status Details (تفاصيل نصية اختياري):", font=ctk.CTkFont(size=11))
        lbl_sp_det.pack(anchor="w", padx=12, pady=(0, 2))

        entry_sp_det = ctk.CTkEntry(sec_spoofer, textvariable=self.var_device_custom_text, placeholder_text="e.g. Exploring VR, Playing PS5...")
        entry_sp_det.pack(fill="x", padx=12, pady=(0, 8))
        setup_entry_context_menu(entry_sp_det)

        spoofer_actions_frame = ctk.CTkFrame(sec_spoofer, fg_color="transparent")
        spoofer_actions_frame.pack(fill="x", padx=12, pady=(4, 10))

        btn_start_spoofer = ctk.CTkButton(
            spoofer_actions_frame,
            text="🚀 Activate Device Badge",
            fg_color="#23a55a",
            hover_color="#1a7f45",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.start_device_spoofer
        )
        btn_start_spoofer.pack(side="left", expand=True, fill="x", padx=(0, 4))

        btn_stop_spoofer = ctk.CTkButton(
            spoofer_actions_frame,
            text="🛑 Stop Device Badge",
            fg_color="#ed4245",
            hover_color="#c03537",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.stop_device_spoofer
        )
        btn_stop_spoofer.pack(side="right", expand=True, fill="x", padx=(4, 0))

        # Game & Preset Selector Dropdown
        preset_frame = ctk.CTkFrame(left_container, fg_color="#2b2d31", corner_radius=10)
        preset_frame.pack(fill="x", pady=(0, 14), padx=2)

        preset_lbl = ctk.CTkLabel(
            preset_frame,
            text="🎮 Games Library Selector / Presets:",
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold")
        )
        preset_lbl.pack(anchor="w", padx=12, pady=(10, 4))

        preset_names = list(self.config.get("presets", {}).keys())
        if not preset_names:
            preset_names = [
                "💻 VS Code / Coding", "🎯 Valorant", "⛏️ Minecraft",
                "🏎️ Grand Theft Auto V", "🔫 Counter-Strike 2",
                "⚔️ League of Legends", "🧱 Roblox", "☕ AFK / Break"
            ]

        self.game_dropdown = ctk.CTkOptionMenu(
            preset_frame,
            values=preset_names,
            fg_color="#5865f2",
            button_color="#4752c4",
            button_hover_color="#3c45a5",
            font=ctk.CTkFont(family="Segoe UI", size=13, weight="bold"),
            dropdown_font=ctk.CTkFont(family="Segoe UI", size=12),
            command=self.apply_preset
        )
        self.game_dropdown.pack(fill="x", padx=12, pady=(0, 8))

        btn_randomizer = ctk.CTkButton(
            preset_frame,
            text="🎲 Roll Random Game, Status & Theme (اختيار عشوائي)",
            command=self.roll_randomizer,
            fg_color="#8a2be2",
            hover_color="#7b1fa2",
            text_color="#ffffff",
            font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
            height=34,
            corner_radius=8
        )
        btn_randomizer.pack(fill="x", padx=12, pady=(0, 12))

        # Section: AFK Auto-Responder System
        sec_afk = self._create_card_section(left_container, "☕ AFK Auto-Responder System")

        lbl_afk_msg = ctk.CTkLabel(sec_afk, text="AFK Auto-Reply Message:", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_afk_msg.pack(anchor="w", padx=12, pady=(8, 2))

        entry_afk_msg = ctk.CTkEntry(sec_afk, textvariable=self.var_afk_message, placeholder_text="I am currently AFK, I will reply as soon as I return! ☕")
        entry_afk_msg.pack(fill="x", padx=12, pady=(0, 8))
        setup_entry_context_menu(entry_afk_msg)

        opts_afk_frame = ctk.CTkFrame(sec_afk, fg_color="transparent")
        opts_afk_frame.pack(fill="x", padx=12, pady=(0, 8))

        sw_afk_dm = ctk.CTkSwitch(opts_afk_frame, text="📩 Auto-Reply in DMs", variable=self.var_afk_dms, progress_color="#23a55a")
        sw_afk_dm.pack(side="left", padx=(0, 12))

        sw_afk_men = ctk.CTkSwitch(opts_afk_frame, text="🏷️ Auto-Reply on Mentions", variable=self.var_afk_mentions, progress_color="#23a55a")
        sw_afk_men.pack(side="left")

        # Cooldown Anti-Ban Row
        cooldown_frame = ctk.CTkFrame(sec_afk, fg_color="transparent")
        cooldown_frame.pack(fill="x", padx=12, pady=(0, 8))

        lbl_cooldown = ctk.CTkLabel(
            cooldown_frame,
            text="⏱️ Cooldown (حماية من البان):",
            font=ctk.CTkFont(size=12, weight="bold")
        )
        lbl_cooldown.pack(side="left", padx=(0, 8))

        entry_cooldown = ctk.CTkEntry(
            cooldown_frame,
            textvariable=self.var_afk_cooldown,
            width=70,
            placeholder_text="15"
        )
        entry_cooldown.pack(side="left")
        setup_entry_context_menu(entry_cooldown)

        lbl_cooldown_unit = ctk.CTkLabel(
            cooldown_frame,
            text="ثانية (15s لحماية حسابك من الإغلاق)",
            font=ctk.CTkFont(size=11),
            text_color="#949ba4"
        )
        lbl_cooldown_unit.pack(side="left", padx=(6, 0))

        afk_actions_frame = ctk.CTkFrame(sec_afk, fg_color="transparent")
        afk_actions_frame.pack(fill="x", padx=12, pady=(4, 10))

        btn_start_afk = ctk.CTkButton(
            afk_actions_frame,
            text="🤖 Enable AFK Responder",
            fg_color="#23a55a",
            hover_color="#1a7f45",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.start_afk_responder
        )
        btn_start_afk.pack(side="left", expand=True, fill="x", padx=(0, 4))

        btn_stop_afk = ctk.CTkButton(
            afk_actions_frame,
            text="🛑 Disable AFK Responder",
            fg_color="#ed4245",
            hover_color="#c03537",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.stop_afk_responder
        )
        btn_stop_afk.pack(side="right", expand=True, fill="x", padx=(4, 0))

        # Section: Voice Channel 24/7 Stay
        sec_voice = self._create_card_section(left_container, "🎙️ 24/7 Voice Channel Stay Setup")

        lbl_vchan = ctk.CTkLabel(sec_voice, text="Voice Channel ID:", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_vchan.pack(anchor="w", padx=12, pady=(8, 2))

        entry_vchan = ctk.CTkEntry(sec_voice, textvariable=self.var_voice_channel_id, placeholder_text="e.g. 123456789012345678")
        entry_vchan.pack(fill="x", padx=12, pady=(0, 8))
        setup_entry_context_menu(entry_vchan)

        opts_voice_frame = ctk.CTkFrame(sec_voice, fg_color="transparent")
        opts_voice_frame.pack(fill="x", padx=12, pady=(0, 8))

        sw_mute = ctk.CTkSwitch(opts_voice_frame, text="🔇 Self Mute", variable=self.var_voice_mute, progress_color="#5865f2")
        sw_mute.pack(side="left", padx=(0, 12))

        sw_deaf = ctk.CTkSwitch(opts_voice_frame, text="🎧 Self Deaf", variable=self.var_voice_deaf, progress_color="#5865f2")
        sw_deaf.pack(side="left")

        v_actions_frame = ctk.CTkFrame(sec_voice, fg_color="transparent")
        v_actions_frame.pack(fill="x", padx=12, pady=(4, 10))

        btn_join_voice = ctk.CTkButton(
            v_actions_frame,
            text="🎙️ Connect 24/7 Voice",
            fg_color="#23a55a",
            hover_color="#1a7f45",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.start_voice_stay
        )
        btn_join_voice.pack(side="left", expand=True, fill="x", padx=(0, 4))

        btn_leave_voice = ctk.CTkButton(
            v_actions_frame,
            text="🛑 Disconnect Voice",
            fg_color="#ed4245",
            hover_color="#c03537",
            font=ctk.CTkFont(size=12, weight="bold"),
            command=self.stop_voice_stay
        )
        btn_leave_voice.pack(side="right", expand=True, fill="x", padx=(4, 0))

        # Section 1: Client ID & Game Name
        sec1 = self._create_card_section(left_container, "🆔 Discord Application Setup & Game Title")

        lbl_gname = ctk.CTkLabel(sec1, text="اسم اللعبة المعروضة (Game Title):", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_gname.pack(anchor="w", padx=12, pady=(8, 2))
        entry_gname = ctk.CTkEntry(sec1, textvariable=self.var_game_name, placeholder_text="e.g. Valorant, Minecraft, omar dev")
        entry_gname.pack(fill="x", padx=12, pady=(0, 8))
        setup_entry_context_menu(entry_gname)

        lbl_cid = ctk.CTkLabel(sec1, text="Application Client ID:", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_cid.pack(anchor="w", padx=12, pady=(0, 2))

        cid_frame = ctk.CTkFrame(sec1, fg_color="transparent")
        cid_frame.pack(fill="x", padx=12, pady=(0, 10))

        entry_cid = ctk.CTkEntry(cid_frame, textvariable=self.var_client_id, placeholder_text="e.g. 1529031652255203438")
        entry_cid.pack(side="left", fill="x", expand=True, padx=(0, 8))
        setup_entry_context_menu(entry_cid)

        btn_dev_portal = ctk.CTkButton(
            cid_frame,
            text="🌐 Developer Portal",
            width=130,
            fg_color="#35363c",
            hover_color="#4e5058",
            command=lambda: webbrowser.open("https://discord.com/developers/applications")
        )
        btn_dev_portal.pack(side="right")

        # Section 2: Details & State
        sec2 = self._create_card_section(left_container, "📝 Activity Text Details")

        lbl_det = ctk.CTkLabel(sec2, text="Details (تفاصيل السطر الأول):", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_det.pack(anchor="w", padx=12, pady=(8, 2))
        entry_det = ctk.CTkEntry(sec2, textvariable=self.var_details, placeholder_text="Writing code in VS Code")
        entry_det.pack(fill="x", padx=12, pady=(0, 8))
        setup_entry_context_menu(entry_det)

        lbl_st = ctk.CTkLabel(sec2, text="State (حالة السطر الثاني):", font=ctk.CTkFont(size=12, weight="bold"))
        lbl_st.pack(anchor="w", padx=12, pady=(0, 2))
        entry_st = ctk.CTkEntry(sec2, textvariable=self.var_state, placeholder_text="Developing awesome apps 🚀")
        entry_st.pack(fill="x", padx=12, pady=(0, 10))
        setup_entry_context_menu(entry_st)

        # Section 3: Art Assets / Images
        sec3 = self._create_card_section(left_container, "🖼️ Rich Presence Image Keys")

        row_img = ctk.CTkFrame(sec3, fg_color="transparent")
        row_img.pack(fill="x", padx=12, pady=(8, 10))

        # Large Image Box
        l_box = ctk.CTkFrame(row_img, fg_color="transparent")
        l_box.pack(side="left", fill="x", expand=True, padx=(0, 6))

        ctk.CTkLabel(l_box, text="Large Image Key:", font=ctk.CTkFont(size=11, weight="bold")).pack(anchor="w")
        e_l_img = ctk.CTkEntry(l_box, textvariable=self.var_large_img, placeholder_text="coding_icon")
        e_l_img.pack(fill="x", pady=(2, 4))
        setup_entry_context_menu(e_l_img)

        ctk.CTkLabel(l_box, text="Large Image Text (Tooltip):", font=ctk.CTkFont(size=10)).pack(anchor="w")
        e_l_txt = ctk.CTkEntry(l_box, textvariable=self.var_large_txt, placeholder_text="Hover text")
        e_l_txt.pack(fill="x", pady=(2, 0))
        setup_entry_context_menu(e_l_txt)

        # Small Image Box
        s_box = ctk.CTkFrame(row_img, fg_color="transparent")
        s_box.pack(side="right", fill="x", expand=True, padx=(6, 0))

        ctk.CTkLabel(s_box, text="Small Image Key:", font=ctk.CTkFont(size=11, weight="bold")).pack(anchor="w")
        e_s_img = ctk.CTkEntry(s_box, textvariable=self.var_small_img, placeholder_text="python_logo")
        e_s_img.pack(fill="x", pady=(2, 4))
        setup_entry_context_menu(e_s_img)

        ctk.CTkLabel(s_box, text="Small Image Text (Tooltip):", font=ctk.CTkFont(size=10)).pack(anchor="w")
        e_s_txt = ctk.CTkEntry(s_box, textvariable=self.var_small_txt, placeholder_text="Hover text")
        e_s_txt.pack(fill="x", pady=(2, 0))
        setup_entry_context_menu(e_s_txt)

        # Section 4: Timer & Interactive Buttons
        sec4 = self._create_card_section(left_container, "🔗 Interactive Buttons & Elapsed Timer")

        switch_timer = ctk.CTkSwitch(
            sec4,
            text="إظهار عدّاد الوقت (Show Elapsed Timer)",
            variable=self.var_show_timer,
            progress_color="#23a55a",
            font=ctk.CTkFont(size=12, weight="bold")
        )
        switch_timer.pack(anchor="w", padx=12, pady=(10, 10))

        # Button 1 setup
        b1_frame = ctk.CTkFrame(sec4, fg_color="transparent")
        b1_frame.pack(fill="x", padx=12, pady=(0, 6))

        ctk.CTkLabel(b1_frame, text="Button 1 Label:", font=ctk.CTkFont(size=11)).pack(side="left", padx=(0, 4))
        e_b1_lbl = ctk.CTkEntry(b1_frame, textvariable=self.var_btn1_lbl, width=130, placeholder_text="GitHub")
        e_b1_lbl.pack(side="left", padx=(0, 8))
        setup_entry_context_menu(e_b1_lbl)

        ctk.CTkLabel(b1_frame, text="URL:", font=ctk.CTkFont(size=11)).pack(side="left", padx=(0, 4))
        e_b1_url = ctk.CTkEntry(b1_frame, textvariable=self.var_btn1_url, placeholder_text="https://...")
        e_b1_url.pack(side="left", fill="x", expand=True)
        setup_entry_context_menu(e_b1_url)

        # Button 2 setup
        b2_frame = ctk.CTkFrame(sec4, fg_color="transparent")
        b2_frame.pack(fill="x", padx=12, pady=(0, 12))

        ctk.CTkLabel(b2_frame, text="Button 2 Label:", font=ctk.CTkFont(size=11)).pack(side="left", padx=(0, 4))
        e_b2_lbl = ctk.CTkEntry(b2_frame, textvariable=self.var_btn2_lbl, width=130, placeholder_text="Omar Dev Site")
        e_b2_lbl.pack(side="left", padx=(0, 8))
        setup_entry_context_menu(e_b2_lbl)

        ctk.CTkLabel(b2_frame, text="URL:", font=ctk.CTkFont(size=11)).pack(side="left", padx=(0, 4))
        e_b2_url = ctk.CTkEntry(b2_frame, textvariable=self.var_btn2_url, placeholder_text="https://...")
        e_b2_url.pack(side="left", fill="x", expand=True)
        setup_entry_context_menu(e_b2_url)

        # Action Buttons Grid
        action_frame = ctk.CTkFrame(left_container, fg_color="transparent")
        action_frame.pack(fill="x", pady=(4, 10))

        self.btn_start = ctk.CTkButton(
            action_frame,
            text="🚀 Start Rich Presence",
            font=ctk.CTkFont(size=13, weight="bold"),
            fg_color="#23a55a",
            hover_color="#1a7f45",
            height=40,
            command=self.start_presence
        )
        self.btn_start.pack(side="left", expand=True, fill="x", padx=(0, 4))

        self.btn_update = ctk.CTkButton(
            action_frame,
            text="🔄 Update",
            font=ctk.CTkFont(size=13, weight="bold"),
            fg_color="#5865f2",
            hover_color="#4752c4",
            height=40,
            command=self.update_presence_now
        )
        self.btn_update.pack(side="left", expand=True, fill="x", padx=4)

        self.btn_stop = ctk.CTkButton(
            action_frame,
            text="🛑 Stop",
            font=ctk.CTkFont(size=13, weight="bold"),
            fg_color="#ed4245",
            hover_color="#c03537",
            height=40,
            command=self.stop_presence
        )
        self.btn_stop.pack(side="left", expand=True, fill="x", padx=(4, 0))

        # Bottom secondary actions (Save & Background mode)
        sec_actions = ctk.CTkFrame(left_container, fg_color="transparent")
        sec_actions.pack(fill="x", pady=(0, 10))

        btn_save = ctk.CTkButton(
            sec_actions,
            text="💾 Save Config",
            fg_color="#35363c",
            hover_color="#4e5058",
            height=34,
            command=self.save_config
        )
        btn_save.pack(side="left", expand=True, fill="x", padx=(0, 4))

        btn_bg = ctk.CTkButton(
            sec_actions,
            text="🌙 Run in Background Mode",
            fg_color="#35363c",
            hover_color="#4e5058",
            height=34,
            command=self.run_background_mode
        )
        btn_bg.pack(side="right", expand=True, fill="x", padx=(4, 0))

    def _create_card_section(self, parent, title: str) -> ctk.CTkFrame:
        card = ctk.CTkFrame(parent, fg_color="#2b2d31", corner_radius=10)
        card.pack(fill="x", pady=(0, 14), padx=2)

        lbl = ctk.CTkLabel(
            card,
            text=title,
            font=ctk.CTkFont(family="Segoe UI", size=13, weight="bold"),
            text_color="#f2f3f5"
        )
        lbl.pack(anchor="w", padx=12, pady=(10, 4))
        return card

    def _build_right_panel(self):
        right_container = ctk.CTkFrame(self, fg_color="transparent")
        right_container.grid(row=0, column=1, sticky="nsew", padx=(8, 16), pady=16)

        # Discord Profile Outer Glass Frame
        user_card_frame = ctk.CTkFrame(
            right_container,
            fg_color="#18191c",
            corner_radius=16,
            border_width=1,
            border_color="#2b2d31"
        )
        user_card_frame.pack(fill="both", expand=True)

        # Modern Gradient Banner Header
        banner = ctk.CTkFrame(
            user_card_frame,
            fg_color="#5865f2",
            height=105,
            corner_radius=0
        )
        banner.pack(fill="x", side="top")

        # Avatar Outer Circle Frame with Online Status Dot
        avatar_outer = ctk.CTkFrame(
            user_card_frame,
            fg_color="#18191c",
            width=84,
            height=84,
            corner_radius=42
        )
        avatar_outer.place(x=18, y=60)

        avatar_inner = ctk.CTkFrame(
            avatar_outer,
            fg_color="#5865f2",
            width=76,
            height=76,
            corner_radius=38
        )
        avatar_inner.place(relx=0.5, rely=0.5, anchor="center")

        avatar_emoji = ctk.CTkLabel(
            avatar_inner,
            text="😎",
            font=ctk.CTkFont(size=38)
        )
        avatar_emoji.place(relx=0.5, rely=0.5, anchor="center")

        # Online Status Indicator Dot (🟢)
        status_dot = ctk.CTkFrame(
            avatar_outer,
            fg_color="#23a55a",
            width=20,
            height=20,
            corner_radius=10,
            border_width=3,
            border_color="#18191c"
        )
        status_dot.place(x=58, y=58)

        # Profile Information Section
        info_frame = ctk.CTkFrame(user_card_frame, fg_color="transparent")
        info_frame.pack(fill="x", padx=20, pady=(55, 6))

        user_name = ctk.CTkLabel(
            info_frame,
            text="omar dev",
            font=ctk.CTkFont(family="Segoe UI", size=20, weight="bold"),
            text_color="#ffffff"
        )
        user_name.pack(anchor="w")

        user_tag = ctk.CTkLabel(
            info_frame,
            text="omar_dev_official",
            font=ctk.CTkFont(family="Segoe UI", size=13),
            text_color="#949ba4"
        )
        user_tag.pack(anchor="w", pady=(0, 6))

        # Badges Bar (Active Developer, VR Headset, PlayStation, Nitro)
        badges_frame = ctk.CTkFrame(info_frame, fg_color="transparent")
        badges_frame.pack(anchor="w", pady=(0, 10))

        b1 = ctk.CTkLabel(badges_frame, text="👨‍💻 Active Dev", fg_color="#2b2d31", text_color="#5865f2", corner_radius=6, font=ctk.CTkFont(size=10, weight="bold"))
        b1.pack(side="left", padx=(0, 4), ipadx=6, ipady=2)

        b2 = ctk.CTkLabel(badges_frame, text="🥽 VR Quest", fg_color="#2b2d31", text_color="#fee75c", corner_radius=6, font=ctk.CTkFont(size=10, weight="bold"))
        b2.pack(side="left", padx=(0, 4), ipadx=6, ipady=2)

        b3 = ctk.CTkLabel(badges_frame, text="🎮 PS5", fg_color="#2b2d31", text_color="#00439c", corner_radius=6, font=ctk.CTkFont(size=10, weight="bold"))
        b3.pack(side="left", padx=(0, 4), ipadx=6, ipady=2)

        b4 = ctk.CTkLabel(badges_frame, text="💎 Nitro", fg_color="#2b2d31", text_color="#eb459e", corner_radius=6, font=ctk.CTkFont(size=10, weight="bold"))
        b4.pack(side="left", ipadx=6, ipady=2)

        # Live Card Preview Component
        self.preview_card = DiscordPreviewCard(user_card_frame)
        self.preview_card.pack(fill="both", expand=True, padx=16, pady=(0, 16))

    def _build_status_bar(self):
        self.status_frame = ctk.CTkFrame(self, fg_color="#111214", height=32, corner_radius=0)
        self.status_frame.grid(row=1, column=0, columnspan=2, sticky="ew")

        self.status_label = ctk.CTkLabel(
            self.status_frame,
            text="⚪ Discord RPC Status: Disconnected / Idle",
            font=ctk.CTkFont(family="Segoe UI", size=11),
            text_color="#949ba4"
        )
        self.status_label.pack(side="left", padx=16, pady=4)

    def set_status(self, text: str, color: str = "#949ba4"):
        self.status_label.configure(text=text, text_color=color)

        # Automatically broadcast to Notification Manager
        if text and hasattr(self, "notif_manager") and self.notif_manager:
            level = "info"
            title = "Status Update"
            if "❌" in text or "Error" in text or "error" in text or "failed" in text or color == "#ed4245":
                level = "error"
                title = "Error Alert ❌"
            elif "🟢" in text or "active" in text.lower() or "connected" in text.lower() or "saved" in text.lower() or color == "#23a55a":
                level = "success"
                title = "Success 🟢"
            elif "⚠️" in text or "warning" in text.lower() or color == "#fee75c":
                level = "warning"
                title = "Warning ⚠️"

            clean_text = text.replace("❌", "").replace("🟢", "").replace("⚠️", "").replace("⚪", "").replace("🔄", "").replace("🌙", "").replace("🔴", "").strip()
            if clean_text:
                self.notif_manager.notify(title=title, message=clean_text, level=level)

    def _on_new_notification(self, item: NotificationItem):
        """Called when a new notification is added."""
        self.show_toast(item)
        self._update_notification_badge()
        if self.notif_dialog and self.notif_dialog.winfo_exists():
            self.notif_dialog._refresh_list()

    def _update_notification_badge(self):
        """Updates the notification center bell button count and color."""
        if hasattr(self, "btn_notif_center") and self.btn_notif_center:
            unread = self.notif_manager.get_unread_count()
            total = len(self.notif_manager.notifications)
            if unread > 0:
                self.btn_notif_center.configure(
                    text=f"🔔 Notifications ({unread})",
                    fg_color="#ed4245",
                    hover_color="#c03537",
                    text_color="#ffffff"
                )
            else:
                self.btn_notif_center.configure(
                    text=f"🔔 Notifications ({total})",
                    fg_color="#2b2d31",
                    hover_color="#35363c",
                    text_color="#dbdee1"
                )

    def open_notification_center(self):
        """Opens the sleek Notification Center Dialog."""
        if self.notif_dialog and self.notif_dialog.winfo_exists():
            self.notif_dialog.lift()
            self.notif_dialog.focus()
            return

        self.notif_dialog = NotificationCenterDialog(self, self.notif_manager)

    def show_toast(self, item: NotificationItem):
        """Displays a sleek floating toast notification in the bottom-right corner."""
        try:
            self._dismiss_toast()

            self.toast_frame = ctk.CTkFrame(
                self,
                fg_color="#232428",
                corner_radius=10,
                border_width=1,
                border_color=item.color
            )
            self.toast_frame.place(relx=0.98, rely=0.94, anchor="se")

            top_row = ctk.CTkFrame(self.toast_frame, fg_color="transparent")
            top_row.pack(fill="x", padx=12, pady=(8, 2))

            icon_lbl = ctk.CTkLabel(
                top_row,
                text=f"{item.icon} {item.title}",
                font=ctk.CTkFont(family="Segoe UI", size=12, weight="bold"),
                text_color=item.color
            )
            icon_lbl.pack(side="left")

            btn_close = ctk.CTkButton(
                top_row,
                text="✕",
                width=20,
                height=20,
                fg_color="transparent",
                hover_color="#35363c",
                text_color="#949ba4",
                font=ctk.CTkFont(size=10, weight="bold"),
                command=self._dismiss_toast
            )
            btn_close.pack(side="right", padx=(8, 0))

            msg_lbl = ctk.CTkLabel(
                self.toast_frame,
                text=item.message,
                font=ctk.CTkFont(family="Segoe UI", size=11),
                text_color="#dbdee1",
                wraplength=300,
                justify="left"
            )
            msg_lbl.pack(anchor="w", padx=12, pady=(0, 10))

            if self.toast_job:
                self.after_cancel(self.toast_job)
            self.toast_job = self.after(4000, self._dismiss_toast)
        except Exception:
            pass

    def _dismiss_toast(self):
        try:
            if hasattr(self, "toast_frame") and self.toast_frame and self.toast_frame.winfo_exists():
                self.toast_frame.destroy()
                self.toast_frame = None
        except Exception:
            pass

    def toggle_token_visibility(self):
        if self.entry_token.cget("show") == "*":
            self.entry_token.configure(show="")
            self.btn_toggle_token.configure(text="🔒 Hide")
        else:
            self.entry_token.configure(show="*")
            self.btn_toggle_token.configure(text="👁️ Show")

    def verify_account_token(self):
        tok = self.var_user_token.get().strip()
        if not tok:
            self.set_status("❌ Please enter your Token first!", color="#ed4245")
            return

        self.set_status("🔄 Verifying Account Token...", color="#fee75c")

        def _verify_thread():
            valid, msg, data = verify_discord_token(tok)
            if valid:
                self.after(0, lambda: self.set_status(msg, color="#23a55a"))
            else:
                self.after(0, lambda: self.set_status(msg, color="#ed4245"))

        threading.Thread(target=_verify_thread, daemon=True).start()

    def start_device_spoofer(self):
        tok = self.var_user_token.get().strip()
        plat = self.var_device_platform.get().strip()
        cust_det = self.var_details.get().strip()
        cust_game = self.var_game_name.get().strip()
        cust_st = self.var_state.get().strip()

        ok, msg = self.spoofer_worker.start(
            token=tok,
            platform_mode=plat,
            custom_details=cust_det,
            custom_game_name=cust_game,
            custom_state=cust_st
        )

        if ok:
            self.set_status(msg, color="#23a55a")
        else:
            self.set_status(f"❌ {msg}", color="#ed4245")

    def stop_device_spoofer(self):
        self.spoofer_worker.stop()
        self.set_status("🔴 Device Badge Spoofer Stopped.", color="#ed4245")

    def start_voice_stay(self):
        tok = self.var_user_token.get().strip()
        vchan = self.var_voice_channel_id.get().strip()

        ok, msg = self.voice_worker.start(
            token=tok,
            channel_id=vchan,
            self_deaf=self.var_voice_deaf.get(),
            self_mute=self.var_voice_mute.get()
        )

        if ok:
            self.set_status(msg, color="#23a55a")
        else:
            self.set_status(f"❌ {msg}", color="#ed4245")

    def stop_voice_stay(self):
        self.voice_worker.stop()
        self.set_status("🔴 Voice Channel Stay Stopped.", color="#ed4245")

    def start_afk_responder(self):
        tok = self.var_user_token.get().strip()
        msg = self.var_afk_message.get().strip()
        try:
            cooldown = int(self.var_afk_cooldown.get().strip())
        except Exception:
            cooldown = 15

        ok, res_msg = self.afk_worker.start(
            token=tok,
            afk_message=msg,
            reply_dms=self.var_afk_dms.get(),
            reply_mentions=self.var_afk_mentions.get(),
            cooldown_sec=cooldown
        )

        if ok:
            self.set_status(res_msg, color="#23a55a")
        else:
            self.set_status(f"❌ {res_msg}", color="#ed4245")

    def stop_afk_responder(self):
        self.afk_worker.stop()
        self.set_status("🔴 AFK Auto-Responder Stopped.", color="#ed4245")

    def apply_preset(self, preset_name: str):
        presets = self.config.get("presets", {})
        if preset_name in presets:
            data = presets[preset_name]
            if "game_name" in data and data["game_name"]:
                self.var_game_name.set(data["game_name"])
            if "client_id" in data and data["client_id"]:
                self.var_client_id.set(data["client_id"])
            self.var_details.set(data.get("details", ""))
            self.var_state.set(data.get("state", ""))
            self.var_large_img.set(data.get("large_image", ""))
            self.var_large_txt.set(data.get("large_text", ""))
            self.var_small_img.set(data.get("small_image", ""))
            self.var_small_txt.set(data.get("small_text", ""))
            self.var_show_timer.set(data.get("show_timer", True))
            self.var_btn1_lbl.set(data.get("button1_label", ""))
            self.var_btn1_url.set(data.get("button1_url", ""))
            self.var_btn2_lbl.set(data.get("button2_label", ""))
            self.var_btn2_url.set(data.get("button2_url", ""))
            self.set_status(f"⚡ Applied game preset: {preset_name}", color="#5865f2")

    def roll_randomizer(self):
        """Randomly generates game, rich status text, and visual theme."""
        data = generate_random_presence()

        self.var_game_name.set(data["game_name"])
        self.var_details.set(data["details"])
        self.var_state.set(data["state"])
        self.var_large_img.set(data["large_image"])
        self.var_large_txt.set(data["large_text"])
        self.var_small_img.set(data["small_image"])
        self.var_small_txt.set(data["small_text"])
        self.var_device_custom_text.set(f"{data['game_name']} - {data['details']}")

        # Update preview card visual theme & accent colors
        if hasattr(self, "preview_card") and self.preview_card:
            self.preview_card.large_img_label.configure(text=data["theme_icon"])
            self.preview_card.large_img_frame.configure(fg_color=data["theme_accent"])
            self.preview_card.header_label.configure(
                text=f"⚡ THEME: {data['theme_name'].upper()}",
                text_color=data["theme_accent"]
            )
            self.preview_card.card_bg.configure(border_color=data["theme_accent"])

        # Immediately refresh live preview card
        self.update_live_preview()

        # Update live Discord Rich Presence
        if self.rpc_manager.is_connected:
            self.update_presence_now()
        elif self.spoofer_worker.is_running:
            self.start_device_spoofer()

        self.set_status(f"🎲 Randomizer Applied: {data['game_name']} ({data['theme_name']})", color="#23a55a")

    def update_live_preview(self):
        elapsed_sec = int(time.time() - self.start_timestamp)
        hours, remainder = divmod(elapsed_sec, 3600)
        minutes, seconds = divmod(remainder, 60)
        elapsed_str = f"{hours:02d}:{minutes:02d}:{seconds:02d}"

        self.preview_card.update_card(
            app_name=self.var_game_name.get(),
            details=self.var_details.get(),
            state=self.var_state.get(),
            large_image=self.var_large_img.get(),
            large_text=self.var_large_txt.get(),
            small_image=self.var_small_img.get(),
            small_text=self.var_small_txt.get(),
            show_timer=self.var_show_timer.get(),
            elapsed_str=elapsed_str,
            btn1_label=self.var_btn1_lbl.get(),
            btn1_url=self.var_btn1_url.get(),
            btn2_label=self.var_btn2_lbl.get(),
            btn2_url=self.var_btn2_url.get()
        )

    def update_timer_loop(self):
        self.update_live_preview()
        self.after(1000, self.update_timer_loop)

    def start_presence(self):
        cid = self.var_client_id.get().strip()
        if not cid or not cid.isdigit():
            self.set_status("❌ Client ID is invalid! Must contain digits only.", color="#ed4245")
            return

        if cid == "123456789012345678":
            self.set_status("⚠️ Warning: Please change Client ID to your Discord Application ID!", color="#fee75c")

        self.set_status("🔄 Connecting to Discord desktop client...", color="#fee75c")

        def _connect_thread():
            import asyncio
            try:
                asyncio.get_event_loop()
            except RuntimeError:
                asyncio.set_event_loop(asyncio.new_event_loop())

            success = self.rpc_manager.connect(cid)
            if success:
                self.start_timestamp = time.time()
                self._send_rpc_update()
                self.after(0, lambda: self.set_status("🟢 Rich Presence active on your Discord account!", color="#23a55a"))
            else:
                err_msg = self.rpc_manager.last_error
                self.after(0, lambda err=err_msg: self.set_status(f"❌ {err}", color="#ed4245"))

        threading.Thread(target=_connect_thread, daemon=True).start()

    def update_presence_now(self):
        if not self.rpc_manager.is_connected:
            self.set_status("⚠️ Discord is not connected. Click Start first!", color="#fee75c")
            return

        def _update_thread():
            success = self._send_rpc_update()
            if success:
                self.after(0, lambda: self.set_status("🟢 Rich Presence updated on Discord!", color="#23a55a"))
            else:
                err = self.rpc_manager.last_error
                self.after(0, lambda err=err_msg: self.set_status(f"❌ Update failed: {err}", color="#ed4245"))

        threading.Thread(target=_update_thread, daemon=True).start()

    def _send_rpc_update(self) -> bool:
        return self.rpc_manager.update_presence(
            details=self.var_details.get(),
            state=self.var_state.get(),
            large_image=self.var_large_img.get(),
            large_text=self.var_large_txt.get(),
            small_image=self.var_small_img.get(),
            small_text=self.var_small_txt.get(),
            show_timer=self.var_show_timer.get(),
            button1_label=self.var_btn1_lbl.get(),
            button1_url=self.var_btn1_url.get(),
            button2_label=self.var_btn2_lbl.get(),
            button2_url=self.var_btn2_url.get(),
            start_time=self.start_timestamp
        )

    def stop_presence(self):
        self.rpc_manager.disconnect()
        self.set_status("🔴 Discord Rich Presence stopped.", color="#ed4245")

    def run_background_mode(self):
        self.save_config()
        vbs_script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "start_background.vbs")

        if os.path.exists(vbs_script):
            subprocess.Popen(["wscript.exe", vbs_script], shell=True)
            self.set_status("🌙 Presence launched silently in background! Closing GUI...", color="#5865f2")
            self.after(1500, self.destroy)
        else:
            self.set_status("❌ start_background.vbs file not found!", color="#ed4245")


if __name__ == "__main__":
    app = OmarDevApp()
    app.mainloop()
