# Privacy

Level asks for nothing, stores nothing about you, and has no way to reach the network.

That is the whole policy. The rest of this page is the evidence for it, because a privacy
policy that cannot be checked is just a promise.

## No permissions

`app/src/main/AndroidManifest.xml` declares **no `uses-permission` at all**. Android does
not gate the motion sensors behind a permission, so a level has nothing to ask for.

There is no `INTERNET` permission. Without it Android will not let the app open a network
connection, so nothing it knows could leave the phone even by accident.

Upstream declares `WAKE_LOCK` to hold the screen on. This does not: on E Ink the picture
stays up with the backlight off, so a wake lock would cost battery for no visible effect.

## What is stored

Four things, all in `SharedPreferences`, all visible in `level/Preferences.kt`:

- whether you read it in degrees or percent,
- whether the orientation is locked,
- whether it beeps when level,
- your calibration offsets — three small numbers for each of the five ways of holding the
  phone.

Nothing else. No history of readings, no log, no file.

## No analytics

No crash reporting, no telemetry, no advertising identifier, no third-party SDK. The
dependency list in `app/build.gradle.kts` is AndroidX, Jetpack Compose and Mudita's MMD
component library, and nothing else.

## Checking for yourself

```
aapt2 dump badging app-release.apk | grep uses-permission
```

That prints every permission the built app actually carries. It prints exactly one line:

```
uses-permission: name='com.wanderwildwood.mizumori.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
```

That one is not mine and is not a permission in the sense you care about. AndroidX defines
it automatically for every app; it is a signature-level permission scoped to this package,
which only this app can hold, and it exists so that a broadcast receiver registered at
runtime is not exported to other apps. It grants access to nothing.

No `INTERNET`, no location, no storage, no sensors — because sensors need none.
