package com.example.dropzone.models

import com.google.gson.annotations.SerializedName

data class CloudinaryUploadResponse(
    @SerializedName("public_id")
    val publicId: String? = null,
    @SerializedName("secure_url")
    val secureUrl: String? = null,
    val format: String? = null
)
