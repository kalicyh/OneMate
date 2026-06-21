# OneMate

[中文](README.md)

OneMate is an LSPosed/libxposed module for Samsung Keyboard. Its main goal is to restore the missing **Text editing** toolbar feature in recent Samsung Keyboard versions, with extra controls for hidden keyboard settings, toolbar badges, and keyboard configuration backup/restore.

The module currently targets `com.samsung.android.honeyboard` and uses libxposed API 102.

## Features

- Restore the old Samsung Keyboard `Text editing` toolbar entry.
- Provide cursor movement, selection, select all, cut, copy, paste, enter, and backspace controls.
- Force-enable hidden Samsung Keyboard settings that may be gated by region, device, Knox, AI service availability, or hardware capability.
- Disable toolbar new-feature badges/red dots.
- Save and restore OneMate preferences and Samsung Keyboard configuration with root.
- Miuix-style app UI with a status card, keyboard restart button, test input box, theme settings, and about page.

## Requirements

- Android API 36+.
- Samsung Keyboard package: `com.samsung.android.honeyboard`.
- LSPosed or a compatible framework with libxposed API 102 support.
- OneMate must be enabled for the Samsung Keyboard scope.
- Root is only required for force-stopping Samsung Keyboard and backing up/restoring Samsung Keyboard data.

## Usage

1. Install the OneMate APK.
2. Enable OneMate in LSPosed and make sure Samsung Keyboard is selected in the module scope.
3. Open OneMate and enable Text editing, badge control, or hidden settings as needed.
4. Restart Samsung Keyboard, or tap the force-stop button on the OneMate home page.
5. Tap the test input box to bring Samsung Keyboard back and verify the feature.

If hidden settings or config restore do not take effect immediately, restart Samsung Keyboard first. If it still fails, clear Samsung Keyboard data and restore the saved configuration.

## Build

Required local tools:

- JDK 17.
- Android SDK Platform `android-37.0`.
- Android Build Tools `37.0.0`.

Build and test:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug --no-configuration-cache
```

Build release:

```bash
./gradlew :app:assembleRelease --no-configuration-cache
```

Local release builds are unsigned unless signing environment variables are provided.

## Release

GitHub Actions publishes releases when a `v*` tag is pushed.

Required GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Create the base64 keystore value on macOS:

```bash
base64 -i keystore.jks | tr -d '\n' | pbcopy
```

Publish a version:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Do not commit keystore files.

## Notes

- Samsung Keyboard internals are obfuscated, so compatibility may vary between versions.
- Config backup/restore writes Samsung Keyboard data with root. Prefer restoring on the same device, package, and a close keyboard version.
- If Samsung Keyboard crashes after enabling the module, disable the module, clear Samsung Keyboard data, then re-enable features one by one.
