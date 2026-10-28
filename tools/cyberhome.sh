#!/system/bin/sh
# The stick's shell is allowed to open Cyber. The app's own start is ignored
# while Fire TV is handling the Home button.
while true; do
  logcat -b system -T 1 | grep -m 1 handleShortPressOnHome >/dev/null
  am start -n com.gathin.tvhome/.MainActivity >/dev/null 2>&1
done
