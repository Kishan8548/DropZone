package com.example.dropzone

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore // Keep if other Firestore uses, otherwise can remove
import de.hdodenhof.circleimageview.CircleImageView

class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditProfileActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var profileImageView: CircleImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var saveProfileButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        profileImageView = findViewById(R.id.editProfileImageView)
        fullNameEditText = findViewById(R.id.fullNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        saveProfileButton = findViewById(R.id.saveProfileButton)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val photoUrl = currentUser.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_account_circle_large)
                    .error(R.drawable.ic_account_circle_large)
                    .into(profileImageView)
                Log.d(TAG, "Loading profile image from URL: $photoUrl")
            } else {
                profileImageView.setImageResource(R.drawable.ic_account_circle_large)
                Log.d(TAG, "Using default profile image (no URL found).")
            }

            fullNameEditText.setText(currentUser.displayName)
            emailEditText.setText(currentUser.email)
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "User not logged in. Finishing EditProfileActivity.")
            finish()
            return
        }

        saveProfileButton.setOnClickListener {
            Log.d(TAG, "Save Profile button clicked. Attempting to save changes.")
            saveProfileChanges()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun saveProfileChanges() {
        val newFullName = fullNameEditText.text.toString().trim()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        var changesMade = false

        if (newFullName.isNotEmpty() && newFullName != currentUser.displayName) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(newFullName)
                .build()
            currentUser.updateProfile(profileUpdates)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "User profile (display name) updated successfully.")
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "Failed to update profile (display name): ${task.exception?.message}", task.exception)
                        Toast.makeText(this, "Failed to update profile: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            changesMade = true
        }

        if (!changesMade) {
            Toast.makeText(this, "No changes made.", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "No profile changes detected or made.")
        } else {
            Log.d(TAG, "Changes detected and save operations initiated.")
        }
    }
}