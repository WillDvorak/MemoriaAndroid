package com.cs407.memoria.model

/**
 * Represents a suggested outfit combination.
 * This is used temporarily while viewing suggestions before the user decides to save it.
 */
data class OutfitSuggestion(
    val id: String = "",
    val clothingItemIds: List<String> = emptyList(), // IDs of items in this suggestion
    val shirtId: String = "",
    val pantsId: String = "",
    val shoesId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
)
