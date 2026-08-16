import os
import sys
import json
import time
import argparse
import logging

# Ensure UTF-8 encoding for console output and handle pythonw devnull streams
if sys.stdout is None:
    sys.stdout = open(os.devnull, "w", encoding="utf-8")
else:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

if sys.stderr is None:
    sys.stderr = open(os.devnull, "w", encoding="utf-8")
else:
    try:
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

from rpc_manager import DiscordRPCManager

CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.json")


def load_config() -> dict:
    """Loads configuration from config.json."""
    if os.path.exists(CONFIG_PATH):
        try:
            with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception as e:
            logging.error(f"Failed to read config file: {e}")
    return {}


def main():
    parser = argparse.ArgumentParser(description="Omar Dev - Discord Rich Presence CLI Launcher")
    parser.add_argument("--preset", type=str, help="Specific preset name to load (e.g. Coding, Gaming, AFK)")
    parser.add_argument("--client-id", type=str, help="Override Discord Application Client ID")
    parser.add_argument("--background", action="store_true", help="Run in silent background loop")
    args = parser.parse_args()

    config = load_config()
    client_id = args.client_id or config.get("client_id", "1529031652255203438")

    presence_data = config.get("current_presence", {})

    if args.preset and "presets" in config and args.preset in config["presets"]:
        presence_data = config["presets"][args.preset]
        logging.info(f"Loaded preset: {args.preset}")

    print("==================================================")
    print(" 🎮 omar dev - Discord Rich Presence Launcher")
    print("==================================================")
    print(f"📌 Client ID    : {client_id}")
    print(f"📝 Details      : {presence_data.get('details', '')}")
    print(f"📌 State        : {presence_data.get('state', '')}")
    print(f"🖼️ Large Image  : {presence_data.get('large_image', '')}")
    print(f"⏱️ Elapsed Timer: {'Enabled' if presence_data.get('show_timer', True) else 'Disabled'}")
    print("==================================================")

    rpc = DiscordRPCManager(client_id)
    if not rpc.connect():
        print(f"❌ Error connecting to Discord: {rpc.last_error}")
        print("💡 Ensure Discord desktop app is running and your Client ID is valid.")
        if not args.background:
            input("\nPress Enter to exit...")
        sys.exit(1)

    print("✅ Discord Rich Presence connected & active!")

    success = rpc.update_presence(
        details=presence_data.get("details", ""),
        state=presence_data.get("state", ""),
        large_image=presence_data.get("large_image", ""),
        large_text=presence_data.get("large_text", ""),
        small_image=presence_data.get("small_image", ""),
        small_text=presence_data.get("small_text", ""),
        show_timer=presence_data.get("show_timer", True),
        button1_label=presence_data.get("button1_label", ""),
        button1_url=presence_data.get("button1_url", ""),
        button2_label=presence_data.get("button2_label", ""),
        button2_url=presence_data.get("button2_url", ""),
    )

    if not success:
        print(f"⚠️ Warning: Could not update presence card: {rpc.last_error}")

    print("\n🚀 Status is active! Keep this window running (or run in background mode).")
    print("Press Ctrl+C to stop.")

    try:
        while True:
            time.sleep(15)
            # Re-assert or pulse RPC update
            rpc.update_presence(
                details=presence_data.get("details", ""),
                state=presence_data.get("state", ""),
                large_image=presence_data.get("large_image", ""),
                large_text=presence_data.get("large_text", ""),
                small_image=presence_data.get("small_image", ""),
                small_text=presence_data.get("small_text", ""),
                show_timer=presence_data.get("show_timer", True),
                button1_label=presence_data.get("button1_label", ""),
                button1_url=presence_data.get("button1_url", ""),
                button2_label=presence_data.get("button2_label", ""),
                button2_url=presence_data.get("button2_url", ""),
                start_time=rpc.start_time
            )
    except KeyboardInterrupt:
        print("\nStopping Discord Rich Presence...")
        rpc.disconnect()
        print("👋 Goodbye!")


if __name__ == "__main__":
    main()
