package com.cs407.memoria.model

data class Outfit(
    val id: String = "",
    val imageUrl: String = "",
    val detectedItems: List<ClothingItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val rating: Int? = null
)