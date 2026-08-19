# Codex Quota Monitor

**Codex Quota** is a native, modern Android application built for developers, team leads, and power users to monitor real-time token usage, request rate limits, and quota reset countdowns across multiple OpenAI Codex and developer accounts.

---

## Features

- **Multi-Account Management**: Add, name, color-code, reorder, and monitor unlimited OpenAI accounts (Personal Plus, Team, Enterprise, API Tier keys).
- **Glanceable Real-Time Quotas**: Visual circular and linear gauges display remaining token percentage, request counts, and rolling window reset durations.
- **Signed-Out Detection & Alerts**: Detects when tokens expire, are revoked, or require re-authentication, sending high-priority Android notifications with deep links to re-sign in.
- **Low-Quota Threshold Warnings**: Configurable proactive alerts (5%, 10%, 25% remaining) to prevent sudden workflow interruptions.
- **Android Home-Screen Widgets**: Built with Jetpack Glance supporting Small (Compact), Medium (Standard with progress bar), and Multi-Account home-screen widgets with Material You dynamic theming.
- **Battery-Conscious Background Sync**: WorkManager integration with configurable periodic sync (15m, 30m, 1h, 3h, 6h), network constraint enforcement, and exponential retry backoff.
- **Offline-First Resilience**: Cached snapshots remain visible offline with stale indicators and relative timestamps (`Updated 4m ago`).
- **Hardware-Protected Security**: Android Keystore AES-256-GCM encryption with complete per-account credential isolation.
- **Privacy By Default**: Zero analytics, zero third-party telemetry, no cloud relays, and credential exclusion from Android backups.

---

## Technical Stack & Architecture

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3 (Dynamic Color / Material You support, Dark/Light modes, Edge-to-Edge)
- **Architecture**: Clean Architecture with Unidirectional Data Flow (UDF)
  - `domain`: Pure Kotlin entities, use cases, and repository interfaces
  - `data`: Room Database (`AppDatabase`), DataStore Preferences, OkHttp client, Keystore security storage
  - `worker`: WorkManager `QuotaRefreshWorker`
  - `notifications`: Notification channels with deep link intents (`codexquota://account/{id}`)
  - `widget`: Jetpack Glance AppWidgets
- **Build System**: Gradle Kotlin DSL with Version Catalog (`gradle/libs.versions.toml`)

```text
com.codex.quota/
├── auth/            # Auth status models and token resolver
├── data/
│   ├── local/       # Room database, DAOs, DataStore preferences
│   ├── remote/      # OkHttp API client, rate-limit header parsing, Real & Mock data sources
│   └── repository/  # Repository implementations
├── domain/
│   ├── model/       # CodexAccount, CodexUsage, AuthStatus, PlanType, RateLimitInfo
│   ├── repository/  # Repository interfaces
│   └── usecase/     # Use cases for sync, auth detection, CRUD
├── notifications/   # SignedOutNotificationManager & QuotaAlertNotificationManager
├── security/        # Android Keystore AES-256-GCM encryption
├── ui/
│   ├── components/  # CircularQuotaGauge, LinearQuotaBar, StatusBadge, RelativeTimeText
│   ├── feature/     # Dashboard, AccountDetail, AddAccount, Settings, Onboarding, About
│   ├── navigation/  # Navigation host and deep link handling
│   └── theme/       # Material 3 colors, typography, shapes, dynamic theme
├── widget/          # Jetpack Glance Small, Medium, and Multi-Account widgets
└── worker/          # WorkManager QuotaRefreshWorker & WorkScheduler
```

---

## OpenAI & Codex Integration Details

### Integration Transparency
1. **Public Developer Platform APIs**:
   - The application connects directly to official OpenAI Developer Platform endpoints (`https://api.openai.com/v1/models`).
   - Every authenticated request inspects live HTTP response headers:
     - `x-ratelimit-limit-requests` & `x-ratelimit-remaining-requests`
     - `x-ratelimit-reset-requests`
     - `x-ratelimit-limit-tokens` & `x-ratelimit-remaining-tokens`
     - `x-ratelimit-reset-tokens`
   - HTTP 401/403 responses cleanly trigger `AUTHENTICATION_REQUIRED` states and user notifications.
   - HTTP 429 triggers `TEMPORARY_ERROR` / rate-limited state.

2. **Consumer ChatGPT/Codex Quota API Limitation**:
   - OpenAI does *not* offer a public OAuth2 authorization grant or REST API for personal ChatGPT Plus/Team consumer subscription rolling message quotas (e.g. 50 messages / 3 hours).
   - In accordance with OpenAI Terms of Service and security requirements, this application does **not** scrape web dashboards, intercept passwords, or automate hidden browser sessions.
   - The app provides a clean `CodexAccountDataSource` abstraction and a built-in **Demo Simulator** mode with realistic profiles (Personal Plus 78%, Work Team 31%, Enterprise 94%, Rate-Limited simulator, Expired Session simulator) allowing complete UI, widget, and notification testing.

---

## Security & Privacy Policy

- **No Passwords**: The app never collects or stores user passwords.
- **Hardware-Backed Encryption**: All API keys are encrypted using AES-GCM-256 via the Android Keystore.
- **Per-Account Isolation**: Each account's credentials are stored and resolved independently.
- **Backup Exclusions**: Android cloud backups are configured in `data_extraction_rules.xml` to exclude private credential preferences.
- **Zero Telemetry**: No analytics SDKs or tracking libraries are bundled.

---

## Building and Testing

### Prerequisites
- JDK 21
- Android SDK 35 (Android 15) with Build Tools 35.0.0

### Run Unit Tests
```bash
./gradlew test
```

### Run Android Lint
```bash
./gradlew lint
```

### Build Debug APK
```bash
./gradlew assembleDebug
```
The resulting debug APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Disclaimer

Codex Quota is an independent, open-source utility and is not affiliated with, maintained, authorized, sponsored, or endorsed by OpenAI, Inc. OpenAI, ChatGPT, and Codex are trademarks or registered trademarks of OpenAI, Inc.
