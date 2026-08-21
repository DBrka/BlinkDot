# BlinkDot

A blinking notification dot for the lock screen of a Samsung Galaxy S22 Ultra.

When a message arrives while your phone is locked or the screen is off, BlinkDot
turns the screen on, paints it completely black, and blinks a small coloured dot
in the corner you chose. Each app gets its own colour.

**APK:** [`BlinkDot-v1.1.apk`](BlinkDot-v1.1.apk) (4.5 MB, signed, installs on Android 8.1+)

---

## How it works, and one honest limitation

No third-party app can draw on a screen that is genuinely off, and Samsung's
Always-On Display is closed to third-party apps. So BlinkDot does the next best
thing: it turns the screen on and shows a **pure black screen at minimum
brightness with only the dot on it**.

On the S22 Ultra's AMOLED panel, black pixels are physically switched off, so
what you see is a black phone with one blinking dot — visually identical to a
notification LED, and very cheap on battery.

---

## Installing

1. On the phone, open
   `https://github.com/DBrka/BlinkDot/raw/main/BlinkDot-v1.1.apk`
   in Chrome, or copy the APK across by USB.
2. Open it with **My Files** and tap **Install**.
3. Android will warn about installing from an unknown source — allow it for the
   app you are installing from (My Files / Chrome). This is normal for an app
   that does not come from the Play Store.

### If it says "App not installed"

The APK is fine; the phone is refusing the sideload. In order of likelihood:

1. **Play Protect** — Play Store → your avatar → Play Protect → ⚙ → turn off
   "Scan apps with Play Protect", install, then turn it back on.
   *(This is the one that actually blocked the first install here.)*
2. **Samsung Auto Blocker** — Settings → Security and privacy → Auto Blocker →
   off. One UI turns this on by default and it blocks all sideloading.
3. **Install unknown apps** — Settings → Apps → ⋮ → Special access →
   Install unknown apps → grant it to Chrome *and* My Files separately.
4. **A leftover copy** signed with a different key — uninstall it first.

### After installing an update

Check that **Notification access** is still granted. Reinstalling an APK can
unbind the notification listener, and a listener that is not bound sees nothing.
If the dot stops appearing after an update, toggle it off and on again in
Settings → Notifications → Special access → Notification access.

## Setting it up (first launch)

Open BlinkDot. The **Setup** card has four rows. Grant them top to bottom:

| Permission | Why it is needed |
|---|---|
| **Notification access** | So BlinkDot can see which app a message came from. Required. |
| **Display over other apps** | So the dot can appear while the phone is locked. Required. |
| **Unrestricted battery** | Stops One UI from putting BlinkDot to sleep. Strongly recommended. |
| **Post notifications** | Only used as a backup trigger. Optional. |

Then tap **Choose apps and colours**, turn on WhatsApp, Viber, Messenger,
Messages, or whatever else you want, and tap the coloured circle next to an app
to change its colour.

### One extra Samsung step, worth doing

Settings → Battery → Background usage limits → make sure BlinkDot is **not** in
"Sleeping apps" or "Deep sleeping apps". One UI is aggressive about this, and a
sleeping app cannot see your notifications.

### The one-time "Viewing full screen" banner

The very first time the black screen appears, Android shows a one-time
"Viewing full screen" banner in the top-left — right where the dot is. Tap
**Got it** and it never comes back. To get it out of the way before it can
interrupt a real message, tap **Preview the dot** in the app once during setup
and dismiss it there.

---

## Settings

- **Corner** — TL / TR / BL / BR. Default is top-left, as requested. (Handy on
  the S22 Ultra, whose camera cut-out is centred, so the top-left is clear.)
- **Dot size** — 6–44 dp.
- **Distance from corner** — 4–90 dp, to clear the curved edge of the screen.
- **Flash length** and **Pause between flashes** — the blink rhythm.
- **Screen brightness while blinking** — defaults to 4 %. Low is good: the
  screen is black anyway, and this is what keeps battery use down.
- **Blink until I open the message** — on by default. The dot keeps blinking
  and comes back every time you lock the phone, until the message is actually
  read. Glancing at the screen without reading does not clear it; opening the
  message does. Turn this off to get the **Give up after** timer instead.
- **Give up after** — only shown when the option above is off. Stops blinking
  after N minutes. Set to 0 to blink until you unlock.
- **Soft fade** — pulse in and out instead of a hard on/off blink.
- **Glow halo** — a soft halo around the dot.
- **Preview the dot** — shows the black screen with your settings for 12 seconds.

## Behaviour

- Blinks **only** when the screen is off or the phone is locked. If you are
  using the phone, nothing happens.
- **Touch, or a press of the power button, puts the dot away.** It will not
  immediately reappear — but with "blink until I open the message" on, it comes
  back the next time you wake and re-lock the phone with something still unread.
- If two apps are waiting, the dot **alternates between their colours**.
- **Touch the screen** to dismiss and go back to the normal lock screen.
- Unlocking the phone dismisses it automatically.
- Ongoing notifications (music players, downloads, sync bars) and group summary
  headers are ignored, so you only get a dot for real messages.
- SMS is covered automatically — it arrives as a notification from your
  messaging app, so no SMS permission is needed.

---

## Rebuilding from source

Requires the Android SDK (platform 34) and JDK 17.

```bash
cd D:/BlinkDot && ./gradlew assembleRelease
```

Output lands in `app/build/outputs/apk/release/app-release.apk`.

### Signing

The signing key is deliberately **not in this repository**. Anyone holding it
could sign an update that Android would happily install over BlinkDot as if it
came from you.

To produce a signed release build you need two files in the project root, both
git-ignored:

- `blinkdot.jks` — the keystore itself
- `keystore.properties`:

  ```properties
  storeFile=blinkdot.jks
  storePassword=<your password>
  keyAlias=blinkdot
  keyPassword=<your password>
  ```

Without them the project still builds — the release APK just comes out
unsigned, and `./gradlew assembleDebug` works as normal.

**Keep the original `blinkdot.jks` safe.** Android only installs an update over
an existing app when both are signed with the same key. Lose it and the only
way to update BlinkDot on your phone is to uninstall it first.

To generate a fresh keystore:

```bash
keytool -genkeypair -v -keystore blinkdot.jks -alias blinkdot -keyalg RSA -keysize 2048 -validity 10950
```

### Project layout

| File | Role |
|---|---|
| `NotifListener.kt` | Reads every notification, filters it, decides whether to blink. |
| `Blink.kt` | Launches the blink screen — directly, or via a full-screen intent as fallback. |
| `BlinkActivity.kt` | The black lock-screen surface. Shows over the keyguard and turns the screen on. |
| `DotView.kt` | Draws and animates the dot. |
| `Prefs.kt` | All settings and per-app colours. |
| `AppListActivity.kt` | App picker with per-app colour swatches. |
| `ColorPicker.kt` | 24 presets plus RGB sliders. |

BlinkDot logs to logcat under the tag `BlinkDot`, which is useful if a
particular app is not triggering it:

```bash
adb logcat -s BlinkDot:D
```
