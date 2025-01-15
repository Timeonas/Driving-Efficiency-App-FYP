package com.example.drivingefficiencyapp

import android.content.Intent
import android.os.*
import androidx.appcompat.app.AppCompatActivity

class AppIntro : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.app_intro)

        // Add a delay and then start the main activity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, Test::class.java))
            finish()
        }, 2000) // 2 seconds delay
    }
}