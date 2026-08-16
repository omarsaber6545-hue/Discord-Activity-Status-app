import time
from datetime import datetime
from typing import List, Dict, Callable, Optional


class NotificationItem:
    """Represents a single in-app notification event."""

    def __init__(self, title: str, message: str, level: str = "info"):
        self.id = str(time.time_ns())
        self.timestamp = datetime.now().strftime("%H:%M:%S")
        self.title = title
        self.message = message
        self.level = level.lower() if level.lower() in ("info", "success", "warning", "error") else "info"
        self.read = False

    @property
    def icon(self) -> str:
        icons = {
            "success": "🟢",
            "error": "❌",
            "warning": "⚠️",
            "info": "ℹ️"
        }
        return icons.get(self.level, "🔔")

    @property
    def color(self) -> str:
        colors = {
            "success": "#23a55a",
            "error": "#ed4245",
            "warning": "#fee75c",
            "info": "#5865f2"
        }
        return colors.get(self.level, "#5865f2")


class NotificationManager:
    """Central notification management engine with history logging and event broadcasting."""

    def __init__(self):
        self.notifications: List[NotificationItem] = []
        self.on_notification_added: Optional[Callable[[NotificationItem], None]] = None
        self.on_state_changed: Optional[Callable[[], None]] = None

    def notify(self, title: str, message: str, level: str = "info") -> NotificationItem:
        """Adds a new notification and broadcasts to UI handlers."""
        item = NotificationItem(title=title, message=message, level=level)
        self.notifications.insert(0, item)  # Newest first

        if len(self.notifications) > 100:  # Keep last 100 entries
            self.notifications.pop()

        if self.on_notification_added:
            try:
                self.on_notification_added(item)
            except Exception:
                pass

        if self.on_state_changed:
            try:
                self.on_state_changed()
            except Exception:
                pass

        return item

    def get_unread_count(self) -> int:
        return sum(1 for item in self.notifications if not item.read)

    def mark_all_read(self):
        for item in self.notifications:
            item.read = True
        if self.on_state_changed:
            try:
                self.on_state_changed()
            except Exception:
                pass

    def clear_all(self):
        self.notifications.clear()
        if self.on_state_changed:
            try:
                self.on_state_changed()
            except Exception:
                pass

    def get_filtered(self, filter_level: Optional[str] = None) -> List[NotificationItem]:
        if not filter_level or filter_level.lower() == "all":
            return list(self.notifications)
        return [item for item in self.notifications if item.level == filter_level.lower()]
