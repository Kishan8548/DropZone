package com.example.dropzone.models

import com.google.firebase.firestore.Exclude // Add this import
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Post(
    @get:Exclude @set:Exclude var id: String = "",

    val userId: String = "",
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val location: String? = null,
    val status: String = "Lost",
    val imageUrl: String? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)