# PRD.md (Product Requirements Document)

## 1. Meta
* **Project Name:** Anti Gravity
* **Platform:** Android (Sender) -> macOS / iPadOS (Receivers)
* **Goal:** A lightweight, one-way notification syncing pipeline that forwards Android system notifications to Apple devices using native APNs, bypassing the need for a paid Apple Developer account.
* **Target Audience:** Solo developer / Personal use (Open-source release on GitHub).

## 2. Problem Statement
Apple's ecosystem does not natively support syncing notifications from Android devices. Custom iPadOS/macOS applications require an active $99/yr Apple Developer account to utilize background Apple Push Notification service (APNs). 

## 3. Proposed Solution
Build an Android client (`NotificationListenerService`) that intercepts device notifications, filters out background noise, and pushes the payload to an existing open-source push gateway (ntfy.sh or Bark). The gateway handles the APNs routing, delivering native notifications to the iPad and macOS machine.

## 4. Core Requirements
* **Requirement 1: Notification Interception:** Read all incoming Android notifications in real-time.
* **Requirement 2: Smart Filtering:** Ignore `isOngoing` notifications (media controls, background services, persistent system alerts) to prevent flooding.
* **Requirement 3: Payload Forwarding:** POST structured JSON (Title, Body, App Name) to a configurable ntfy/Bark webhook endpoint.
* **Requirement 4: Battery Optimization:** Maintain active background execution despite OEM battery throttling.
* **Requirement 5: Privacy:** All payloads must support AES-GCM end-to-end encryption before leaving the Android device.