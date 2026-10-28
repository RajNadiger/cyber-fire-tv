#!/usr/bin/env python3
"""Put this computer's screen on a Fire TV running Cyber, on the same Wi-Fi.

On the Fire TV, open the Second screen app. Then, on this computer:

    python3 tools/second-screen.py

Needs ffmpeg. Mac: brew install ffmpeg. Linux: apt install ffmpeg. Windows: install ffmpeg and put it on PATH.
"""
import http.server
import platform
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
from pathlib import Path

PORT = 8765
DISCOVER = 47865


def capture_args():
    system = platform.system()
    if system == "Darwin":
        listed = subprocess.run(
            ["ffmpeg", "-hide_banner", "-f", "avfoundation", "-list_devices", "true", "-i", ""],
            capture_output=True, text=True)
        index = "1"
        for line in (listed.stderr or "").splitlines():
            if "Capture screen" in line and "[" in line:
                index = line.split("[")[-1].split("]")[0]
                break
        return ["-f", "avfoundation", "-framerate", "30", "-i", index + ":none"]
    if system == "Linux":
        display = __import__("os").environ.get("DISPLAY", ":0.0")
        return ["-f", "x11grab", "-framerate", "30", "-video_size", "1920x1080", "-i", display]
    if system == "Windows":
        return ["-f", "gdigrab", "-framerate", "30", "-i", "desktop"]
    sys.exit("This system is not supported. Use Mac, Linux, or Windows.")


def main():
    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg is not installed.")
    folder = Path(tempfile.mkdtemp(prefix="cyber-screen-"))
    ffmpeg = subprocess.Popen(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", *capture_args(),
         "-an", "-c:v", "libx264", "-preset", "veryfast", "-tune", "zerolatency",
         "-pix_fmt", "yuv420p", "-g", "30", "-bf", "0", "-vf", "scale=1280:-2",
         "-f", "hls", "-hls_time", "1", "-hls_list_size", "4",
         "-hls_flags", "delete_segments+append_list+omit_endlist",
         "-hls_segment_filename", str(folder / "s%03d.ts"),
         str(folder / "index.m3u8")])

    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=str(folder), **kwargs)

        def log_message(self, fmt, *args):
            return

    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    notice = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    notice.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    message = f"CYBER-SCREEN {PORT} {socket.gethostname()}".encode()
    print(f"Sharing this screen. Open Second screen on the Fire TV. ({socket.gethostname()})")
    print("Press Ctrl-C to stop.")
    try:
        threading.Thread(target=server.serve_forever, daemon=True).start()
        while ffmpeg.poll() is None:
            notice.sendto(message, ("255.255.255.255", DISCOVER))
            try:
                ffmpeg.wait(timeout=1)
            except subprocess.TimeoutExpired:
                pass
    except KeyboardInterrupt:
        pass
    finally:
        ffmpeg.terminate()
        server.shutdown()


if __name__ == "__main__":
    main()
