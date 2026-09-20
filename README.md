# Anti Gravity 🚀

> **One-way notification syncing pipeline that forwards Android system notifications to Apple devices (macOS & iPadOS) via native APNs — without requiring a paid $99/yr Apple Developer account.**

---

## 📖 Overview

Apple's ecosystem does not natively allow receiving push notifications from Android devices. Traditional custom iOS/macOS apps require an active Apple Developer Program subscription to use Apple Push Notification service (APNs).

**Anti Gravity** solves this by acting as a lightweight, battery-efficient Android background utility (`NotificationListenerService`). It intercepts incoming status bar notifications, filters out ongoing media and system background noise, optionally encrypts the payload using AES-256-GCM, and forwards it to an open push gateway ([ntfy.sh](https://ntfy.sh) or [Bark](https://github.com/Finb/Bark)), which delivers native APNs alerts directly to your Mac and iPad.

```
[Android Notification]
         │
         ▼
[AntiGravity NotificationListenerService]
  ├── isOngoing & System Filter
  ├── App Whitelist / Blacklist Filter
  └── Optional AES-256-GCM Encryption
         │
         ▼ (HTTP POST via OkHttp)
[ntfy.sh / Bark Gateway]
         │
         ▼ (Apple APNs)
[macOS / iPadOS Alert Banners]
```

---

## ✨ Features

- **Real-Time Notification Interception:** Low-latency forwarding for incoming messages, alerts, and calls.
- **Smart Filtering:**
  - Automatically suppresses persistent / `isOngoing` notifications (media players, navigation, foreground service banners).
  - Built-in suppression of system noise (`com.android.systemui`, `android`, etc.).
  - Per-app customization: Blacklist or Whitelist mode with search to toggle forwarding per installed application.
- **Privacy & Encryption:** Optional client-side AES-256-GCM encryption with SHA-256 key derivation and randomized 12-byte IVs.
- **Battery Efficient:** Operates event-driven via Android's native `NotificationListenerService` without persistent wake locks or CPU-heavy polling loops.
- **Zero UI on Apple Devices:** Seamlessly displays in macOS Notification Center and iPad lock screen using existing native client apps.

---

## 🛠️ Quick Setup Guide

### Step 1: Set Up Apple Receiver (macOS / iPadOS)

Choose either **ntfy** (recommended) or **Bark**:

#### Option A: ntfy (Recommended)
1. Install **ntfy** from the [Mac App Store](https://apps.apple.com/app/ntfy/id1625396347) or [iOS App Store](https://apps.apple.com/app/ntfy/id1625396347).
2. Open ntfy, tap the **+** icon, and subscribe to a private, unguessable topic (e.g., `ag-sync-7x9q2m`).
3. Your webhook URL will be:
   ```
   https://ntfy.sh/ag-sync-7x9q2m
   ```

#### Option B: Bark
1. Install **Bark** from the App Store.
2. Open Bark and copy your device push URL:
   ```
   https://api.day.app/YOUR_DEVICE_KEY
   ```

---

### Step 2: Configure Android Device

1. Build and install the APK on your Android device (see [Building from Source](#-building-from-source)).
2. Launch **Anti Gravity**.
3. **Grant Notification Access:** Tap **Grant Permission** and toggle on "Anti Gravity" in the system Notification Access settings.
4. **Disable Battery Optimization:** Tap **Disable Battery Optimization** so the system does not kill the listener service when in deep sleep.
5. **Configure Webhook:**
   - Enter your Webhook URL from Step 1 (e.g., `https://ntfy.sh/ag-sync-7x9q2m`).
   - *(Optional)* Enter an AES-256 passphrase for end-to-end encryption.
   - Tap **Save Configuration**.
6. **Verify:** Tap **Send Test Push**. A test notification should appear on your Mac / iPad within seconds.
7. **Filter Apps:** Tap **Configure App Filters** to choose which apps should forward notifications.

---

## 🔒 End-to-End Encryption (E2EE)

When an encryption key is specified:
1. Anti Gravity generates a cryptographically secure 12-byte random IV.
2. The key is derived using SHA-256 to ensure a 256-bit AES key.
3. The payload is encrypted with `AES/GCM/NoPadding` with a 128-bit authentication tag.
4. The IV is prepended to the ciphertext and sent as Base64.
5. The HTTP request includes an `X-Encrypted: AES-256-GCM` header.

---

## 📦 Building from Source

### Requirements
- JDK 17+
- Android SDK 34 (compileSdk: 34, minSdk: 26)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install Directly to Connected Android Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📁 Project Architecture

```
app/src/main/
├── AndroidManifest.xml
├── java/com/antigravity/droidconnect/
│   ├── AntiGravityApp.kt                 # Application class & global preferences
│   ├── crypto/
│   │   └── CryptoManager.kt              # AES-256-GCM encryption & decryption
│   ├── data/
│   │   ├── AppPreferences.kt             # SharedPreferences wrapper
│   │   └── FilterMode.kt                 # Whitelist / Blacklist mode enum
│   ├── model/
│   │   └── NotificationData.kt           # Payload & App item models
│   ├── network/
│   │   └── WebhookClient.kt              # OkHttp client for ntfy & Bark
│   ├── service/
│   │   └── AntiGravityNotificationService.kt # NotificationListenerService
│   └── ui/
│       ├── MainActivity.kt               # Main controls, switch & test push
│       ├── AppFilterActivity.kt          # App list & search filter screen
│       └── AppFilterAdapter.kt           # RecyclerView adapter for apps
└── res/
    ├── layout/                           # Clean Material 3 XML layouts
    ├── values/                           # Colors, strings, themes
    └── xml/                              # Backup & data extraction rules
```

---

## 📄 License
MIT License. Open-source release for solo developers and personal productivity.
