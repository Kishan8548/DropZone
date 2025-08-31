package com.example.dropzone

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.dropzone.models.Post
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.os.Build
import com.google.android.material.color.MaterialColors

class AddPostActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddPostActivity"
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

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

    private val pickImageFromGallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            postImageView.setImageURI(it)
            postImageView.visibility = View.VISIBLE
            Log.d(TAG, "Image selected from gallery: $it")
        } ?: run {
            Log.w(TAG, "Image selection from gallery cancelled or failed.")
        }
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
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

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
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
            Toast.makeText(this, "Permissions required to pick/take image.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Permissions denied: ${deniedPermissions.joinToString()}")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

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
        val navIcon = toolbar.navigationIcon
        navIcon?.setTint(
            MaterialColors.getColor(toolbar, com.google.android.material.R.attr.colorOnPrimary)
        )
        supportActionBar?.title = "New Lost or Found Item"

        val categories = arrayOf("Select Category", "ID Card", "Electronics", "Books", "Keys", "Documents", "Accessories", "Others")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        postCategorySpinner.adapter = adapter

        statusLostRadioButton.isChecked = true

        selectImageButton.setOnClickListener {
            Log.d(TAG, "Select Image button clicked. Checking permissions.")
            checkPermissionsAndShowPicker()
        }

        createPostButton.setOnClickListener {
            Log.d(TAG, "Create Post button clicked. Attempting to create post.")
            createPost()
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
        val builder: android.app.AlertDialog.Builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Add Photo")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {
                    Log.d(TAG, "User selected 'Take Photo'. Launching camera.")
                    takePicture.launch(null)
                }
                options[item] == "Choose from Gallery" -> {
                    Log.d(TAG, "User selected 'Choose from Gallery'. Launching gallery.")
                    pickImageFromGallery.launch("image/*")
                }
                options[item] == "Cancel" -> {
                    Log.d(TAG, "User cancelled image selection dialog.")
                    dialog.dismiss()
                }
            }
        }
        builder.show()
    }

    private fun getImageUri(inImage: Bitmap): Uri? {
        try {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(contentResolver, inImage, "DropZone_Image_${System.currentTimeMillis()}", null)
            val uri = Uri.parse(path)
            Log.d(TAG, "Bitmap converted to URI: $uri")
            return uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get URI from Bitmap: ${e.message}", e)
            Toast.makeText(this, "Could not save image from camera: ${e.message}", Toast.LENGTH_LONG).show()
            return null
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
            Toast.makeText(this, "Please fill in all required fields and select a category.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Post creation failed: Missing required fields.")
            return
        }

        val userId = auth.currentUser?.uid
        val userName = auth.currentUser?.email

        if (userId == null || userName == null) {
            Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Post creation failed: User not authenticated.")
            return
        }

        progressBar.visibility = View.VISIBLE
        createPostButton.isEnabled = false

        if (selectedImageUri != null) {
            Log.d(TAG, "Image found. Uploading image and saving post.")
            uploadImageAndSavePost(userId, userName, title, description, category, location, status)
        } else {
            Log.d(TAG, "No image selected. Saving post without image.")
            savePostToFirestore(userId, userName, title, description, category, location, status, null)
        }
    }

    private fun uploadImageAndSavePost(
        userId: String,
        userName: String,
        title: String,
        description: String,
        category: String,
        location: String?,
        status: String
    ) {
        val fileName = "posts/${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(fileName)

        selectedImageUri?.let { uri ->
            imageRef.putFile(uri)
                .addOnSuccessListener {
                    Log.d(TAG, "Image uploaded successfully to: $fileName")
                    imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        val imageUrl = downloadUri.toString()
                        Log.d(TAG, "Image download URL obtained: $imageUrl")
                        savePostToFirestore(userId, userName, title, description, category, location, status, imageUrl)
                    }.addOnFailureListener { e -> // Use 'e' to capture the error
                        progressBar.visibility = View.GONE
                        createPostButton.isEnabled = true
                        Toast.makeText(this, "Failed to get image download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to get download URL", e) // Log the error object
                    }
                }
                .addOnFailureListener { e -> // Use 'e' to capture the error
                    progressBar.visibility = View.GONE
                    createPostButton.isEnabled = true
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Image upload failed", e) // Log the error object
                }
        }
    }

    private fun savePostToFirestore(
        userId: String,
        userName: String,
        title: String,
        description: String,
        category: String,
        location: String?,
        status: String,
        imageUrl: String?
    ) {
        val newPost = Post(
            userId = userId,
            userName = userName,
            title = title,
            description = description,
            category = category,
            location = location,
            status = status,
            imageUrl = imageUrl
        )

        firestore.collection("posts")
            .add(newPost)
            .addOnSuccessListener { documentReference ->
                progressBar.visibility = View.GONE
                createPostButton.isEnabled = true
                Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                createPostButton.isEnabled = true
                Toast.makeText(this, "Error creating post: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Error adding document to Firestore: ${e.message}", e)
            }
    }
}
