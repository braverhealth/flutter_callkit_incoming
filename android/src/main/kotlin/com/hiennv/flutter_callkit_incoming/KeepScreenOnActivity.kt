package com.hiennv.flutter_callkit_incoming

import android.app.Activity
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

/**
 * An invisible activity that keeps the screen on during incoming call ringing.
 * 
 * This is the modern alternative to using deprecated wake locks (SCREEN_BRIGHT_WAKE_LOCK).
 * The activity uses FLAG_KEEP_SCREEN_ON which is the recommended way to prevent
 * the screen from turning off.
 */
class KeepScreenOnActivity : Activity() {

    companion object {
        private const val ACTION_FINISH = "com.hiennv.flutter_callkit_incoming.ACTION_FINISH_KEEP_SCREEN_ON"
        
        fun getIntent(context: Context, duration: Long): Intent {
            return Intent(context, KeepScreenOnActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                putExtra("duration", duration)
            }
        }
        
        fun getFinishIntent(context: Context): Intent {
            return Intent("${context.packageName}.$ACTION_FINISH")
        }
        
        fun sendFinishBroadcast(context: Context) {
            context.sendBroadcast(getFinishIntent(context))
        }
    }
    
    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            finishActivity()
        }
    }
    
    private val timeoutHandler = Handler(Looper.getMainLooper())
    
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make the activity invisible
        setContentView(android.R.color.transparent)
        
        // Keep screen on using the modern, non-deprecated approach
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Register receiver to finish when call ends
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                finishReceiver,
                IntentFilter("${packageName}.$ACTION_FINISH"),
                Context.RECEIVER_EXPORTED
            )
        } else {
            registerReceiver(
                finishReceiver,
                IntentFilter("${packageName}.$ACTION_FINISH")
            )
        }
        
        // Auto-finish after duration (fallback safety)
        val duration = intent.getLongExtra("duration", 30000L)
        timeoutHandler.postDelayed({ finishActivity() }, duration)
    }
    
    private fun finishActivity() {
        if (!isFinishing) {
            finish()
            overridePendingTransition(0, 0)
        }
    }
    
    override fun onDestroy() {
        timeoutHandler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(finishReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
    
    override fun onBackPressed() {
        // Prevent back button from closing this activity
    }
}
