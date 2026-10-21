---
name: run-on-device
description: Use when asked to run, launch, screenshot, or visually verify the app, or after a UI change that needs confirming in the running app. Selects a physical Android device or emulator, installs and launches with the android CLI, then inspects the UI with android layout / android screen. Also covers KMP desktop and JVM server run targets.
---

# Run on device

Confirm a change in the running app, not just in tests. Read the
`android-cli` skill's `references/interact.md` before driving any Android UI.

## 1. Pick the run target

| Shape | Target |
|---|---|
| Android / KMP `androidTarget` | Android device (below) |
| KMP desktop (`jvm("desktop")` + Compose) | `./gradlew :composeApp:run` (via `gradle-run`); screenshot the window is not available — ask the user to confirm visuals |
| KMP iOS | Not possible on Windows. Say so; verify shared code with `jvmTest` instead |
| JVM server (Ktor/Spring) | Start with `./gradlew :server:run` in the background, hit the changed endpoint with `curl`, stop it after |

## 2. Select an Android device

```sh
adb devices -l
```

Classify each line:

- `emulator-NNNN` → emulator. Anything else in state `device` → **physical device**.
- `unauthorized` → ask the user to accept the RSA prompt on the phone. `offline` → ask them to replug / re-enable USB debugging. Do not proceed with that device.

Choice rules:

1. If `ANDROID_SERIAL` is set or the user named a device, use it.
2. Exactly one usable device → use it.
3. Several → prefer a physical device when the change involves camera, sensors, biometrics, push, Bluetooth, performance, or real-world rendering; otherwise ask which one (one question, list serial + model).
4. None → `android emulator list`, then `android emulator start <name>` (one AVD → use it; several → ask; none → offer `android emulator create`). If the user wants a physical device instead, tell them: enable Developer options → USB debugging, connect, accept the prompt; or for Wi-Fi: `adb pair <ip:port>` then `adb connect <ip:port>`.

Pass the serial explicitly everywhere: `--device <serial>` for `android`,
`-s <serial>` for `adb`.

## 3. Physical-device safety

A real phone holds the user's data and other apps.

- Never `adb uninstall`, `pm clear`, `rm`, or `reboot` without asking (settings.json enforces "ask").
- Install with the normal update path; if install fails with signature mismatch, stop and ask — fixing it requires uninstalling, which wipes app data.
- Wake/unlock before inspecting: `adb -s <serial> shell input keyevent KEYCODE_WAKEUP`. If the lock screen is secure, ask the user to unlock; never try to enter a PIN.
- Screenshots may contain personal notifications. Keep them under `build/device-shots/` (git-ignored) and don't upload them anywhere.
- Note device differences when reporting: `adb -s <serial> shell getprop ro.product.model` and `ro.build.version.sdk`.

## 4. Install and launch

Build the APK with `build-and-verify` (`:app:assembleDebug` through `gradle-run`),
then install the built artifact so Gradle never runs outside the wrapper:

```sh
android run --device <serial> --apks app/build/outputs/apk/debug/app-debug.apk --activity <package>.MainActivity
```

`--activity` is required with `--apks`; without it the APK is installed but not
launched. Several app modules or flavors → `android describe` to find the APK.

## 5. Verify

1. `android layout --device <serial>` — primary check: expected text, ids, states.
2. `android screen capture --device <serial> -o build/device-shots/<step>.png` — then **look at the PNG** before continuing.
3. Drive the flow with `adb -s <serial> shell input tap|text|swipe|keyevent` using `center` coordinates from the layout, or `android screen resolve` for annotated shots.
4. Crash or blank screen → `adb -s <serial> logcat -d -t 300 *:E` and report the top app frame.
5. Blank/black screen with no crash → before blaming the app, launch a system
   app (`adb -s <serial> shell am start -a android.settings.SETTINGS`) and capture
   it. If it is black too, the emulator compositor is stuck (often a lingering
   `Splash Screen` layer in `dumpsys SurfaceFlinger --list`): `android emulator stop`
   then `android emulator start --cold <avd>`. Report it as an environment issue,
   not an app result.

## 6. Report

```
Device: Pixel 8 (physical, API 35) | emulator-5554 <avd name> (API 36)
Flow:   open Login → enter bad password → tap Sign in
Saw:    error text "Wrong password" under field ✅ ; button stays enabled ✅
Shot:   build/device-shots/login-error.png
Issues: none | <what differs from expected>
```
