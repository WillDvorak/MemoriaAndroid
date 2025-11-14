package com.cs407.memoria.model

data class Outfitsuggestion(
    val id: String = "",
    val clothingItemIds: List<String> = emptyList(),
    val virtualTryOnImageUrl: String = "",
    val userRating: Int? = null, // 1-5 stars
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
)
