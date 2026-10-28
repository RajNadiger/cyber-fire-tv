<div align="center">

# Cyber Fire TV

A quiet home screen for a Fire TV Stick.

Press **Home**. Cyber opens.
Your apps stay. The ads go quiet.
Nothing is deleted.

<img src="docs/home.png" alt="Cyber home: greeting, clock, the app name, and two rows of apps" width="920">

<sub>Time on the left. The app you are on, in the middle. Your apps along the bottom. Pictures only.</sub>

</div>

---

## Put it on the stick

The stick and the computer sit on the **same Wi-Fi**.
One command does the rest.

```text
        YOUR COMPUTER                         FIRE TV STICK
   ┌──────────────────────┐              ┌──────────────────────┐
   │                      │              │                      │
   │    ./install.sh      │  same Wi-Fi  │   ADB debugging on   │
   │                      │ ───────────► │   Unknown sources    │
   │                      │              │                      │
   └──────────────────────┘              └──────────────────────┘
              │                                      │
              │   1   ads go quiet                   │
              │   2   Cyber is built                 │
              │   3   the night look is applied      │
              │   4   Home opens Cyber               ▼
                                                Cyber is on screen
```

### On the stick, four taps

1. **Settings → My Fire TV → Developer options**
2. Turn on **ADB debugging**
3. Turn on **Apps from Unknown Sources**
4. **Settings → My Fire TV → About → Network** — write down the address

### On the computer

You need [JDK 17](https://adoptium.net/). On a Mac: `brew install openjdk@17`.

```sh
git clone https://github.com/RajNadiger/cyber-fire-tv.git
cd cyber-fire-tv
./install.sh 192.168.1.50
```

`192.168.1.50` is only an example. Type the address from the stick.

When it finishes, press **Home**. Cyber is there.

```text
   you press Home
         │
         ▼
   Cyber                    clock, name, your apps
```

The **TV** button opens Amazon’s own home. It stays until you press Home again.

---

## What you see

```text
┌──────────────────────────────────────────────────────────────┐
│  Good afternoon                            weather · buttons │
│  4:19 PM                                                   │
│  Wednesday, October 28                                      │
│                                                              │
│  Netflix                    the name stays on this line      │
│  ━  ·  ·  ·  ·              the mark moves, the name does not│
│                                                              │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │      │ │      │ │      │ │      │ │      │ │      │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│   the glass row. Pictures only.                              │
│                                                              │
│   ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐            │
│   └────┘  └────┘  └────┘  └────┘  └────┘  └────┘            │
│   the next row, fully on screen. No names under the pictures│
└──────────────────────────────────────────────────────────────┘
```

| You look at | It means |
| --- | --- |
| Greeting, clock, date | The time of day. |
| Large name | The app you have selected. It stays in the same place as you move. Its colour follows that app. |
| Short line and dots | Where you are along the row. “Last opened” sits under the name and does not move it. |
| Glass row | Your main apps. Pictures only. Press the centre to open one. |
| The row under it | The next apps, fully on screen. No names under those pictures. |
| Weather | A small forecast at the top. Press it for the next hours. |
| Round buttons | Wi-Fi, apps, and settings. The one you are on shows its name. A Fire TV Stick has no HDMI ports, so there is no Inputs button. |
| **TV** | Amazon’s own home, until you press Home. |

---

## The remote

```text
                    ┌─────────┐
                    │  HOME   │     opens Cyber
                    └─────────┘

                        ↑
                   ←   OK   →      move, then open
                        ↓

                    ┌─────────┐
                    │  back   │
                    └─────────┘
```

```text
   left  /  right     along a row, and stop at the ends
   down               the same column, one row lower
   down again         Restore, Second screen, then App library
   up                 back the way you came
   up from the top    the round buttons. The chosen one shows its name
```

Home opens Cyber from Netflix, YouTube, or any other app.
After the stick sleeps and wakes, Home still opens Cyber.

The top row stays in place when you move to the row under it. One more step down keeps that row on screen and shows Restore and Second screen, each with its name.

---

## Make an app yours

Open **Settings**, then **Your apps**.

```text
   Your apps
   ┌─────────────────────┐     ┌─────────────────────┐
   │  Netflix            │     │                     │
   │  YouTube            │     │      the picture    │
   │  Prime Video     ▶  │ ──► │                     │
   │  Spotify            │     │   the name you set  │
   └─────────────────────┘     └─────────────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    ▼                 ▼                 ▼
              Change the name   Change the picture   a colour
```

| You want | What you do |
| --- | --- |
| A different name | **Change the name**, type it, press OK. |
| Your own picture | **Change the picture**, and choose a photo. |
| A colour for the name | Press one of the coloured dots. |
| The app’s own look back | **Put the app’s own look back**. |

Home updates when you leave Settings.

**Font**, on the settings page, changes the letters for the clock, the date, and the names, all together.

---

## Settings, in one look

The picture at the top of Settings is a small home screen. Move down the rows. The picture tells you what the highlighted row does.

```text
   Settings
   ┌────────────────────────────────────────┐
   │  Good night            9:41            │
   │  Netflix                               │
   │  (apps)                                │
   │  “this row changes the top row”        │
   ├────────────────────────────────────────┤
   │  Your apps          name, picture, colour
   │  Arrange the apps   how many, how many rows
   │  Wallpaper          one picture, or a folder
   │  Font               the letters
   │  Movement           on, or calmer
   └────────────────────────────────────────┘
```

**Wallpaper**

| Choice | What you see |
| --- | --- |
| Change automatically | The folder, about once a minute |
| Choose one | One picture, and it stays |
| Theme default | The built-in night scene |

Put `.jpg` or `.png` files here, with a file manager on the stick:

```text
Android/data/com.gathin.tvhome/files/mywallpapers
```

---

## Use the stick as a second screen

Open **Second screen** on the TV. The screen itself is the guide.

```text
   1  Same Wi-Fi          the stick and the computer, one network
   2  Start sharing      python3 tools/second-screen.py
   3  Press OK           the computer’s name appears, then the picture
```

The computer needs [ffmpeg](https://ffmpeg.org/). On a Mac: `brew install ffmpeg`.

Back leaves the picture and returns to the guide.

---

## What stays, what goes quiet

Nothing is deleted. A switched-off app can be turned back on.

```text
   GOES QUIET                           STAYS
   ┌────────────────────────┐           ┌────────────────────────┐
   │  ads                   │           │  Netflix, YouTube      │
   │  shopping              │           │  Prime Video, Spotify  │
   │  extra tips            │           │  Silk, Appstore        │
   │                        │           │  Alexa, casting        │
   │                        │           │  Live TV, updates      │
   │                        │           │  Amazon’s own home     │
   └────────────────────────┘           └────────────────────────┘
```

The list is `debloat/firetv.txt`.

Turn one package back on:

```sh
TV_HOST=192.168.1.50 tools/venv/bin/python -I tools/tv.py 'pm enable --user 0 com.amazon.bueller.music'
```

Undo the whole list. Amazon’s launcher is named so it is left on:

```sh
TV_HOST=192.168.1.50 tools/restore.sh debloat/firetv.txt com.amazon.tv.launcher
```

The **Restore** tile brings the earlier launcher back. It does not turn the quiet packages back on. Use `tools/restore.sh` for those.

---

## After the stick restarts

```text
   unplug  →  plug in  →  turn the TV on  →  press Home  →  Cyber
```

Fire TV will not let another app become the official Home app. Amazon’s launcher stays installed. Cyber watches the Home button and opens itself again.

---

## If something looks wrong

| What you see | What to do |
| --- | --- |
| The computer cannot find the stick | Same Wi-Fi. ADB debugging on. Use the address from About → Network. |
| Home still shows Amazon | Wait a few seconds, then press Home again. If it never changes, run `./install.sh` again. |
| An app is missing from the row | It was switched off, not deleted. Turn that package back on, or run `tools/restore.sh`. |

---

## Credits

The launcher, the tools, and the safety rules come from [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin, MIT.

Cyber Fire TV is a Fire TV home on that project: the night look, the Home button helper, weather, second screen, Your apps, and this guide.

The clock uses [Sora](https://fonts.google.com/specimen/Sora). The theme title face is [Chakra Petch](https://fonts.google.com/specimen/Chakra+Petch). Both are under the SIL Open Font License. See `LICENSE` and `NOTICE`.

<div align="center">

```text
   Home   →   Cyber   →   your apps
   TV     →   Amazon, until you press Home again
```

</div>
