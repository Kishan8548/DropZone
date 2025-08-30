package com.example.dropzone

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.dropzone.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Glide.with(this)
                .load(currentUser.photoUrl)
                .placeholder(R.drawable.ic_account_circle_large)
                .error(R.drawable.ic_account_circle_large)
                .into(binding.editProfileImageView)

            binding.fullNameEditText.setText(currentUser.displayName)
            binding.emailEditText.setText(currentUser.email)
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "User not logged in. Finishing EditProfileActivity.")
            finish()
            return
        }

        binding.saveProfileButton.setOnClickListener {
            Log.d(TAG, "Save Profile button clicked.")
            saveProfileChanges()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun saveProfileChanges() {
        val newFullName = binding.fullNameEditText.text.toString().trim()
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        if (newFullName.isNotEmpty() && newFullName != currentUser.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newFullName)
                .build()

            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Profile updated successfully.")
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Update failed: ${task.exception?.message}", task.exception)
                        Toast.makeText(this, "Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "No changes made.", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "No profile changes detected.")
        }
    }
}
