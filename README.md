<div align="center">

<img src="docs/wordmark.svg" alt="Cyber Fire TV. Press Home. Cyber opens. Your apps stay. Nothing is deleted." width="880">

<br>

<img src="docs/home-card.png" alt="The Cyber home screen: greeting, clock, the app name, and two rows of apps" width="880">

<br>

<img src="docs/caption.svg" alt="Time on the left. The name stays in one place. Apps are pictures only. Home opens this screen." width="880">

<br>

<img src="docs/steps.svg" alt="Four steps. On the stick, turn on ADB debugging and Unknown Sources. Write down the network address. On the computer, install JDK 17 and run the three lines. Press Home." width="880">

</div>

### The three lines

`192.168.1.50` is an example. Use the address from the stick.

```sh
git clone https://github.com/RajNadiger/cyber-fire-tv.git
cd cyber-fire-tv
./install.sh 192.168.1.50
```

<div align="center">

<img src="docs/remote.svg" alt="Home opens Cyber. Arrows move. Select opens an app. Back leaves a screen." width="880">

<br><br>

<img src="docs/after.svg" alt="Settings can change an app name, picture, and colour. Second screen uses the same Wi-Fi. If it fails, check Wi-Fi, ADB, and the address, then run install again." width="880">

</div>

### Second screen

On the stick, open **Second screen**. On the computer, the same Wi-Fi, with [ffmpeg](https://ffmpeg.org/) installed:

```sh
python3 tools/second-screen.py
```

On a Mac, one time: `brew install ffmpeg`. Press Select when the computer’s name appears. Back returns to the guide.

### Wallpaper

In Settings, choose one picture, or a folder that changes about once a minute. Put `.jpg` or `.png` files here:

```text
Android/data/com.gathin.tvhome/files/mywallpapers
```

### Bring an app back

Nothing is deleted. Ads go quiet. Netflix, YouTube, Prime Video, Spotify, Silk, the Appstore, Alexa, and Amazon’s own home stay.

```sh
TV_HOST=192.168.1.50 tools/restore.sh debloat/firetv.txt com.amazon.tv.launcher
```

The **Restore** tile brings the earlier launcher back. It leaves the quiet apps quiet. The line above turns those back on too.

### Credits

The launcher, the tools, and the safety rules come from [CleanYourTV](https://github.com/Gathin23/cleanyourtv) by Gathin, MIT.

The clock uses [Sora](https://fonts.google.com/specimen/Sora). The theme title face is [Chakra Petch](https://fonts.google.com/specimen/Chakra+Petch). Both are under the SIL Open Font License. See `LICENSE` and `NOTICE`.
