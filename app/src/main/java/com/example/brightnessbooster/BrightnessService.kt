package com.example.brightnessbooster

import android.app.*
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import moe.shizuku.api.Shizuku
import moe.shizuku.api.ShizukuBinderWrapper

class BrightnessService : Service() {
    
    companion object {
        var offset = 30
        private var shizukuResolver: ContentResolver? = null
        private var brightnessObserver: ContentObserver? = null
        private var isApplying = false
        private var currentSystemBrightness = 0
        
        fun setupShizuku() {
            if (shizukuResolver == null && Shizuku.pingBinder()) {
                shizukuResolver = ContentResolver(ShizukuBinderWrapper(Shizuku.getBinder()))
            }
        }
        
        fun updateOffset(newOffset: Int) {
            offset = newOffset
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        setupShizuku()
        createNotificationChannel()
        startForeground(1, getNotification())
        registerBrightnessObserver()
        applyInitialOffset()
    }
    
    private fun registerBrightnessObserver() {
        brightnessObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (!isApplying) {
                    val systemBrightness = getSystemBrightness()
                    if (systemBrightness != currentSystemBrightness) {
                        currentSystemBrightness = systemBrightness
                        applyBrightnessOffset()
                    }
                }
            }
        }
        
        val resolver = shizukuResolver ?: contentResolver
        resolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            brightnessObserver
        )
    }
    
    private fun getSystemBrightness(): Int {
        return try {
            val resolver = shizukuResolver ?: contentResolver
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 100)
        }
    }
    
    private fun applyInitialOffset() {
        currentSystemBrightness = getSystemBrightness()
        applyBrightnessOffset()
    }
    
    private fun applyBrightnessOffset() {
        val targetBrightness = (currentSystemBrightness + offset).coerceIn(0, 255)
        
        isApplying = true
        try {
            val resolver = shizukuResolver ?: contentResolver
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, targetBrightness)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            Handler(Looper.getMainLooper()).postDelayed({ isApplying = false }, 100)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "brightness_channel",
                "亮度增强服务",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
    
    private fun getNotification(): Notification {
        return NotificationCompat.Builder(this, "brightness_channel")
            .setContentTitle("亮度增强器")
            .setContentText("自动亮度 +$offset 运行中")
            .setSmallIcon(android.R.drawable.ic_menu_brightness)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    
    override fun onDestroy() {
        super.onDestroy()
        brightnessObserver?.let {
            (shizukuResolver ?: contentResolver).unregisterContentObserver(it)
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
