# Note-ification

A dead-simple Android notes app: **one plain-text note**, always shown as a
live ongoing notification.

- Type in the box — the notification updates live as you type (debounced)
- **Done** just closes the keyboard; everything autosaves, even if you leave
  the app without hitting Done
- **Dismiss** clears the note; an empty note unpins the notification
- On Android 16+, the notification is promoted to a Live Update so the first
  7 characters appear as a status-bar chip
- Tapping the notification opens the app; the notification is re-posted
  after a reboot
- Follows the system light/dark theme automatically — no toggle, no settings

Native Kotlin + Jetpack Compose. `applicationId`: `com.noteification.app`.
