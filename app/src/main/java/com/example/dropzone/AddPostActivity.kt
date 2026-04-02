package com.example.dropzone

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.dropzone.models.CloudinaryUploadResponse
import com.example.dropzone.models.GeminiRequest
import com.example.dropzone.models.Post
import com.example.dropzone.services.CloudinaryRetrofitClient
import com.example.dropzone.services.RetrofitClient
import com.google.android.material.color.MaterialColors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.Locale

class AddPostActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddPostActivity"
        private const val CLOUDINARY_IMAGE_QUALITY = 85
        private const val MAX_CANDIDATE_POSTS = 8
    }

    private data class PendingPostDraft(
        val userId: String,
        val userName: String,
        val title: String,
        val description: String,
        val category: String,
        val location: String?,
        val status: String
    )

    private data class UploadedImage(
        val url: String,
        val publicId: String?
    )

    private data class RankedPost(
        val post: Post,
        val score: Int
    )

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var postTitleEditText: EditText
    private lateinit var postDescriptionEditText: EditText
    private lateinit var postCategorySpinner: Spinner
    private lateinit var postLocationEditText: EditText
    private lateinit var postStatusRadioGroup: RadioGroup
    private lateinit var statusLostRadioButton: RadioButton
    private lateinit var statusFoundRadioButton: RadioButton
    private lateinit var postImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var createPostButton: Button
    private lateinit var progressBar: ProgressBar

    private var selectedImageUri: Uri? = null

    private val pickImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                selectedImageUri = it
                postImageView.setImageURI(it)
                postImageView.visibility = View.VISIBLE
                Log.d(TAG, "Image selected from gallery: $it")
            } ?: run {
                Log.w(TAG, "Image selection from gallery cancelled or failed.")
            }
        }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
            bitmap?.let {
                val uri = getImageUri(it)
                selectedImageUri = uri
                postImageView.setImageURI(uri)
                postImageView.visibility = View.VISIBLE
                Log.d(TAG, "Picture taken and processed. URI: $uri")
            } ?: run {
                Log.w(TAG, "Picture taking cancelled or failed.")
            }
        }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            val deniedPermissions = mutableListOf<String>()

            for (permission in permissions.keys) {
                if (permissions[permission] == false) {
                    allGranted = false
                    deniedPermissions.add(permission)
                }
            }

            if (allGranted) {
                Log.d(TAG, "All requested permissions granted.")
                showImagePickerDialog()
            } else {
                Toast.makeText(this, "Permissions required to pick/take image.", Toast.LENGTH_SHORT)
                    .show()
                Log.e(TAG, "Permissions denied: ${deniedPermissions.joinToString()}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        postTitleEditText = findViewById(R.id.postTitleEditText)
        postDescriptionEditText = findViewById(R.id.postDescriptionEditText)
        postCategorySpinner = findViewById(R.id.postCategorySpinner)
        postLocationEditText = findViewById(R.id.postLocationEditText)
        postStatusRadioGroup = findViewById(R.id.postStatusRadioGroup)
        statusLostRadioButton = findViewById(R.id.statusLostRadioButton)
        statusFoundRadioButton = findViewById(R.id.statusFoundRadioButton)
        postImageView = findViewById(R.id.postImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        createPostButton = findViewById(R.id.createPostButton)
        progressBar = findViewById(R.id.progressBar)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.navigationIcon?.setTint(
            MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnPrimary)
        )
        supportActionBar?.title = "New Lost or Found Item"

        val categories = arrayOf(
            "Select Category",
            "ID Card",
            "Electronics",
            "Books",
            "Keys",
            "Documents",
            "Accessories",
            "Others"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        postCategorySpinner.adapter = adapter

        statusLostRadioButton.isChecked = true

        selectImageButton.setOnClickListener {
            Log.d(TAG, "Select Image button clicked. Checking permissions.")
            checkPermissionsAndShowPicker()
        }

        createPostButton.setOnClickListener {
            hideKeyboard(this)
            createPost()
        }
    }

    fun hideKeyboard(activity: Activity) {
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun checkPermissionsAndShowPicker() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> takePicture.launch(null)
                options[item] == "Choose from Gallery" -> pickImageFromGallery.launch("image/*")
                options[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun getImageUri(inImage: Bitmap): Uri? {
        return try {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                contentResolver,
                inImage,
                "DropZone_Image_${System.currentTimeMillis()}",
                null
            )
            Uri.parse(path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get URI from Bitmap: ${e.message}", e)
            Toast.makeText(this, "Could not save image from camera: ${e.message}", Toast.LENGTH_LONG)
                .show()
            null
        }
    }

    private fun createPost() {
        val title = postTitleEditText.text.toString().trim()
        val description = postDescriptionEditText.text.toString().trim()
        val category = postCategorySpinner.selectedItem.toString()
        val location = postLocationEditText.text.toString().trim().takeIf { it.isNotEmpty() }
        val statusRadioButtonId = postStatusRadioGroup.checkedRadioButtonId
        val status = findViewById<RadioButton>(statusRadioButtonId)?.text.toString()

        if (title.isEmpty() || description.isEmpty() || category == "Select Category" || status.isEmpty()) {
            Toast.makeText(
                this,
                "Please fill in all required fields and select a category.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val userId = auth.currentUser?.uid
        val userName = auth.currentUser?.email

        if (userId == null || userName == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val draft = PendingPostDraft(
            userId = userId,
            userName = userName,
            title = title,
            description = description,
            category = category,
            location = location,
            status = status
        )

        setPostingState(true)
        if (draft.status.equals("Lost", ignoreCase = true)) {
            fetchSimilarFoundPostSuggestions(draft)
        } else {
            uploadImageAndSavePost(draft)
        }
    }

    private fun fetchSimilarFoundPostSuggestions(draft: PendingPostDraft) {
        firestore.collection("posts")
            .whereEqualTo("status", "Found")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val foundPosts = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Post::class.java)?.apply { id = document.id }
                }
                if (foundPosts.isEmpty()) {
                    uploadImageAndSavePost(draft)
                    return@addOnSuccessListener
                }

                if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                    val fallbackMatches = findMatchingPosts(draft, foundPosts, emptyList())
                    if (fallbackMatches.isNotEmpty()) {
                        showSimilarPostsDialog(fallbackMatches, draft)
                    } else {
                        uploadImageAndSavePost(draft)
                    }
                    return@addOnSuccessListener
                }

                requestGeminiRelatedTerms(draft, foundPosts)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch found posts for suggestion check.", e)
                uploadImageAndSavePost(draft)
            }
    }

    private fun requestGeminiRelatedTerms(draft: PendingPostDraft, foundPosts: List<Post>) {
        val prompt = buildGeminiTermsPrompt(draft)
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(
                    parts = listOf(GeminiRequest.Content.Part(prompt))
                )
            )
        )

        RetrofitClient.instance.generateText(request).enqueue(object : Callback<com.example.dropzone.models.GeminiResponse> {
            override fun onResponse(
                call: Call<com.example.dropzone.models.GeminiResponse>,
                response: Response<com.example.dropzone.models.GeminiResponse>
            ) {
                if (!response.isSuccessful) {
                    Log.w(TAG, "Gemini term generation failed with code ${response.code()}")
                    val fallbackMatches = findMatchingPosts(draft, foundPosts, emptyList())
                    if (fallbackMatches.isNotEmpty()) {
                        showSimilarPostsDialog(fallbackMatches, draft)
                    } else {
                        uploadImageAndSavePost(draft)
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
                val matchedPosts = findMatchingPosts(draft, foundPosts, generatedTerms)
                if (matchedPosts.isNotEmpty()) {
                    showSimilarPostsDialog(matchedPosts, draft)
                } else {
                    uploadImageAndSavePost(draft)
                }
            }

            override fun onFailure(call: Call<com.example.dropzone.models.GeminiResponse>, t: Throwable) {
                Log.e(TAG, "Gemini term generation request failed.", t)
                val fallbackMatches = findMatchingPosts(draft, foundPosts, emptyList())
                if (fallbackMatches.isNotEmpty()) {
                    showSimilarPostsDialog(fallbackMatches, draft)
                } else {
                    uploadImageAndSavePost(draft)
                }
            }
        })
    }

    private fun showSimilarPostsDialog(similarPosts: List<Post>, draft: PendingPostDraft) {
        setPostingState(false)

        val titles = similarPosts.map { post ->
            buildString {
                append(post.title.ifBlank { "Untitled item" })
                append(" (")
                append(post.category.ifBlank { "Unknown category" })
                append(")")
            }
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Similar found posts found")
            .setMessage("These found-item posts may match your lost item. Tap one to check it, or continue posting.")
            .setItems(titles) { _, which ->
                startActivity(
                    android.content.Intent(this, PostDetailActivity::class.java).apply {
                        putExtra("postId", similarPosts[which].id)
                    }
                )
            }
            .setPositiveButton("Continue Post") { _, _ ->
                setPostingState(true)
                uploadImageAndSavePost(draft)
            }
            .setNegativeButton("Edit Post", null)
            .show()
    }

    private fun uploadImageAndSavePost(draft: PendingPostDraft) {
        val imageUri = selectedImageUri
        if (imageUri == null) {
            savePostToFirestore(draft, null)
            return
        }

        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isBlank() || BuildConfig.CLOUDINARY_UPLOAD_PRESET.isBlank()) {
            setPostingState(false)
            Toast.makeText(
                this,
                "Cloudinary is not configured yet. Add cloud name and upload preset first.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val imageBytes = compressImageForUpload(imageUri)
        if (imageBytes == null) {
            setPostingState(false)
            Toast.makeText(this, "Unable to process the selected image.", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes)
        val filePart = MultipartBody.Part.createFormData(
            "file",
            "post_${System.currentTimeMillis()}.jpg",
            imageBody
        )
        val uploadPresetBody = RequestBody.create(
            MediaType.parse("text/plain"),
            BuildConfig.CLOUDINARY_UPLOAD_PRESET
        )

        CloudinaryRetrofitClient.instance
            .uploadImage(BuildConfig.CLOUDINARY_CLOUD_NAME, filePart, uploadPresetBody)
            .enqueue(object : Callback<CloudinaryUploadResponse> {
                override fun onResponse(
                    call: Call<CloudinaryUploadResponse>,
                    response: Response<CloudinaryUploadResponse>
                ) {
                    if (!response.isSuccessful || response.body() == null) {
                        setPostingState(false)
                        Toast.makeText(
                            this@AddPostActivity,
                            "Image upload failed. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e(TAG, "Cloudinary upload failed with code ${response.code()}")
                        return
                    }

                    val uploadResponse = response.body()!!
                    val optimizedUrl = buildOptimizedCloudinaryUrl(uploadResponse)
                    if (optimizedUrl == null) {
                        setPostingState(false)
                        Toast.makeText(
                            this@AddPostActivity,
                            "Image upload completed, but no image URL was returned.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }

                    savePostToFirestore(
                        draft,
                        UploadedImage(
                            url = optimizedUrl,
                            publicId = uploadResponse.publicId
                        )
                    )
                }

                override fun onFailure(call: Call<CloudinaryUploadResponse>, t: Throwable) {
                    setPostingState(false)
                    Toast.makeText(
                        this@AddPostActivity,
                        "Image upload failed: ${t.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "Cloudinary upload failed.", t)
                }
            })
    }

    private fun savePostToFirestore(draft: PendingPostDraft, uploadedImage: UploadedImage?) {
        val newPost = Post(
            userId = draft.userId,
            userName = draft.userName,
            title = draft.title,
            description = draft.description,
            category = draft.category,
            location = draft.location,
            status = draft.status,
            imageUrl = uploadedImage?.url,
            imagePublicId = uploadedImage?.publicId,
            lowerCaseTitle = draft.title.lowercase(Locale.getDefault())
        )

        firestore.collection("posts")
            .add(newPost)
            .addOnSuccessListener {
                setPostingState(false)
                Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show()
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                setPostingState(false)
                Toast.makeText(this, "Error creating post: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding document to Firestore.", e)
            }
    }

    private fun compressImageForUpload(imageUri: Uri): ByteArray? {
        return try {
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
                ByteArrayOutputStream().use { outputStream ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        CLOUDINARY_IMAGE_QUALITY,
                        outputStream
                    )
                    outputStream.toByteArray()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image for upload.", e)
            null
        }
    }

    private fun buildOptimizedCloudinaryUrl(response: CloudinaryUploadResponse): String? {
        val publicId = response.publicId ?: return response.secureUrl
        val formatSuffix = response.format?.takeIf { it.isNotBlank() }?.let { ".$it" }.orEmpty()
        return "https://res.cloudinary.com/${BuildConfig.CLOUDINARY_CLOUD_NAME}/image/upload/f_auto,q_auto/$publicId$formatSuffix"
    }

    private fun findMatchingPosts(
        draft: PendingPostDraft,
        foundPosts: List<Post>,
        generatedTerms: List<String>
    ): List<Post> {
        return foundPosts
            .map { post -> RankedPost(post, scoreFoundPostMatch(draft, post, generatedTerms)) }
            .filter { it.score >= 3 }
            .sortedByDescending { it.score }
            .take(MAX_CANDIDATE_POSTS)
            .map { it.post }
    }

    private fun scoreFoundPostMatch(
        draft: PendingPostDraft,
        post: Post,
        generatedTerms: List<String>
    ): Int {
        val postTitleTokens = tokenize(post.title)
        val postDescriptionTokens = tokenize(post.description)
        val postText = "${post.title} ${post.description}".lowercase(Locale.getDefault())

        val originalTitleTokens = tokenize(draft.title)
        val originalDescriptionTokens = tokenize(draft.description)
        val relatedTokens = generatedTerms
            .flatMap { tokenize(it).toList() }
            .toSet()
        val relatedPhrases = generatedTerms
            .map { it.trim().lowercase(Locale.getDefault()) }
            .filter { it.contains(" ") && it.length > 3 }
            .toSet()

        var score = 0

        score += originalTitleTokens.intersect(postTitleTokens).size * 4
        score += originalTitleTokens.intersect(postDescriptionTokens).size * 3
        score += originalDescriptionTokens.intersect(postTitleTokens).size * 3
        score += originalDescriptionTokens.intersect(postDescriptionTokens).size * 2
        score += relatedTokens.intersect(postTitleTokens).size * 2
        score += relatedTokens.intersect(postDescriptionTokens).size
        score += relatedPhrases.count { phrase -> postText.contains(phrase) } * 3

        if (post.category.equals(draft.category, ignoreCase = true)) {
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

    private fun buildGeminiTermsPrompt(draft: PendingPostDraft): String {
        return """
            Generate 10 to 15 short search terms, synonyms, alternate names, and related descriptive phrases
            for this lost item based only on its title and description.
            Use words and short phrases that could appear in a found-post title or description.
            Return only a JSON array of strings and nothing else.

            Item title:
            title: ${draft.title}

            Item description:
            description: ${draft.description}
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
        } catch (e: Exception) {
            cleaned
                .split(",", "\n")
                .map { it.trim().trim('"') }
                .filter { it.length > 2 }
                .distinct()
        }
    }

    private fun setPostingState(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        createPostButton.isEnabled = !isLoading
    }
}
