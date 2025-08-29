package com.example.dropzone

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.dropzone.models.Post
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PostDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PostDetailActivity"
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var detailTitle: TextView
    private lateinit var detailDescription: TextView
    private lateinit var detailCategory: TextView
    private lateinit var detailLocation: TextView
    private lateinit var detailStatus: TextView
    private lateinit var detailTimestamp: TextView
    private lateinit var detailPoster: TextView
    private lateinit var detailImageView: ImageView
    private lateinit var contactPosterButton: Button
    private lateinit var deletePostButton: Button
    private lateinit var progressBar: ProgressBar

    private var currentPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        detailTitle = findViewById(R.id.detailPostTitle)
        detailDescription = findViewById(R.id.detailPostDescription)
        detailCategory = findViewById(R.id.detailPostCategory)
        detailLocation = findViewById(R.id.detailPostLocation)
        detailStatus = findViewById(R.id.detailPostStatus)
        detailTimestamp = findViewById(R.id.detailPostTimestamp)
        detailPoster = findViewById(R.id.detailPostPoster)
        detailImageView = findViewById(R.id.detailPostImage)
        contactPosterButton = findViewById(R.id.contactPosterButton)
        deletePostButton = findViewById(R.id.deletePostButton)
        progressBar = findViewById(R.id.detailProgressBar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Post Details"

        val postId = intent.getStringExtra("postId")

        if (postId == null) {
            Toast.makeText(this, "Post ID not found.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "PostDetailActivity started with null postId.")
            finish()
            return
        }

        Log.d(TAG, "Fetching details for post ID: $postId")
        fetchPostDetails(postId)

        contactPosterButton.setOnClickListener {
            currentPost?.let { post ->
                val currentUser = auth.currentUser
                if (currentUser != null && currentUser.uid == post.userId) {
                    Toast.makeText(this, "This is your own post.", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Contact button clicked for own post. Hiding.")
                } else if (!post.userName.isNullOrEmpty() &&
                    android.util.Patterns.EMAIL_ADDRESS.matcher(post.userName).matches()) {
                    Log.d(TAG, "Attempting to send email to ${post.userName} for post: ${post.title}")
                    sendEmailToPoster(post.userName, post.title)
                } else {
                    Toast.makeText(this, "Contact information not available.", Toast.LENGTH_SHORT).show()
                    Log.w(TAG, "Contact information not available for post user: ${post.userName}")
                }
            }
        }

        deletePostButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchPostDetails(postId: String) {
        progressBar.visibility = View.VISIBLE
        firestore.collection("posts").document(postId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                progressBar.visibility = View.GONE
                if (documentSnapshot.exists()) {
                    val post = documentSnapshot.toObject(Post::class.java)
                    post?.id = documentSnapshot.id
                    post?.let {
                        currentPost = it
                        Log.d(TAG, "Post details fetched successfully: ${it.title}")
                        displayPostDetails(it)
                    } ?: run {
                        Toast.makeText(this, "Post data is malformed.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Fetched document is malformed or null for ID: $postId")
                        finish()
                    }
                } else {
                    Toast.makeText(this, "Post not found.", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Post not found in Firestore for ID: $postId")
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching post: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error fetching document from Firestore: ${e.message}", e)
                finish()
            }
    }

    private fun displayPostDetails(post: Post) {
        detailTitle.text = post.title
        detailDescription.text = post.description
        detailCategory.text = "Category: ${post.category}"
        detailLocation.text = "Location: ${post.location ?: "Not specified"}"
        detailStatus.text = "Status: ${post.status}"
        detailPoster.text = "Posted by: ${post.userName}"

        if (post.status == "Lost") {
            detailStatus.setBackgroundResource(R.drawable.status_lost_background)
        } else {
            detailStatus.setBackgroundResource(R.drawable.status_found_background)
        }

        post.timestamp?.let {
            detailTimestamp.text = "Posted: ${SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(it)}"
        } ?: run {
            detailTimestamp.text = "Posted: N/A"
            Log.w(TAG, "Post timestamp is null for post: ${post.id}")
        }

        if (!post.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(post.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(detailImageView)
            detailImageView.visibility = View.VISIBLE
            Log.d(TAG, "Image loaded for post: ${post.id}")
        } else {
            detailImageView.visibility = View.GONE
            Log.i(TAG, "No image URL for post: ${post.id}. Image view hidden.")
        }

        auth.currentUser?.let { currentUser ->
            if (currentUser.uid == post.userId) {
                contactPosterButton.visibility = View.GONE
                deletePostButton.visibility = View.VISIBLE
            } else {
                contactPosterButton.visibility = View.VISIBLE
                deletePostButton.visibility = View.GONE
            }
        } ?: run {
            contactPosterButton.visibility = View.GONE
            deletePostButton.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                deletePost()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deletePost() {
        currentPost?.let { post ->
            val loggedInUserUid = auth.currentUser?.uid
            Log.d(TAG, "Attempting to delete post: ${post.id}")
            Log.d(TAG, "Logged-in User UID: $loggedInUserUid")
            Log.d(TAG, "Post Owner User ID: ${post.userId}")

            progressBar.visibility = View.VISIBLE
            firestore.collection("posts").document(post.id)
                .delete()
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Post ${post.id} deleted successfully.")
                    finish()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error deleting post: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Error deleting post ${post.id}: ${e.message}", e)
                }
        } ?: run {
            Toast.makeText(this, "No post selected for deletion.", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "deletePost called when currentPost is null.")
        }
    }

    private fun sendEmailToPoster(email: String, subject: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Regarding your post: $subject")
            setPackage("com.google.android.gm")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gmail app not found", Toast.LENGTH_SHORT).show()
        }
    }


    private fun formatTimestampDetailed(timestamp: Date): String {
        return SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(timestamp)
    }
}
