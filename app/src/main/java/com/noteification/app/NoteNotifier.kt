package com.noteification.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    fun sync(text: String) {
        ensureChannel()
        if (text.isBlank()) {
            nm.cancel(NOTIF_ID)
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return // can't post; the note itself is still saved
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note_small)
            .setContentTitle("Note-ification")
            .setContentText(text.lineSequence().firstOrNull()?.take(120).orEmpty())
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("Note-ification")
                    .bigText(text)
            )
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)

        if (Build.VERSION.SDK_INT >= 36) {
            // Extras-based promotion (same keys NotificationCompat 1.17 uses);
            // harmless on releases that don't understand them.
            builder.addExtras(
                Bundle().apply {
                    putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
                    putString(EXTRA_SHORT_CRITICAL_TEXT, text.trim().take(7))
                }
            )
        }

        nm.notify(NOTIF_ID, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "note"
        const val NOTIF_ID = 1

        // Live Update promotion extras, from NotificationCompat 1.17 source.
        // Kept as literals so the project builds on the offline toolchain
        // (core-ktx 1.13.1) without a dependency upgrade.
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
    }
}
