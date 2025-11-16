package com.cs407.memoria.model

data class Outfit(
    val id: String = "",
    val imageUrl: String = "", // Full outfit photo (base64)
    val clothingItemIds: List<String> = emptyList(), // References to ClothingItem IDs
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val rating: Int? = null
)