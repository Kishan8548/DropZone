package com.example.dropzone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.dropzone.adapters.PostAdapter
import com.example.dropzone.databinding.ActivityProfileBinding
import com.example.dropzone.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ProfileActivity : AppCompatActivity(), PostAdapter.OnItemClickListener {

    companion object {
        private const val TAG = "ProfileActivity"
    }

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postAdapter: PostAdapter

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"

        binding.profilePostsRecyclerView.layoutManager = LinearLayoutManager(this)
        postAdapter = PostAdapter(emptyList(), this)
        binding.profilePostsRecyclerView.adapter = postAdapter

        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            binding.userEmailTextView.text = currentUser.email

            binding.userNameTextView.text = currentUser.displayName
                ?: currentUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
                        ?: "DropZone User"

            val avatarUrl = currentUser.photoUrl?.toString()
            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle_large)
                    .error(R.drawable.ic_account_circle_large)
                    .into(binding.userAvatarImageView)
                Log.d(TAG, "Loading user avatar from URL: $avatarUrl")
            } else {
                Glide.with(this)
                    .load(R.drawable.ic_account_circle_large)
                    .into(binding.userAvatarImageView)
                Log.d(TAG, "Using default user avatar (no URL found).")
            }

            fetchUserPosts()
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "User not logged in. Redirecting to authentication screen.")
            navigateToAuth()
            return
        }

        binding.editProfileButton.setOnClickListener {
            Log.d(TAG, "Edit Profile button clicked. Navigating to EditProfileActivity.")
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        binding.logoutItem.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "User logged out successfully.")
            navigateToAuth()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        Log.d(TAG, "Toolbar back button pressed.")
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchUserPosts() {
        currentUserId?.let { userId ->
            binding.profileProgressBar.visibility = View.VISIBLE
            firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    binding.profileProgressBar.visibility = View.GONE
                    val userPosts = querySnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Post::class.java)?.apply { id = doc.id }
                    }
                    postAdapter.updatePosts(userPosts)
                    if (userPosts.isEmpty()) {
                        Toast.makeText(this, "You haven't posted any items yet.", Toast.LENGTH_SHORT).show()
                        Log.i(TAG, "No posts found for the current user (ID: $userId).")
                    } else {
                        Log.d(TAG, "Fetched ${userPosts.size} posts for user (ID: $userId).")
                    }
                }
                .addOnFailureListener { e ->
                    binding.profileProgressBar.visibility = View.GONE
                    Log.e(TAG, "Error fetching user posts: ${e.message}", e)
                    Toast.makeText(this, "Error loading your posts: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } ?: run {
            Toast.makeText(this, "User ID not available.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Current user ID is null during fetchUserPosts. Cannot fetch posts.")
            navigateToAuth()
        }
    }

    override fun onItemClick(post: Post) {
        Log.d(TAG, "User's post clicked: ${post.id}. Navigating to PostDetailActivity.")
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("postId", post.id)
        startActivity(intent)
    }

    private fun navigateToAuth() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
