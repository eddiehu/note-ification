package com.noteification.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Posts, updates, and cancels the note's ongoing notification.
 *
 * A non-empty note is always pinned (ongoing notification). An empty note
 * cancels it. On Android 16+ the notification is additionally promoted to a
 * Live Update so the first 7 characters appear as a status-bar chip.
 */
class NoteNotifier(private val context: Context) {

    private val nm = context.getSystemService(NotificationManager::class.java)

    private fun ensureChannel() {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Note", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shows your pinned note"
            }
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun openAppIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun sync(text: String) {
        ensureChannel()
        if (text.isBlank()) {
            nm.cancel(NOTIF_ID)
            return
        }
        if (!hasNotificationPermission()) return // note itself is still saved

        val notification = if (Build.VERSION.SDK_INT >= 36) {
            buildApi36(text)
        } else {
            buildCompat(text)
        }
        nm.notify(NOTIF_ID, notification)
    }

    /** Pre-36 path: plain ongoing notification via NotificationCompat. */
    private fun buildCompat(text: String): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note_small)
            .setContentTitle(TITLE)
            .setContentText(text.lineSequence().firstOrNull()?.take(120).orEmpty())
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(TITLE)
                    .bigText(text)
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    /**
     * API 36+ path: builds with the framework builder so the Live Update
     * promotion actually takes effect.
     *
     * NotificationCompat 1.17's setShortCriticalText() calls the framework
     * Notification.Builder.setShortCriticalText() directly on API 36 (the
     * "android.shortCriticalText" extra is only a pre-36 compat shim), so
     * extras alone never produce the status-bar chip. Our compileSdk is 34,
     * so the API-36-only method is invoked via reflection.
     */
    @RequiresApi(36)
    private fun buildApi36(text: String): Notification {
        val firstLine = text.lineSequence().firstOrNull()?.take(120).orEmpty()
        val frameworkBuilder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note_small)
            .setContentTitle(TITLE)
            .setContentText(firstLine)
            .setStyle(
                Notification.BigTextStyle()
                    .setBigContentTitle(TITLE)
                    .bigText(text)
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        // Status-bar chip text (up to ~7 chars). Reflective: added in API 36.
        runCatching {
            Notification.Builder::class.java.methods
                .first { it.name == "setShortCriticalText" && it.parameterCount == 1 }
                .invoke(frameworkBuilder, text.trim().take(7))
        }

        val notification = frameworkBuilder.build()
        // Promotion request flag — the key SystemUI looks for.
        notification.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
        return notification
    }

    companion object {
        const val CHANNEL_ID = "note"
        const val NOTIF_ID = 1
        private const val TITLE = "Note-ification"

        // Live Update promotion extra, from NotificationCompat 1.17 source.
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    }
}
