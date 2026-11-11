package com.kalpesh.app

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class LauncherActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())

    private val singleTapAction = Runnable {
        val prefs = getSharedPreferences("kalpesh", Context.MODE_PRIVATE)
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(this, AdminReceiver::class.java)

        if (prefs.getBoolean("tap_lock_enabled", true) && dpm.isAdminActive(admin)) {
            dpm.lockNow()                       // 1 tap = phone lock
        } else {
            openSettings()                      // permission nathi to settings khule
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handler.postDelayed(singleTapAction, 450)
    }

    // Biji var icon dabavyu = double tap
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handler.removeCallbacks(singleTapAction)
        openSettings()
        finish()
    }

    private fun openSettings() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onDestroy() {
        handler.removeCallbacks(singleTapAction)
        super.onDestroy()
    }
}
