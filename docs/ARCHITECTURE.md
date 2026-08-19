# Architecture & Technical Design

This document details the system design, modular layers, data flows, security controls, and background workers in the **Codex Quota Monitor** Android application.

---

## 1. Architectural Overview

The application follows the modern **Clean Architecture** and **Unidirectional Data Flow (UDF)** patterns recommended by Google for Android:

```text
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Jetpack Compose UI  │  ViewModels  │  Glance Widgets │
└───────────────────────────┬────────────────────────────┘
                            │ (Observes UI State / Triggers Events)
                            ▼
┌────────────────────────────────────────────────────────┐
│                      Domain Layer                      │
│   Models  │  Use Cases  │  Repository Interfaces       │
└───────────────────────────┬────────────────────────────┘
                            │
                            ▼
┌────────────────────────────────────────────────────────┐
│                       Data Layer                       │
│   Repository Impl  │  Room Database  │  DataStore      │
│   Keystore Storage │  OkHttp Remote Data Sources       │
└────────────────────────────────────────────────────────┘
```

---

## 2. Layers & Responsibilities

### Domain Layer (`com.codex.quota.domain`)
- **`model`**: Pure Kotlin models free from Android framework dependencies:
  - `CodexAccount`: Stable ID, nickname, plan type, auth status, color tag, created timestamp.
  - `CodexUsage`: Remaining/used percent, token counts, credit balance, reset timestamp, fetched time.
  - `AuthStatus`: `AUTHENTICATED`, `REFRESHING`, `OFFLINE`, `TEMPORARY_ERROR`, `AUTHENTICATION_REQUIRED`, `UNKNOWN`.
  - `RateLimitWindow`: Request and token limits, remaining counts, reset durations.
- **`repository`**: `CodexAccountRepository`, `UserPreferencesRepository`.
- **`usecase`**:
  - `ObserveAccountsUseCase`: Reactive Flow of all accounts with current usage.
  - `RefreshAccountUseCase`: Single account on-demand synchronization.
  - `RefreshAllAccountsUseCase`: Periodic / manual refresh of all registered accounts.
  - `AddAccountUseCase` / `RemoveAccountUseCase` / `UpdateAccountNicknameUseCase`.
  - `CheckSignedOutAccountsUseCase`: Compares prior vs current state to emit single-shot alerts.

### Data Layer (`com.codex.quota.data`)
- **`local`**:
  - `AppDatabase`: Room SQLite storage for `AccountEntity` and `UsageSnapshotEntity`.
  - `DataStore`: `UserPreferences` (theme mode, sync frequency, notification thresholds).
- **`security`**:
  - `EncryptedCredentialStore`: Android Keystore-backed AES-256-GCM encryption. Each account ID has its own isolated ciphertext entry. Secrets are never exposed to logs or ordinary SharedPreferences.
- **`remote`**:
  - `OpenAiUsageApi`: OkHttp client with strict timeouts, TLS 1.3, and custom interceptors for rate-limit header parsing.
  - `RealOpenAiDataSource`: Live API integration.
  - `MockOpenAiDataSource`: Deterministic multi-profile mock engine for testing.

### Presentation Layer (`com.codex.quota.ui`)
- **Theme**: Material 3 theme with dynamic color support (Android 12+) and high-contrast fallbacks.
- **Navigation**: Jetpack Navigation Compose with type-safe arguments and deep link support (`codexquota://account/{id}`).
- **Features**:
  - `Dashboard`: Top app bar with instant refresh action, overall quota health, multi-account card list with live circular and linear gauges, relative time formatting.
  - `AccountDetail`: Full breakdown of RPM/TPM limits, token usage, plan details, re-auth and delete dialogs.
  - `AddAccount`: Step-by-step account addition with instant credential validation.
  - `Settings`: Background sync interval, theme selector, notification threshold toggles, privacy audit & clear data.
  - `Onboarding`: 3-page introduction to multi-account monitoring and home-screen widgets.
  - `About`: Disclaimers, open-source attributions, version info.

---

## 3. Background Sync & Notifications

- **`QuotaRefreshWorker`**:
  - Scheduled via `WorkManager` with `Constraints(NetworkType.CONNECTED, RequiresBatteryNotLow = false)`.
  - User-configurable intervals: 15 min, 30 min, 1 hour, 3 hours, 6 hours.
  - On run: Refreshes all accounts, records snapshots, updates Glance widgets, and checks for transitions to `AUTHENTICATION_REQUIRED`.
- **`SignedOutNotificationManager`**:
  - Creates dedicated notification channel: `channel_auth_alerts`.
  - Emits notification with deep link to `AccountDetail` / re-auth screen.
  - State tracking prevents duplicate alert spam on consecutive failed background runs.

---

## 4. Jetpack Glance Home-Screen Widgets

- **`SmallQuotaWidget`**: Single account gauge with percent remaining and reset countdown.
- **`MediumQuotaWidget`**: Progress bar, remaining tokens, reset time, last updated timestamp.
- **`MultiAccountQuotaWidget`**: 2-4 account multi-row summary.
- **`WidgetConfigActivity`**: Account selection activity launched when placing a widget on the home screen.
