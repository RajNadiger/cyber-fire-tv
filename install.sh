#!/usr/bin/env bash
# One command for a Fire TV Stick. Nothing is uninstalled.
# Usage: ./install.sh 192.168.1.50
set -euo pipefail
cd "$(dirname "$0")"
HOST="${1:?Usage: ./install.sh <fire-tv-ip>}"
export TV_HOST="$HOST"

if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17 ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17
fi
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
if ! command -v java >/dev/null && [ -z "${JAVA_HOME:-}" ]; then
  echo "Install JDK 17 first (brew install openjdk@17)." >&2
  exit 1
fi

python3 -m venv tools/venv
tools/venv/bin/pip install -q -r tools/requirements.txt
PY=tools/venv/bin/python

echo "1/4  Turning off ads and telemetry. Apps stay installed."
"$PY" -I tools/tv.py 'echo connected' >/dev/null
tools/disable.sh debloat/firetv.txt || echo "Some packages were left on. That is safe. Continuing."

echo "2/4  Building Cyber"
( cd launcher && ./gradlew assembleRelease --console=plain )
APK=launcher/app/build/outputs/apk/release/app-release.apk
"$PY" -I tools/b64push.py "$APK" /data/local/tmp/cyber.apk
"$PY" -I tools/tv.py 'pm install -r /data/local/tmp/cyber.apk; rm -f /data/local/tmp/cyber.apk'

echo "3/4  Applying the look"
tools/theme.sh themes/cyberpunk
if [ -f themes/cyberpunk/hero.ttf ]; then
  "$PY" -I tools/b64push.py themes/cyberpunk/hero.ttf /sdcard/Android/data/com.gathin.tvhome/files/hero.ttf
fi
"$PY" -I tools/tv.py 'mkdir -p /sdcard/Android/data/com.gathin.tvhome/files/mywallpapers'

echo "4/4  Home button and startup"
"$PY" -I tools/tv.py 'pm grant com.gathin.tvhome android.permission.WRITE_SECURE_SETTINGS; pm grant com.gathin.tvhome android.permission.READ_LOGS; settings put secure enabled_accessibility_services ""; settings put secure accessibility_enabled 0; settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService; settings put secure accessibility_enabled 1; am start -n com.gathin.tvhome/.MainActivity'
"$PY" -I tools/b64push.py tools/cyberhome.sh /data/local/tmp/cyberhome.sh
"$PY" -I tools/tv.py 'chmod 755 /data/local/tmp/cyberhome.sh'
echo
echo "Cyber is on the Fire TV. Home opens it from any app. The TV icon at the top right opens Amazon home."
echo "Add pictures to Android/data/com.gathin.tvhome/files/mywallpapers"
