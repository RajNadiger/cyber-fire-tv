<div align="center">

# Cyber Fire TV

A calm home screen for a Fire TV Stick.

Press **Home**. Cyber opens.
Your apps stay. The ads go quiet.
Nothing is deleted.

<img src="docs/home.png" alt="Cyber home screen: greeting, clock, the app name, and a row of apps" width="920">

<sub>Greeting and clock on the left. The app you are on, in the middle. Your apps along the bottom.</sub>

</div>

---

## What you see

```text
┌──────────────────────────────────────────────────────────────┐
│  Good morning                                                │
│  9:41          Wednesday                                     │
│                                                              │
│  Netflix                                                     │
│  ━  ·  ·  ·  ·                                               │
│                                                              │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐      │
│  │      │ │      │ │      │ │      │ │      │ │      │      │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘      │
│   your apps, in one quiet row                                │
│                                                              │
│                                    weather · buttons · TV    │
└──────────────────────────────────────────────────────────────┘
```

| You look at | It means |
| --- | --- |
| Greeting, clock, date | The time of day. The letters stay light. |
| Large name | The app you have selected. Its colour follows that app. |
| Short line and dots | Where you are along the row. |
| The row of apps | Left and right on the remote. Press the centre to open one. |
| Weather | A small live forecast at the top. Press it for the next hours. |
| Round buttons | Wi-Fi, inputs, apps, settings. |
| **TV** | Amazon’s own home. It stays until you press Home. |

---

## The remote

```text
                    ┌─────────┐
                    │  power  │
                    └─────────┘

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

Home opens Cyber, including when you were in Netflix, YouTube, or any other app.
After you tap the **TV** button, Amazon’s home stays up. Home brings Cyber back.

Fire TV sometimes holds Amazon’s screen for a few seconds. That pause is Fire TV. Cyber comes forward as soon as the stick allows it.

```text
   you press Home
         │
         ▼
   Amazon’s screen          a short pause, on some sticks
         │
         ▼
   Cyber                    your clock, your apps
```

---

## Make an app yours

Open **Settings**, then **Your apps**.

```text
   Your apps
   ┌─────────────────────┐     ┌─────────────────────┐
   │  Netflix            │     │                     │
   │  YouTube            │     │      the tile       │
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

A picture can also be a jpg or png you copy into the stick’s `icons` folder, or into `mywallpapers`. Then open **Change the picture** again.

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
        │
        ├──  night.jpg
        ├──  rain.png
        └──  desk.webp
```

---

## Use the stick as a second screen

Same Wi-Fi. On the stick, open **Second screen**. On the computer, in this folder:

```sh
python3 tools/second-screen.py
```

The computer needs [ffmpeg](https://ffmpeg.org/). On a Mac: `brew install ffmpeg`.

The stick finds the computer and shows its screen.

---

## Put it on a stick

One command. Apps stay installed. Ads are only switched off.

```text
   your computer                         Fire TV Stick
   ┌─────────────────────┐               ┌─────────────────────┐
   │  this folder        │   same Wi-Fi  │  ADB debugging on   │
   │  ./install.sh       │ ────────────► │  Unknown sources on │
   └─────────────────────┘               └─────────────────────┘
          │                                         │
          │   1  ads go quiet                       │
          │   2  Cyber is built                     │
          │   3  the night look is applied          │
          │   4  Home opens Cyber                   ▼
                                               Cyber is on screen
```

### On the stick

1. **Settings → My Fire TV → Developer options**
2. Turn on **ADB debugging**
3. Turn on **Apps from Unknown Sources**
4. Note the address under **Settings → My Fire TV → About → Network**

### On the computer

You need [JDK 17](https://adoptium.net/). On a Mac: `brew install openjdk@17`.

```sh
git clone https://github.com/RajNadiger/cyber-fire-tv.git
cd cyber-fire-tv
./install.sh 192.168.1.50
```

`192.168.1.50` is an example. Use the address from the stick.

When it finishes, Home opens Cyber. The **TV** button opens Amazon’s home.

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

---

## After the stick restarts

```text
   unplug  →  plug in  →  turn the TV on  →  Cyber
```

Fire TV will not let another app become the official Home app. Amazon’s launcher stays installed. Cyber watches the Home button and opens itself again.

---

## If something looks wrong

| What you see | What to do |
| --- | --- |
| The computer cannot find the stick | Same Wi-Fi. ADB debugging on. Use the address from About → Network. |
| Home still shows Amazon | Wait a few seconds. If it never changes, run `./install.sh` again. |
| An app is missing from the row | It was switched off, not deleted. Turn that package back on, or run `tools/restore.sh`. |
| You want the earlier launcher | Open the **Restore** tile. That does not turn the quiet packages back on. Use `tools/restore.sh` for those. |

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
