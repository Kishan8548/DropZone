package com.example.dropzone

import android.content.Intent
import android.nfc.Tag
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
import com.example.dropzone.databinding.ActivityMainBinding
import com.example.dropzone.models.Post

class MainActivity : AppCompatActivity(), PostAdapter.OnItemClickListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var postAdapter: PostAdapter
    private lateinit var binding: ActivityMainBinding

    private var currentFilter: String = "All"
    private var allPosts: List<Post> = emptyList()

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition{
            !viewModel.isReady.value
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        postAdapter = PostAdapter(emptyList(), this)
        binding.postsRecyclerView.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }


        binding.addPostFab.setOnClickListener {
            Log.d(TAG, "FAB clicked. Navigating to AddPostActivity.")
            startActivity(Intent(this, AddPostActivity::class.java))
        }

        binding.chipAll.setOnClickListener {
            currentFilter = "All"
            displayPostsByFilter()
        }
        binding.chipLost.setOnClickListener {
            currentFilter = "Lost"
            displayPostsByFilter()
        }
        binding.chipFound.setOnClickListener {
            currentFilter = "Found"
            displayPostsByFilter()
        }
        fetchPostsFromFirestore()
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
            Log.d(TAG, "User logged in: ${auth.currentUser?.email}. Re-fetching posts.")
//            fetchPosts()
        }
    }

    private fun fetchPostsFromFirestore() {
        binding.progressBar.visibility = View.VISIBLE
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                binding.progressBar.visibility = View.GONE
                val posts = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Post::class.java)?.apply { id = document.id }
                }
                allPosts = posts
                displayPostsByFilter()
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading posts: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPostsByFilter() {
        val filteredList = when (currentFilter) {
            "Lost" -> allPosts.filter { it.status == "Lost" }
            "Found" -> allPosts.filter { it.status == "Found" }
            else -> allPosts
        }
        postAdapter.updatePosts(filteredList)
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No posts found for '$currentFilter' filter.", Toast.LENGTH_SHORT).show()
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
            R.id.action_payment -> {
                Log.d(TAG, "Navigating to PaymentActivity.")
                val intent = Intent(this, PaymentActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
