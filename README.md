<div align="center">

# Cyber Fire TV

A quiet home screen for a Fire TV Stick.

Ads and shopping stay installed, but switched off.
The Home button opens Cyber.
Amazon’s own home is one tap away, on the TV icon.

<img src="docs/home.png" alt="Cyber home: night wallpaper, large clock, app name, and a dock of apps" width="920">

<sub>Night wallpaper · clock and greeting · app name in that app’s colour · dock along the bottom</sub>

</div>

---

## The screen, drawn

```text
┌────────────────────────────────────────────────────────────┐
│  GOOD NIGHT                              10 : 41 PM    TV  │
│  Wednesday, October 28                                     │
│                                                            │
│                         Netflix                            │
│                         ●   ○   ○   ○                      │
│                                                            │
│  (N) (YT) (Prime) (Spotify) (Silk) (Store)          Style  │
└────────────────────────────────────────────────────────────┘
     move left and right on the dock          Style    TV = Amazon
```

| On the screen | What it is |
| --- | --- |
| Clock, greeting, date | Top left. The face stays clean and light. |
| Big name | The app you are on. The colour follows that app. |
| Dots | How far you have moved along the row. |
| Dock | Your apps. Left and right on the remote. |
| **Style** | Layout, motion, wallpaper, and a live preview. |
| **TV** | Amazon’s home. It stays there until you press Home. |

The dock starts as Netflix, YouTube, Prime Video, Spotify, Silk, and the Appstore, when those apps are on the stick. Change the list in `themes/cyberpunk/config.json`.

---

## The remote

```text
                 ┌───────────┐
                 │   power   │
                 └───────────┘
                 ┌───────────┐
                 │   HOME    │  ── opens Cyber
                 └───────────┘
                      ( ↑ )
              ( ← )  ( OK )  ( → )   move on the dock, open an app
                      ( ↓ )
                 ┌───────────┐
                 │   back    │
                 └───────────┘
```

Home opens Cyber, including after the stick has been fully off.
After you tap the **TV** icon, Amazon’s home stays up. Home brings Cyber back.

Fire TV holds its own home for a short time. Amazon’s screen can stay for a few seconds. Cyber replaces it as soon as Fire TV allows another app in front.

```text
   you press Home
         │
         ▼
   ┌──────────┐
   │  Amazon  │   Fire TV keeps this for a few seconds
   └──────────┘
         │
         ▼
   ┌──────────┐
   │  Cyber   │   your home, clock, dock
   └──────────┘
```

---

## Install

One command. Nothing is deleted.

```text
  your computer                         Fire TV Stick
  ┌─────────────────────┐               ┌─────────────────────┐
  │  clone this repo    │   same Wi-Fi  │  ADB debugging on   │
  │  ./install.sh IP    │ ────────────► │  Unknown sources on │
  └─────────────────────┘               └─────────────────────┘
         │                                        │
         │  1  switch off ads                     │
         │  2  build Cyber                        │
         │  3  apply the night look               │
         │  4  point Home at Cyber                ▼
                                              Cyber is on screen
```

### On the stick

1. **Settings → My Fire TV → Developer options**
2. Turn on **ADB debugging**
3. Turn on **Apps from Unknown Sources**
4. Find the address under **Settings → My Fire TV → About → Network**

### On the computer

You need [JDK 17](https://adoptium.net/) (`brew install openjdk@17` on a Mac).

```sh
git clone https://github.com/RajNadiger/cyber-fire-tv.git
cd cyber-fire-tv
./install.sh 192.168.1.50
```

Use the stick’s address. `192.168.1.50` is only an example.

That command switches off the ad packages, builds Cyber, installs it, applies the night theme, and turns on the Home helper.

When it finishes, Home opens Cyber. The TV icon at the top right opens Amazon’s home. Put your own pictures in `Android/data/com.gathin.tvhome/files/mywallpapers`.

---

## What stays, what goes quiet

Packages are switched off for the current user. They stay installed. Turn any of them back on later.

```text
   SWITCHED OFF                         LEFT ON
   ┌────────────────────────┐           ┌────────────────────────┐
   │  metrics               │           │  Netflix               │
   │  ads                   │           │  YouTube               │
   │  shopping              │           │  Prime Video           │
   │  extra Amazon music    │           │  Spotify               │
   │  tips and notices      │           │  Silk                  │
   └────────────────────────┘           │  Appstore              │
                                        │  Alexa                 │
                                        │  casting               │
                                        │  Live TV               │
                                        │  firmware updates      │
                                        │  Amazon’s own home     │
                                        └────────────────────────┘
```

The list is `debloat/firetv.txt`.

Put one package back:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'pm enable --user 0 com.amazon.bueller.music'
```

Undo the whole list. Amazon’s launcher is named on purpose so it is not touched:

```sh
TV_HOST=192.168.1.50 tools/restore.sh debloat/firetv.txt com.amazon.tv.launcher
```

---

## Style, wallpaper, restore

Open **Style** on the dock. The preview on that screen updates as you change a row.

```text
   Style
   ┌──────────────────────────────────────┐
   │  preview                             │
   │  GOOD NIGHT          10:41           │
   │  Netflix                             │
   │  (apps)                              │
   ├──────────────────────────────────────┤
   │  Look          layout, motion, type  │
   │  Wallpaper     see the three choices │
   └──────────────────────────────────────┘
```

**Wallpaper**

| Choice | What you see |
| --- | --- |
| Change automatically | Walks the folder, about once a minute |
| Choose one | Keeps a single picture |
| Theme default | The built-in night scene |

Copy `.jpg` or `.png` files into the folder with a file manager on the stick:

```text
Android/data/com.gathin.tvhome/files/mywallpapers
        │
        ├──  night-city.jpg
        ├──  rain.png
        └──  desk.webp
```

**Restore**

Cyber adds a **Restore** tile in your apps. Open it when you want the launcher from before these Fire TV changes. It clears Cyber’s settings and wallpapers, then installs the original CleanYourTV build that was saved on the stick. It does not turn the switched-off packages back on. Use `tools/restore.sh` for that.

---

## After a restart

```text
   stick unplugged  →  plug in  →  TV on  →  Cyber
```

Fire OS will not let another app become the official Home app. Amazon’s launcher stays installed. Cyber watches the Home button and Amazon’s home, then opens itself again. The same helper runs after a full shutdown, on whichever TV the stick is plugged into.

---

## If something looks wrong

| What you see | What to do |
| --- | --- |
| Install cannot find the stick | Same Wi-Fi. ADB debugging on. Use the address from About → Network. |
| Home still shows Amazon | Wait a few seconds. Cyber replaces it. If it never does, run `./install.sh` again so the Home helper is turned back on. |
| A needed app disappeared from the dock | It was switched off, not deleted. Enable that package, or run `tools/restore.sh`. |
| You want Amazon’s look back for good | Open the **Restore** tile, then run `tools/restore.sh` for the packages. |

---

## Credits

The launcher, the disable tools, and the safety rules come from [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin, MIT. Cyber Fire TV is a Fire TV setup on that project: the night theme, the Home helper for Fire OS, the wallpaper folder, the settings preview, and the Restore tile.

The title face is [Chakra Petch](https://fonts.google.com/specimen/Chakra+Petch), SIL Open Font License. The clock face is Outfit, also OFL. See `LICENSE` and `NOTICE`.

<div align="center">

```text
        Home  →  Cyber  →  your apps
                 TV icon → Amazon, until Home
```

</div>
