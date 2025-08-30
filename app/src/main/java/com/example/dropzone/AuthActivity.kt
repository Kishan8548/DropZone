package com.example.dropzone

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.dropzone.databinding.ActivityAuthBinding
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener { loginUser() }
        binding.signUpText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.forgotPasswordText.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        when {
            email.isEmpty() || password.isEmpty() -> {
                showToast("Please enter email and password.")
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.endsWith("@iiitl.ac.in") -> {
                binding.emailEditText.error = "Enter a valid IIITL email address"
                binding.emailEditText.requestFocus()
            }
            password.length < 6 -> {
                binding.passwordEditText.error = "Password must be at least 6 characters"
                binding.passwordEditText.requestFocus()
            }
            else -> {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            showToast("Login successful!")
                            navigateToMain()
                        } else {
                            showToast("Login failed: ${task.exception?.message}")
                        }
                    }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
