package com.example.nocturnemusic

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Optional dim ambient screen. This is NOT the manufacturer's system AOD.
 * Android does not let ordinary apps replace the system Always-On Display globally.
 */
class AmbientActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        val title = intent.getStringExtra("title") ?: "Now playing"
        val artist = intent.getStringExtra("artist") ?: "Nocturne Music"
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        root.addView(TextView(this).apply {
            text = "♫"; textSize = 54f; setTextColor(0xFFB7F36B.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = title; textSize = 22f; setTextColor(0xFFF4F5F1.toInt())
            gravity = Gravity.CENTER; setPadding(0, 20, 0, 8)
        })
        root.addView(TextView(this).apply {
            text = artist; textSize = 15f; setTextColor(0xFF8B918B.toInt())
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Tap back to return"; textSize = 12f; setTextColor(0xFF555B55.toInt())
            gravity = Gravity.CENTER; setPadding(0, 40, 0, 0)
        })
        setContentView(root)
    }
}
