package com.example.dropzone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.dropzone.adapters.PostAdapter
import com.example.dropzone.models.Post

class MainActivity : AppCompatActivity(), PostAdapter.OnItemClickListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var postAdapter: PostAdapter
    private lateinit var addPostFab: FloatingActionButton
    private lateinit var progressBar: ProgressBar
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var chipAll: Chip
    private lateinit var chipLost: Chip
    private lateinit var chipFound: Chip

    private var currentFilter: String = "All"
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{
            !viewModel.isReady.value
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        addPostFab = findViewById(R.id.addPostFab)
        progressBar = findViewById(R.id.progressBar)
        filterChipGroup = findViewById(R.id.filterChipGroup)
        chipAll = findViewById(R.id.chipAll)
        chipLost = findViewById(R.id.chipLost)
        chipFound = findViewById(R.id.chipFound)

        postAdapter = PostAdapter(emptyList(), this)
        postsRecyclerView.adapter = postAdapter
        postsRecyclerView.layoutManager = LinearLayoutManager(this)

        addPostFab.setOnClickListener {
            Log.d(TAG, "FAB clicked. Navigating to AddPostActivity.")
            val intent = Intent(this, AddPostActivity::class.java)
            startActivity(intent)
        }

        chipAll.setOnClickListener {
            currentFilter = "All"
            Log.d(TAG, "Filter changed to: All")
            fetchPosts()
        }
        chipLost.setOnClickListener {
            currentFilter = "Lost"
            Log.d(TAG, "Filter changed to: Lost")
            fetchPosts()
        }
        chipFound.setOnClickListener {
            currentFilter = "Found"
            Log.d(TAG, "Filter changed to: Found")
            fetchPosts()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            Log.w(TAG, "User not authenticated. Redirecting to AuthActivity.")
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            // This is the CRITICAL change: call fetchPosts() every time the activity starts.
            Log.d(TAG, "User logged in: ${auth.currentUser?.email}. Re-fetching posts.")
            fetchPosts()
        }
    }

    private fun fetchPosts() {
        progressBar.visibility = View.VISIBLE
        val postsCollection = firestore.collection("posts")

        var query: Query = postsCollection.orderBy("timestamp", Query.Direction.DESCENDING)

        when (currentFilter) {
            "Lost" -> query = query.whereEqualTo("status", "Lost")
            "Found" -> query = query.whereEqualTo("status", "Found")
        }

        query.get()
            .addOnSuccessListener { querySnapshot ->
                progressBar.visibility = View.GONE
                val posts = mutableListOf<Post>()
                for (document in querySnapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    post?.id = document.id
                    post?.let { posts.add(it) }
                }
                postAdapter.updatePosts(posts)
                if (posts.isEmpty()) {
                    Log.i(TAG, "No posts found for '$currentFilter' filter.")
                    Toast.makeText(this, "No posts found for '$currentFilter' filter.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "Fetched ${posts.size} posts for '$currentFilter' filter.")
                }
            }
            .addOnFailureListener { exception ->
                progressBar.visibility = View.GONE
                Log.e(TAG, "Error fetching posts: ${exception.message}", exception)
                Toast.makeText(this, "Error loading posts: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onItemClick(post: Post) {
        Log.d(TAG, "Post clicked: ${post.id}. Navigating to PostDetailActivity.")
        val intent = Intent(this, PostDetailActivity::class.java)
        intent.putExtra("postId", post.id)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "User logged out.")
                val intent = Intent(this, AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            R.id.action_profile -> {
                Log.d(TAG, "Navigating to ProfileActivity.")
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
