package com.hiennv.flutter_callkit_incoming

import android.content.Context
import android.os.PowerManager
import android.util.Log

/**
 * Manages wake locks for incoming calls to prevent the device from sleeping
 * and keep the screen on while a call notification is being displayed.
 * 
 * This is necessary because WakelockPlus (Flutter plugin) only works when
 * the Flutter engine is running and the app is in foreground. When the app
 * is in background or not running, we need native wake lock management.
 */
object CallkitWakeLockManager {
    private const val TAG = "CallkitWakeLockManager"
    private const val WAKE_LOCK_TAG = "Callkit:IncomingCallWakeLock"
    private const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds default
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    /**
     * Acquires a wake lock to keep the screen on during an incoming call.
     * Uses SCREEN_BRIGHT_WAKE_LOCK | FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP
     * to turn on and keep the screen bright.
     * 
     * Note: These flags are deprecated but still work and are necessary
     * for keeping the screen on from a BroadcastReceiver context where
     * we don't have access to a Window to set FLAG_KEEP_SCREEN_ON.
     * 
     * @param context Application context
     * @param durationMs Duration in milliseconds to hold the wake lock.
     *                   Defaults to 30 seconds if not specified.
     */
    @Synchronized
    fun acquireWakeLock(context: Context, durationMs: Long = DEFAULT_TIMEOUT_MS) {
        try {
            // Release any existing wake lock first
            releaseWakeLock()
            
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager == null) {
                Log.e(TAG, "PowerManager not available")
                return
            }
            
            // Use SCREEN_BRIGHT_WAKE_LOCK | FULL_WAKE_LOCK to keep screen on
            // ACQUIRE_CAUSES_WAKEUP turns the screen on when acquired
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or 
                PowerManager.FULL_WAKE_LOCK or 
                PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKE_LOCK_TAG
            )
            
            wakeLock?.acquire(durationMs)
            Log.d(TAG, "Wake lock acquired for ${durationMs}ms (screen will stay on)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }
    
    /**
     * Releases the wake lock if it's currently held.
     */
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
    
    /**
     * Checks if a wake lock is currently held.
     */
    fun isWakeLockHeld(): Boolean {
        return wakeLock?.isHeld == true
    }
}
