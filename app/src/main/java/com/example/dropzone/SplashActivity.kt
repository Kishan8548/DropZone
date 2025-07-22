package com.example.dropzone

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash) // You'll create this layout

        Handler(Looper.getMainLooper()).postDelayed({
            // Check if user is already authenticated
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                // User is logged in, go to MainActivity directly
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                // User not logged in, go to WelcomeActivity
                val intent = Intent(this, WelcomeActivity::class.java)
                startActivity(intent)
            }
            finish() // Close the splash activity
        }, SPLASH_TIME_OUT)
    }
}