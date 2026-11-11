package com.kalpesh.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("kalpesh", Context.MODE_PRIVATE)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED &&
            prefs.getBoolean("shake_enabled", false)
        ) {
            context.startForegroundService(Intent(context, ShakeService::class.java))
        }
    }
}
