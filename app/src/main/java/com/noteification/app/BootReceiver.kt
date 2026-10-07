package com.noteification.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-posts the pinned note's notification after a reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val note = NoteStore(context).load()
        if (note.isNotBlank()) {
            NoteNotifier(context).sync(note)
        }
    }
}
