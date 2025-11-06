package com.cs407.memoria.model

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "",
    val category: ClothingCategory = ClothingCategory.OTHER,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = ""
)

enum class ClothingCategory {
    SHIRT,
    PANTS,
    SHOES,
    DRESS,
    JACKET,
    ACCESSORY,
    OTHER
}