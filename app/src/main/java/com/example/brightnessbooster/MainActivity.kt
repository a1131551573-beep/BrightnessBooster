package com.example.brightnessbooster

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import moe.shizuku.api.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var seekBar: SeekBar
    private lateinit var offsetText: TextView
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    
    private var offset = 30
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        seekBar = findViewById(R.id.seekBar)
        offsetText = findViewById(R.id.offsetText)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        
        checkShizuku()
        
        if (!Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar?, progress: Int, fromUser: Boolean) {
                offset = progress
                offsetText.text = "偏移量: +$offset"
                if (isServiceRunning) {
                    BrightnessService.updateOffset(offset)
                }
            }
            override fun onStartTrackingTouch(seek: SeekBar?) {}
            override fun onStopTrackingTouch(seek: SeekBar?) {}
        })
        
        toggleButton.setOnClickListener {
            if (isServiceRunning) {
                stopService(Intent(this, BrightnessService::class.java))
                isServiceRunning = false
                toggleButton.text = "启动自动调亮"
                statusText.text = "状态: 已停止"
                statusText.setTextColor(0xFFF44336.toInt())
            } else {
                BrightnessService.offset = offset
                BrightnessService.setupShizuku()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(this, BrightnessService::class.java))
                } else {
                    startService(Intent(this, BrightnessService::class.java))
                }
                isServiceRunning = true
                toggleButton.text = "停止自动调亮"
                statusText.text = "状态: 运行中 (自动亮度 + $offset)"
                statusText.setTextColor(0xFF4CAF50.toInt())
            }
        }
    }
    
    private fun checkShizuku() {
        if (!Shizuku.pingBinder()) {
            AlertDialog.Builder(this)
                .setTitle("Shizuku 未运行")
                .setMessage("请先启动 Shizuku 应用并授权本应用")
                .setPositiveButton("确定") { _, _ -> finish() }
                .show()
        } else {
            if (!Shizuku.checkSelfPermission().isGranted) {
                Shizuku.requestPermission(0)
            }
        }
    }
}
