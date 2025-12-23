package com.hiennv.flutter_callkit_incoming

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Manages wake locks for incoming calls to keep the screen on while ringing.
 * 
 * We use deprecated SCREEN_BRIGHT_WAKE_LOCK and FULL_WAKE_LOCK because there is
 * no modern alternative for keeping the screen on from a BroadcastReceiver context.
 * The modern APIs (FLAG_KEEP_SCREEN_ON, setTurnScreenOn) require an Activity with
 * a Window, but we need to wake the screen BEFORE the CallkitIncomingActivity launches.
 */
object CallkitWakeLockManager {
    private const val TAG = "CallkitWakeLockManager"
    private const val WAKE_LOCK_TAG = "Callkit:IncomingCallWakeLock"
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    @Synchronized
    fun acquireWakeLock(context: Context, durationMs: Long) {
        try {
            releaseWakeLock()
            
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                ?: return
            
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or 
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKE_LOCK_TAG
            )
            
            wakeLock?.acquire(durationMs)
            Log.d(TAG, "Wake lock acquired for ${durationMs}ms")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    @Synchronized
    fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "Wake lock released")
                }
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release wake lock", e)
        }
    }
}
