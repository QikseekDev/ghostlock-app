package com.ghostlock.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ghostlock.app.ui.MainActivity

/** Re-enters the existing autoroot-on-startup path after a device reboot. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val enabled = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getBoolean(AUTOROOT_ON_STARTUP, false)
        if (!enabled) return

        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            ),
        )
    }

    private companion object {
        const val PREFERENCES = "ghostlock_prefs"
        const val AUTOROOT_ON_STARTUP = "autoroot_on_startup"
    }
}
