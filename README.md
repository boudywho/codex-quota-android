<div align="center">

# ⚡ Codex Quota Monitor for Android

**A modern, privacy-first Android client to monitor real-time OpenAI Codex and ChatGPT Plus/Team quotas, rate limits, and subscription renewal schedules across multiple accounts.**

[![Release](https://img.shields.io/badge/Release-v1.1.5-10B981?style=for-the-badge&logo=github)](https://github.com/boudywho/codex-quota-android/releases)
[![Android](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20M3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

<br/>

<p align="center">
  <img src="docs/banner.png" width="100%" alt="Codex Quota Monitor Showcase Banner" />
</p>

</div>

---

## ✨ Features

### 🔄 Real-Time Quota & Rolling Limits
- **Live Subscriber Rate Limits**: Directly queries OpenAI's real-time rate limit engines to display accurate `Used %` and `Remaining %` on active ChatGPT Plus, Team, and Enterprise accounts.
- **Dynamic Reset Countdown**: Real-time timers showing the exact duration until rate limit windows roll over.
- **Platform API Keys**: Tracks token-per-minute (TPM) and request-per-minute (RPM) limits with detailed metric meters.

### 🔑 Seamless Device Code Authorization
- **Official Codex Flow**: Sign in using standard OAuth device authorization (`https://auth.openai.com/codex/device`) with automatic PKCE exchange.
- **Zero API Key Leakage**: No need to generate raw API secrets or manage complex tokens manually.

### 📅 Subscription Expiration & Renewal Tracking
- **Renewal Timers**: View exact renewal and expiry dates, billing cycles (monthly/yearly), and auto-renewal statuses.
- **Account Metadata**: View account registration dates, linked emails, and workspace IDs.

### 📱 Android Home-Screen Widgets
- **Jetpack Glance**: Modern, responsive Material 3 home-screen widgets providing real-time quota gauges and reset timers directly on your launcher.

### 🛡️ Enterprise-Grade Security & Privacy
- **Hardware-Backed Keystore**: All credentials and tokens are encrypted on-device via **AES-256-GCM** backed by the Android Keystore system.
- **Zero Telemetry**: Completely tracker-free with zero third-party analytics or remote logging.
- **Direct HTTPS**: App communicates exclusively with OpenAI servers with no intermediate proxies.

---

## 📥 Download & Installation

Grab the latest APK directly from the GitHub Releases:

👉 **[Download Latest APK (GitHub Releases)](https://github.com/boudywho/codex-quota-android/releases/latest)**

1. Download `codex-quota-vX.X.X.apk` onto your Android device.
2. Open the file to install (allow "Install from Unknown Sources" if prompted).
3. Launch **Codex Quotas** and tap `+` to add your first account!

---

## 🏗️ Architecture & Technology Stack

Codex Quota is built according to official **Modern Android Architecture** and Clean Architecture guidelines:

```
app/
├── auth/          # OAuth PKCE Device Code Flow & JWT Token Parsers
├── data/
│   ├── local/     # Room Database, Encrypted Keystore Storage, Preferences DataStore
│   └── remote/    # OkHttp / Kotlinx Serialization, OpenAI Wham & Platform APIs
├── domain/        # Use Cases, Repository Interfaces, Models & Enums
├── ui/
│   ├── components/# Circular gauges, Progress bars, Metric cards
│   ├── feature/   # Dashboard, Account Detail, Add Account, Settings, About
│   ├── navigation/# Jetpack Compose Type-Safe Navigation
│   └── theme/     # Material 3 Color Schemes, Typography & Shapes
├── widget/        # Jetpack Glance Home Screen Widgets
└── worker/        # AndroidX WorkManager for background synchronization & alerts
```

- **UI Framework**: Jetpack Compose with Material Design 3 and Dynamic Colors (Material You)
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Dependency Inversion**: Factory-based ViewModels and Clean Repository Pattern
- **Local Persistence**: Room SQLite + AndroidX Security EncryptedSharedPreferences (AES-256-GCM)
- **Background Tasks**: AndroidX WorkManager periodic sync & system notifications
- **Home Widgets**: Jetpack Glance AppWidget

---

## 🛠️ Building From Source

### Prerequisites
- JDK 17 or JDK 21
- Android Studio Ladybug (2024.2.1+) or Gradle 8.11+
- Android SDK Platform 35

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/boudywho/codex-quota-android.git
cd codex-quota-android

# Run unit tests
./gradlew test

# Assemble Debug APK
./gradlew assembleDebug

# Output APK location:
# app/build/outputs/apk/debug/app-debug.apk
```

Debug builds and unit tests do not require release signing credentials.

### Release Signing

Release APKs must be signed with the production certificate. Create an ignored
`signing.properties` file in the repository root with this shape (use your real
values locally; do not commit the file or keystore):

```properties
storeFile=path/to/release.keystore
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

`storeFile` may be an absolute path or a path relative to the repository root.
Each property can be overridden by its corresponding environment variable:

| `signing.properties` | Environment variable |
| :--- | :--- |
| `storeFile` | `ANDROID_KEYSTORE_FILE` |
| `storePassword` | `ANDROID_KEYSTORE_PASSWORD` |
| `keyAlias` | `ANDROID_KEY_ALIAS` |
| `keyPassword` | `ANDROID_KEY_PASSWORD` |

Build a production APK with `./gradlew assembleRelease`. The build fails with a
clear error if any signing value is missing or the configured keystore file is
unavailable; it never falls back to the Android debug certificate.

> **One-time signing migration:** APKs previously distributed by CI were signed
> with the Android debug certificate. Android cannot upgrade those installations
> with the new production certificate, so users of an old APK must uninstall it
> once before installing the first production-signed release. After that migration,
> future releases signed with the same production certificate can update each other.

### CI and Releases

Pull requests targeting `main` and pushes to `main` run unit tests, Android lint,
and a debug APK build. To enable tagged releases, configure these GitHub Actions
repository secrets:

- `ANDROID_KEYSTORE_BASE64`: the production keystore file encoded as base64
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password
- `ANDROID_KEY_ALIAS`: the signing key alias
- `ANDROID_KEY_PASSWORD`: the signing key password

To publish a release, first set `versionName` and `versionCode` in
`app/build.gradle.kts`, then push a matching `v*` tag (for example, `v1.2.0` for
`versionName = "1.2.0"`). The tag workflow verifies the match before handling
signing material, decodes the keystore only into the runner's temporary directory,
builds the signed release APK, and attaches `codex-quota-vX.X.X.apk` to the GitHub
Release.

---

## 🔒 Security & Privacy

| Principle | Implementation |
| :--- | :--- |
| **Credential Storage** | Android Keystore Hardware-Backed AES-256-GCM |
| **Network Security** | Direct TLS 1.3 HTTPS exclusively to `openai.com` / `chatgpt.com` |
| **Data Collection** | **Zero**. No analytics, no crashlytics, no tracking cookies. |
| **Cloud Backups** | Excluded from Android Auto-Backup (`allowBackup=false`) |

---

## ⚖️ Legal Disclaimer

Codex Quota is an independent, open-source project developed for developers and power users to monitor their personal API usage and subscription windows.

* This application is **not** created, affiliated with, authorized, maintained, sponsored, or endorsed by OpenAI, Inc.
* OpenAI, ChatGPT, Codex, and GPT are trademarks or registered trademarks of OpenAI, Inc.

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for more information.
