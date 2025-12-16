package com.example.dropzone.models

data class AIMatchRequest(
    val lost_text: String,
    val found_items: List<Map<String, String>>
)
