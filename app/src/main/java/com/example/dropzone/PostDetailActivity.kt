package com.example.dropzone

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dropzone.databinding.ActivityPostDetailBinding
import com.example.dropzone.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PostDetailActivity"
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityPostDetailBinding

    private var currentPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val toolbar: Toolbar = binding.toolbar
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

        binding.contactPosterButton.setOnClickListener {
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

        binding.deletePostButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun fetchPostDetails(postId: String) {
        binding.detailProgressBar.visibility = View.VISIBLE
        firestore.collection("posts").document(postId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                binding.detailProgressBar.visibility = View.GONE
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
                binding.detailProgressBar.visibility = View.GONE
                Toast.makeText(this, "Error fetching post: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error fetching document from Firestore: ${e.message}", e)
                finish()
            }
    }

    private fun displayPostDetails(post: Post) {
        binding.detailPostTitle.text = post.title
        binding.detailPostDescription.text = post.description
        binding.detailPostCategory.text = "Category: ${post.category}"
        binding.detailPostLocation.text = "Location: ${post.location ?: "Not specified"}"
        binding.detailPostStatus.text = "Status: ${post.status}"
        binding.detailPostPoster.text = "Posted by: ${post.userName}"

        if (post.status == "Lost") {
            binding.detailPostStatus.setBackgroundResource(R.drawable.status_lost_background)
        } else {
            binding.detailPostStatus.setBackgroundResource(R.drawable.status_found_background)
        }

        post.timestamp?.let {
            binding.detailPostTimestamp.text =
                "Posted: ${SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(it)}"
        } ?: run {
            binding.detailPostTimestamp.text = "Posted: N/A"
            Log.w(TAG, "Post timestamp is null for post: ${post.id}")
        }

        if (!post.imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(post.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_image_error)
                .into(binding.detailPostImage)
            binding.detailPostImage.visibility = View.VISIBLE
            Log.d(TAG, "Image loaded for post: ${post.id}")
        } else {
            binding.detailPostImage.visibility = View.GONE
            Log.i(TAG, "No image URL for post: ${post.id}. Image view hidden.")
        }

        auth.currentUser?.let { currentUser ->
            if (currentUser.uid == post.userId) {
                binding.contactPosterButton.visibility = View.GONE
                binding.deletePostButton.visibility = View.VISIBLE
            } else {
                binding.contactPosterButton.visibility = View.VISIBLE
                binding.deletePostButton.visibility = View.GONE
            }
        } ?: run {
            binding.contactPosterButton.visibility = View.GONE
            binding.deletePostButton.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePost()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
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

            binding.detailProgressBar.visibility = View.VISIBLE
            firestore.collection("posts").document(post.id)
                .delete()
                .addOnSuccessListener {
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Post ${post.id} deleted successfully.")
                    finish()
                }
                .addOnFailureListener { e ->
                    binding.detailProgressBar.visibility = View.GONE
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
}
