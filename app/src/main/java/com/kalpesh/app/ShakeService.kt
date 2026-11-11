package com.kalpesh.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.PowerManager
import kotlin.math.sqrt

class ShakeService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var cpuLock: PowerManager.WakeLock
    private var lastShake = 0L
    private var shakeCount = 0
    private var firstShakeTime = 0L
    private var lastWake = 0L

    override fun onCreate() {
        super.onCreate()
        val channelId = "kalpesh_shake"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "Shake to wake", NotificationManager.IMPORTANCE_MIN)
        )
        val notification = Notification.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_lock)
            .setContentTitle("Kalpesh")
            .setContentText("Shake to wake chalu che")
            .setOngoing(true)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Kalpesh:cpu")
        cpuLock.acquire()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val prefs = getSharedPreferences("kalpesh", Context.MODE_PRIVATE)
        val threshold = 1.5f + (10 - prefs.getInt("sensitivity", 5)) * 0.2f

        val gForce = sqrt(
            event.values[0] * event.values[0] +
            event.values[1] * event.values[1] +
            event.values[2] * event.values[2]
        ) / SensorManager.GRAVITY_EARTH

        if (gForce > threshold) {
            val now = System.currentTimeMillis()
            if (now - lastShake < 150) return
            lastShake = now
            if (now - firstShakeTime > 1000) {
                firstShakeTime = now
                shakeCount = 0
            }
            shakeCount++
            if (shakeCount >= 2 && now - lastWake > 2000) {
                shakeCount = 0
                lastWake = now
                wakeScreen()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) {
            val wl = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Kalpesh:wake"
            )
            wl.acquire(5000)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        if (cpuLock.isHeld) cpuLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
