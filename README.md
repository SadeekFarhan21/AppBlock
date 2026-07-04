# AppBlock

[![Build](https://github.com/SadeekFarhan21/AppBlock/actions/workflows/build.yml/badge.svg)](https://github.com/SadeekFarhan21/AppBlock/actions/workflows/build.yml)

A free, fully local Android app + website blocker. No accounts, no premium tier, no ads, no data collection — the whole APK is about 1 MB.

## Features

- **Quick Block** — one tap blocks every app and site from all your schedules for 15m / 30m / 1h / 2h.
- **Schedules** — named blocking profiles (emoji, days of week, time window or all-day, overnight windows supported), each with its own app list and website list. Toggle on/off per schedule.
- **Templates** — Focus, Study Time, Wind Down, and Digital Detox presets to start from.
- **Strict Mode** — time-lock your setup (1h to 7d). While locked you can't disable/edit/delete schedules or stop Quick Block early. The lock can be extended but never shortened.
- **Insights** — screen time today, 7-day trend chart, top apps (needs the Usage Access permission), plus counters for blocked attempts today and all-time.
- **Block screen** — shows what was blocked, which schedule blocked it, a rotating quote, and today's blocked count.

## How it works

- **App blocking** — an accessibility service watches window changes; when a blocked app comes to the foreground, a full-screen block takes over and sends you home.
- **Website blocking** — the same service reads the URL bar of popular browsers (Chrome, Firefox, Brave, Edge, Opera, Samsung Internet, Vivaldi, Kiwi, DuckDuckGo) after navigation (not while typing), matching domains and subdomains.

Everything is stored locally in SharedPreferences. The app requests no network permission at all.

## Build

Requires JDK 17+ and the Android SDK. Create a `local.properties` in the project root pointing at your SDK:

```properties
sdk.dir=/path/to/your/Android/sdk
```

Then:

```sh
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk` (minified; debug-signed for direct sideloading — set up your own signing config for distribution).

## Install & setup

1. `adb install -r app/build/outputs/apk/release/app-release.apk` (or copy the APK to the phone and open it).
2. Enable the **AppBlock accessibility service** — the app's banner links straight to the setting. Blocking does nothing without it. On Samsung, temporarily disable Auto Blocker to sideload.
3. Optionally grant **Usage Access** from the Insights tab for screen-time stats.

## Project layout

- `MainActivity.kt` — bottom-nav shell (Blocking / Strict Mode / Insights)
- `BlockingScreen.kt` — Quick Block, schedule list, templates
- `ScheduleEditorScreen.kt` — schedule editor (days, times, app picker, site list)
- `StrictModeScreen.kt`, `InsightsScreen.kt`
- `AppBlockerService.kt` — the accessibility service doing the actual blocking
- `BlockedActivity.kt` — full-screen block overlay
- `BlockRepository.kt` — persistence + strict-mode enforcement; `Schedule.kt` — model
- `Theme.kt` — black/graphite Material 3 theme

## Notes / limitations

- Website blocking only covers the browsers listed in `AppBlockerService.kt`; block any other browser as an app instead. In-app WebViews aren't detected (that would need a local VPN service).
- Strict Mode locks changes inside the app; turning off the accessibility service or uninstalling still bypasses it. This is a self-control tool, not parental control.

## License

[MIT](LICENSE)
