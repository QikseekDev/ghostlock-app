package com.ghostlock.app.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.PowerManager
import android.util.Log
import com.ghostlock.app.GhostlockApplication
import kotlinx.coroutines.runBlocking

/** Runs autoroot directly from the boot receiver, matching DirtyFrag's model. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!isBootAction(intent.action)) return
        val deContext = context.createDeviceProtectedStorageContext()
        val power = context.getSystemService(PowerManager::class.java) ?: return
        val wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$TAG:boot")
        wakeLock.acquire()
        Thread {
            var ok = false
            val repository = (deContext.applicationContext as? GhostlockApplication)
                ?.createRepository(deContext)
            try {
                if (repository == null) return@Thread
                val snapshot = runBlocking { repository.snapshot() }
                val pair = snapshot.cpuPairs.getOrNull(snapshot.selectedCpuPair)
                if (!snapshot.kernelSupported || pair == null) return@Thread
                val code = runBlocking {
                    if (snapshot.shizukuEnabled) {
                        if (snapshot.shizukuStatus != com.ghostlock.app.domain.model.ShizukuStatus.READY) 1
                        else repository.runExploitWithShizuku(pair) { line -> Log.i(TAG, line.trim()) }
                    } else {
                        repository.runExploit(pair) { line -> Log.i(TAG, line.trim()) }
                    }
                }
                Log.i(TAG, "boot: exploit rc=$code")
                ok = code == 0
            } catch (error: Exception) {
                Log.e(TAG, "boot: exploit exception", error)
            } finally {
                repository?.close()
                wakeLock.release()
            }
            if (!ok) disableAutorun(context)
        }.start()
    }

    private fun disableAutorun(context: Context) {
        Log.i(TAG, "boot: failed - disabling autorun interlock")
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, BootCompletedReceiver::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    private companion object {
        const val TAG = "GhostLock"
    }
}

internal fun isBootAction(action: String?): Boolean =
    action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED
