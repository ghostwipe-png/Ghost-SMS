# Ghost SMS
**A private, tactile Material 3 SMS & MMS client built for speed, financial clarity, and zero-clutter messaging.**
---
## Core Architecture & Features
### Borderless Material 3 (Material You) Design
* **Zero-Borderline Surface System:** Built with `0dp` strokes and soft tonal elevation across both Deep Obsidian Dark and Mist Light themes.
* **Asymmetric Grouped Chat Bubbles:** Dynamic corner radii that automatically cluster consecutive messages from the same sender with adaptive WCAG luminance contrast.
* **Floating Glass Composer Capsule:** Pill-shaped message composer with inline quick snippets, SIM routing, and attachment controls.
* **Deterministic Monogram Avatars:** Crisp vector contact avatars with consistent tonal color mapping and Material You adaptive launcher icons.
### Tactile & Live Motion Engine
* **Global Micro-Tick Haptics (`GhostHapticEngine`):** Hardware-adaptive tactile feedback across every tap, filter chip, bubble selection, OTP copy, and message send.
* **Spring Physics Message Arrival (`GhostMotionEngine`):** Live incoming messages animate into view with overshoot spring physics and subtle pulse feedback.
* **Borderless Floating Quick-Reply Popup (`GhostIncomingPopupActivity`):** Instant heads-up overlay card with **1-Tap Copy OTP** and inline capsule quick-reply without leaving your current app.
### Smart Inbox, Finance & Privacy Suite
* **Automatic Inbox Classification (`GhostInboxClassifier`):** Separates threads into **All**, **Personal**, **OTPs & Finance**, and **Promotions** tabs with live unread badges.
* **M-Pesa & Financial Ledger (`GhostLedgerParser` & `GhostLedgerBottomSheet`):** Parses mobile money and bank transaction SMS into clean, scannable summaries and inline highlighted cards.
* **1-Tap OTP Detection & Auto-Sweeper (`GhostOtpHelper` & `GhostOtpSweeper`):** Extracts verification codes for one-tap copying from notifications, popups, or chat bubbles, with optional auto-cleanup of expired codes.
* **Privacy Vault & Link Guard (`GhostVaultManager` & `GhostLinkGuard`):** Hide sensitive threads behind PIN/biometric protection and inspect suspicious URLs before opening.
* **Encrypted Backups & Smart Replies (`GhostEncryptedBackupManager` & `GhostSmartReplyHelper`):** Password-protected local backups and instant contextual reply chips.
---
## Tech Stack
* **Language:** Kotlin
* **UI System:** Android Views, ViewBinding, custom Material 3 tonal drawables & spring physics interpolators
* **Minimum SDK:** Android 8.0 (API 26) — Target SDK: Android 14+ (API 34+)
* **Build System:** Gradle Kotlin DSL (`com.ghost.messages`)
---
## Building from Source
```bash
git clone https://github.com/ghostwipe-png/Ghost-SMS.git
cd Ghost-SMS
./gradlew assembleFossDebug
`
Install directly onto a connected Android device via ADB:
`ash
./gradlew installFossDebug
`
---
## License
Licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](https://www.google.com/search?q=LICENSE) for full details.