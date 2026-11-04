# Privacy

Level asks for nothing, stores nothing about you, and has no way to reach the network.

That is the whole policy. The rest of this page is the evidence for it, because a privacy
policy that cannot be checked is just a promise.

## No permissions

`app/src/main/AndroidManifest.xml` declares **no `uses-permission` at all**. Android does
not gate the motion sensors behind a permission, so a level has nothing to ask for.

There is no `INTERNET` permission. Without it Android will not let the app open a network
connection, so nothing it knows could leave the phone even by accident.

Upstream declares `WAKE_LOCK` to hold the screen on. This does not. A level has to stay
readable while it rests against the thing being leveled, so the app keeps the screen on
while it is in front, with the window flag `FLAG_KEEP_SCREEN_ON`. That needs no permission,
and Android drops it by itself the moment the app is no longer in front.

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

<!-- shared-about privacy section -->
## Who we are

Level is made by wander wildwood, an independent developer: one person, not a company. wander wildwood is the data controller for anything the app itself handles, and that is very little. Level keeps what you put in it on your phone and sends nothing to the developer, so the developer holds no data about you.

## How to contact us

Write to email@wanderthe.dev about your data, this policy or anything else. The same address is in the app, under About.

## Your rights

If you are in the EU, the EEA or the UK, the GDPR gives you the right to:

- see the personal data held about you (access)
- have it corrected (rectification) or deleted (erasure)
- limit how it is used (restriction)
- object to its use (objection)
- get a copy to take elsewhere (portability)
- withdraw your consent at any time, where something rests on it
- complain to a data protection supervisory authority, for example the one where you live

Similar laws elsewhere, such as the CCPA in California, give you the right to know, delete and correct, and to opt out of the sale or sharing of your data. Nothing from Level is sold or shared.

Since Level keeps what it stores only on your phone, you can exercise most of these rights yourself: clear the app's data in Android's settings, or uninstall the app, which deletes all of it. For anything you cannot do yourself, write to the address above.
<!-- /shared-about privacy section -->
