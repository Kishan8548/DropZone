package com.example.dropzone

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.Glide
import com.example.dropzone.databinding.ActivityPostDetailBinding
import com.example.dropzone.models.GeminiRequest
import com.example.dropzone.models.GeminiResponse
import com.example.dropzone.models.Post
import com.example.dropzone.services.RetrofitClient
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Locale

class PostDetailActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PostDetailActivity"
        private const val MAX_CANDIDATE_POSTS = 5
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
        val navIcon = binding.toolbar.navigationIcon
        navIcon?.setTint(
            MaterialColors.getColor(binding.toolbar, com.google.android.material.R.attr.colorOnPrimary)
        )
        supportActionBar?.title = "Post Details"

        val postId = intent.getStringExtra("postId")
            ?: intent.getStringExtra("POST_ID")

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
                    android.util.Patterns.EMAIL_ADDRESS.matcher(post.userName).matches()
                ) {
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

        if (post.status.equals("Lost", ignoreCase = true)) {
            binding.detailPostStatus.setBackgroundResource(R.drawable.status_lost_background)
            maybeShowGeminiMatches(post)
        } else {
            binding.detailPostStatus.setBackgroundResource(R.drawable.status_found_background)
            binding.geminiMatchCard.visibility = View.GONE
        }

        post.timestamp?.let {
            binding.detailPostTimestamp.text =
                "Posted: ${SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(it)}"
        } ?: run {
            binding.detailPostTimestamp.text = "Posted: N/A"
            Log.w(TAG, "Post timestamp is null for post: ${post.id}")
        }

        Glide.with(this)
            .load(post.imageUrl)
            .placeholder(R.drawable.ic_image_placeholder)
            .error(R.drawable.ic_image_placeholder)
            .into(binding.detailPostImage)

        binding.detailPostImage.visibility = View.VISIBLE

        if (post.imageUrl.isNullOrEmpty()) {
            Log.i(TAG, "No image URL for post: ${post.id}. Showing placeholder.")
        } else {
            Log.d(TAG, "Image loaded for post: ${post.id}")
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

    private fun maybeShowGeminiMatches(post: Post) {
        val currentUser = auth.currentUser
        if (currentUser == null || currentUser.uid != post.userId) {
            binding.geminiMatchCard.visibility = View.GONE
            return
        }

        binding.geminiMatchCard.visibility = View.VISIBLE
        fetchGeminiMatches(post)
    }

    private fun fetchGeminiMatches(lostPost: Post) {
        binding.geminiMatchProgress.visibility = View.VISIBLE
        binding.geminiMatchEmptyText.visibility = View.GONE
        binding.geminiMatchContainer.visibility = View.GONE
        binding.geminiMatchContainer.removeAllViews()

        firestore.collection("posts")
            .whereEqualTo("status", "Found")
            .get()
            .addOnSuccessListener { snapshot ->
                val foundPosts = snapshot.documents.mapNotNull { document ->
                    document.toObject(Post::class.java)?.apply { id = document.id }
                }

                if (foundPosts.isEmpty()) {
                    showNoGeminiMatches("No matching found posts right now.")
                    return@addOnSuccessListener
                }

                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    val fallbackMatches = rankFoundPosts(lostPost, foundPosts, emptyList())
                    if (fallbackMatches.isNotEmpty()) {
                        renderGeminiMatches(fallbackMatches)
                    } else {
                        showNoGeminiMatches("No matching found posts right now.")
                    }
                    return@addOnSuccessListener
                }

                requestGeminiTerms(lostPost, foundPosts)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load found posts for Gemini matching.", e)
                showNoGeminiMatches("Unable to load matching posts right now.")
            }
    }

    private fun requestGeminiTerms(lostPost: Post, foundPosts: List<Post>) {
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(
                    parts = listOf(
                        GeminiRequest.Content.Part(buildGeminiTermsPrompt(lostPost))
                    )
                )
            )
        )

        RetrofitClient.instance.generateText(request)
            .enqueue(object : Callback<GeminiResponse> {
                override fun onResponse(
                    call: Call<GeminiResponse>,
                    response: Response<GeminiResponse>
                ) {
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Gemini detail matching failed with code ${response.code()}")
                        val fallbackMatches = rankFoundPosts(lostPost, foundPosts, emptyList())
                        if (fallbackMatches.isNotEmpty()) {
                            renderGeminiMatches(fallbackMatches)
                        } else {
                            showNoGeminiMatches("No matching found posts right now.")
                        }
                        return
                    }

                    val rawText = response.body()
                        ?.candidates
                        ?.firstOrNull()
                        ?.content
                        ?.parts
                        ?.joinToString("\n") { it.text.orEmpty() }
                        .orEmpty()

                    val generatedTerms = parseGeminiTerms(rawText)
                    val matches = rankFoundPosts(lostPost, foundPosts, generatedTerms)
                    if (matches.isNotEmpty()) {
                        renderGeminiMatches(matches)
                    } else {
                        showNoGeminiMatches("No matching found posts right now.")
                    }
                }

                override fun onFailure(call: Call<GeminiResponse>, t: Throwable) {
                    Log.e(TAG, "Gemini detail matching request failed.", t)
                    val fallbackMatches = rankFoundPosts(lostPost, foundPosts, emptyList())
                    if (fallbackMatches.isNotEmpty()) {
                        renderGeminiMatches(fallbackMatches)
                    } else {
                        showNoGeminiMatches("Unable to load matching posts right now.")
                    }
                }
            })
    }

    private fun buildGeminiTermsPrompt(lostPost: Post): String {
        return """
            Generate 10 to 15 short search terms, synonyms, alternate names, and related descriptive phrases
            for this lost item based only on its title and description.
            Use words and short phrases that could appear in a found-post title or description.
            Return only a JSON array of strings and nothing else.

            Item title:
            title: ${lostPost.title}

            Item description:
            description: ${lostPost.description}
        """.trimIndent()
    }

    private fun parseGeminiTerms(rawText: String): List<String> {
        val cleaned = rawText
            .replace("```json", "")
            .replace("```", "")
            .trim()

        return try {
            val arrayMatch = Regex("\\[[\\s\\S]*?\\]").find(cleaned)?.value ?: cleaned
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(arrayMatch, type)
                ?.map { it.trim() }
                ?.filter { it.length > 2 }
                ?.distinct()
                ?: emptyList()
        } catch (_: Exception) {
            cleaned
                .split(",", "\n")
                .map { it.trim().trim('"') }
                .filter { it.length > 2 }
                .distinct()
        }
    }

    private fun rankFoundPosts(
        lostPost: Post,
        foundPosts: List<Post>,
        generatedTerms: List<String>
    ): List<Post> {
        return foundPosts
            .map { post -> post to scoreFoundPostMatch(lostPost, post, generatedTerms) }
            .filter { (_, score) -> score >= 3 }
            .sortedByDescending { (_, score) -> score }
            .take(MAX_CANDIDATE_POSTS)
            .map { (post, _) -> post }
    }

    private fun scoreFoundPostMatch(
        lostPost: Post,
        foundPost: Post,
        generatedTerms: List<String>
    ): Int {
        val postTitleTokens = tokenize(foundPost.title)
        val postDescriptionTokens = tokenize(foundPost.description)
        val postText = "${foundPost.title} ${foundPost.description}".lowercase(Locale.getDefault())

        val lostTitleTokens = tokenize(lostPost.title)
        val lostDescriptionTokens = tokenize(lostPost.description)
        val relatedTokens = generatedTerms.flatMap { tokenize(it).toList() }.toSet()
        val relatedPhrases = generatedTerms
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.contains(" ") && it.length > 3 }
            .toSet()

        var score = 0
        score += lostTitleTokens.intersect(postTitleTokens).size * 4
        score += lostTitleTokens.intersect(postDescriptionTokens).size * 3
        score += lostDescriptionTokens.intersect(postTitleTokens).size * 3
        score += lostDescriptionTokens.intersect(postDescriptionTokens).size * 2
        score += relatedTokens.intersect(postTitleTokens).size * 2
        score += relatedTokens.intersect(postDescriptionTokens).size
        score += relatedPhrases.count { phrase -> postText.contains(phrase) } * 3

        if (foundPost.category.equals(lostPost.category, ignoreCase = true)) {
            score += 2
        }

        return score
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase(Locale.getDefault())
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 2 }
            .toSet()
    }

    private fun renderGeminiMatches(matches: List<Post>) {
        binding.geminiMatchProgress.visibility = View.GONE
        binding.geminiMatchEmptyText.visibility = View.GONE
        binding.geminiMatchContainer.removeAllViews()

        matches.forEach { post ->
            val matchView = TextView(this).apply {
                text = buildString {
                    append(post.title.ifBlank { "Untitled item" })
                    if (post.description.isNotBlank()) {
                        append("\n")
                        append(
                            if (post.description.length > 110) {
                                post.description.take(110) + "..."
                            } else {
                                post.description
                            }
                        )
                    }
                }
                textSize = 15f
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(R.drawable.button_rounded_background)
                setOnClickListener {
                    startActivity(Intent(this@PostDetailActivity, PostDetailActivity::class.java).apply {
                        putExtra("postId", post.id)
                    })
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            binding.geminiMatchContainer.addView(matchView, params)
        }

        binding.geminiMatchContainer.visibility = View.VISIBLE
    }

    private fun showNoGeminiMatches(message: String) {
        binding.geminiMatchProgress.visibility = View.GONE
        binding.geminiMatchContainer.visibility = View.GONE
        binding.geminiMatchEmptyText.text = message
        binding.geminiMatchEmptyText.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
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
                    Log.d(TAG, "Post ${post.id} deleted successfully from Firestore.")
                    binding.detailProgressBar.visibility = View.GONE
                    Toast.makeText(this, "Post deleted successfully!", Toast.LENGTH_SHORT).show()
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
