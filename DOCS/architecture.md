# architecture.md

## 1. System Context
The Anti Gravity system relies on a decoupled, one-way data flow utilizing third-party infrastructure for the final APNs delivery leg.

## 2. Component Breakdown

### A. Android Client (The Sender)
* **Core Service:** `NotificationListenerService`
* **Responsibilities:**
  * Listen for `onNotificationPosted`.
  * Extract `EXTRA_TITLE` and `EXTRA_TEXT`.
  * Evaluate against the filtering engine (Regex/Package Name blacklists).
  * Encrypt payload and execute HTTP POST via OkHttp or Retrofit.
* **Permissions:** `BIND_NOTIFICATION_LISTENER_SERVICE`, `INTERNET`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

### B. Push Gateway (The Relay)
* **Service:** ntfy.sh (Self-hosted or Public) OR Bark Server.
* **Responsibilities:** Receive the HTTP POST and trigger the Apple APNs payload.

### C. Apple Clients (The Receivers)
* **Devices:** macOS machine, iPad.
* **Service:** Native ntfy/Bark App Store applications.
* **Responsibilities:** Decrypt the payload and display standard OS-level notification banners.

## 3. Data Flow
1. SMS/WhatsApp arrives on Android.
2. `AntiGravityService` intercepts the status bar notification.
3. Payload is filtered, encrypted, and POSTed to `https://ntfy.sh/{private_topic}`.
4. ntfy server formats payload for APNs and pushes to Apple.
5. iPad/Mac wakes up and displays the native alert.