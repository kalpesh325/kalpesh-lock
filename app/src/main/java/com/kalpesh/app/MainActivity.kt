package com.kalpesh.app

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private val prefs by lazy { getSharedPreferences("kalpesh", Context.MODE_PRIVATE) }
    private val dpm by lazy { getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    private val admin by lazy { ComponentName(this, AdminReceiver::class.java) }
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(40), dp(20), dp(20))
        }

        root.addView(TextView(this).apply {
            text = "Kalpesh"
            textSize = 28f
            gravity = Gravity.CENTER
        })

        statusText = TextView(this).apply {
            textSize = 14f
            setPadding(0, dp(16), 0, dp(16))
        }
        root.addView(statusText)

        root.addView(Switch(this).apply {
            text = "Shake to wake screen"
            isChecked = prefs.getBoolean("shake_enabled", false)
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("shake_enabled", on).apply()
                if (on) {
                    startForegroundService(Intent(this@MainActivity, ShakeService::class.java))
                } else {
                    stopService(Intent(this@MainActivity, ShakeService::class.java))
                }
            }
        })

        root.addView(Switch(this).apply {
            text = "Icon tap = Phone lock"
            isChecked = prefs.getBoolean("tap_lock_enabled", true)
            setPadding(0, dp(16), 0, dp(16))
            setOnCheckedChangeListener { _, on ->
                prefs.edit().putBoolean("tap_lock_enabled", on).apply()
            }
        })

        root.addView(TextView(this).apply { text = "Shake sensitivity" })
        root.addView(SeekBar(this).apply {
            max = 10
            progress = prefs.getInt("sensitivity", 5)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                    prefs.edit().putInt("sensitivity", p).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })

        root.addView(TextView(this).apply {
            text = "Permissions"
            textSize = 18f
            setPadding(0, dp(24), 0, dp(8))
        })

        root.addView(Button(this).apply {
            text = "Device admin permission (lock mate)"
            setOnClickListener {
                startActivity(
                    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                        putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "Icon tap par phone lock karva mate jaruri che")
                    }
                )
            }
        })

        root.addView(Button(this).apply {
            text = "Notification permission"
            setOnClickListener {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Battery optimization band karo"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun updateStatus() {
        val adminOk = dpm.isAdminActive(admin)
        val notifOk = Build.VERSION.SDK_INT < 33 ||
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val batteryOk = pm.isIgnoringBatteryOptimizations(packageName)

        statusText.text =
            "Device admin: ${if (adminOk) "ON ✅" else "OFF ❌"}\n" +
            "Notification: ${if (notifOk) "ON ✅" else "OFF ❌"}\n" +
            "Battery unrestricted: ${if (batteryOk) "ON ✅" else "OFF ❌"}"
    }
}
