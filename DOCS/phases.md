# phases.md

## Phase 1: Core Plumbing (MVP)
* Initialize Android project (Kotlin).
* Implement `NotificationListenerService`.
* Add basic HTTP POST capability to a hardcoded ntfy.sh testing topic.
* Verify notifications appear on the iPad/Mac.

## Phase 2: Logic & Filtering
* Implement `isOngoing` checks to block persistent notifications.
* Build a simple package-name blacklist array to block system noise (e.g., `com.android.systemui`).
* Build the minimal UI to input dynamic Webhook URLs instead of hardcoding.

## Phase 3: Stability & Security
* Implement AES-GCM encryption on the Android sender (matching the ntfy/Bark decryption standard).
* Add Android foreground service notification to prevent battery optimization kills.
* Add crash reporting / silent error handling for null intents.

## Phase 4: Polish & Open Source
* Clean up UI to ensure a minimal, efficient aesthetic.
* Write the `README.md` with setup instructions for ntfy/Bark.
* Strip personal tokens and publish to GitHub.