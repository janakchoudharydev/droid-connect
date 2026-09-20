# design.md

## 1. Design Philosophy
Following a preference for minimal, clean UI and efficient flows, Anti Gravity operates primarily as a headless background utility. Visual elements are strictly limited to configuration.

## 2. Android Configuration App (Minimal UI)
* **Main Screen:**
  * **Master Toggle:** A single, large switch to Start/Stop the `NotificationListenerService`.
  * **Status Indicator:** Plain text showing "Service Running" or "Service Stopped".
  * **Server Config:** Two text input fields for the Webhook URL and the Encryption Key.
* **Filtering Screen (Settings):**
  * A simple checklist of installed applications to toggle forwarding on/off per app.
* **Design Language:** Material You (Android 12+ dynamic theming) for zero-effort, clean native styling without custom assets.

## 3. Receiver UX (macOS / iPadOS)
* **Zero UI:** There is no custom app interface on the Apple devices. 
* **Experience:** Notifications appear natively in the macOS Notification Center and iPad lock screen, styled exactly like native Apple system alerts.