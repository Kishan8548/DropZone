package com.example.dropzone

import android.content.Intent
import android.os.Bundle
import android.util.Log // Ensure this is imported
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout // Import for clickable menu items
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
// Removed: import com.google.android.material.tabs.TabLayout // No longer needed if TabLayout removed from XML
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.dropzone.adapters.PostAdapter
import com.example.dropzone.models.Post

class ProfileActivity : AppCompatActivity(), PostAdapter.OnItemClickListener {

    companion object {
        private const val TAG = "ProfileActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var userAvatarImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var editProfileButton: Button

    private lateinit var logoutItem: LinearLayout

    private lateinit var profilePostsRecyclerView: RecyclerView
    private lateinit var profileProgressBar: ProgressBar

    private lateinit var postAdapter: PostAdapter

    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI Views
        userAvatarImageView = findViewById(R.id.userAvatarImageView)
        userNameTextView = findViewById(R.id.userNameTextView)
        userEmailTextView = findViewById(R.id.userEmailTextView)
        editProfileButton = findViewById(R.id.editProfileButton)

        logoutItem = findViewById(R.id.logoutItem)

        profilePostsRecyclerView = findViewById(R.id.profilePostsRecyclerView)
        profileProgressBar = findViewById(R.id.profileProgressBar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Profile"
        profilePostsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        postAdapter = PostAdapter(emptyList(), this)
        profilePostsRecyclerView.adapter = postAdapter

        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUserId = currentUser.uid
            userEmailTextView.text = currentUser.email

            userNameTextView.text = currentUser.displayName
                ?: currentUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
                        ?: "DropZone User"

            val avatarUrl = currentUser.photoUrl?.toString()
            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_account_circle_large)
                    .error(R.drawable.ic_account_circle_large)
                    .into(userAvatarImageView)
                Log.d(TAG, "Loading user avatar from URL: $avatarUrl")
            } else {
                Glide.with(this)
                    .load(R.drawable.ic_account_circle_large)
                    .into(userAvatarImageView)
                Log.d(TAG, "Using default user avatar (no URL found).")
            }

            fetchUserPosts()
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "User not logged in. Redirecting to authentication screen.")
            navigateToAuth()
            return
        }

        editProfileButton.setOnClickListener {
            Log.d(TAG, "Edit Profile button clicked. Navigating to EditProfileActivity.")
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        logoutItem.setOnClickListener {
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
            profileProgressBar.visibility = View.VISIBLE
            firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    profileProgressBar.visibility = View.GONE
                    val userPosts = mutableListOf<Post>()
                    for (document in querySnapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        post?.id = document.id
                        post?.let { userPosts.add(it) }
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
                    profileProgressBar.visibility = View.GONE
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