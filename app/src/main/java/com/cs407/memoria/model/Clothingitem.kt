package com.cs407.memoria.model

data class ClothingItem(
    val id: String = "",
    val imageUrl: String = "", // Cropped image of just this item (base64)
    val category: ClothingCategory = ClothingCategory.OTHER,
    val description: String = "", // Auto-generated description
    val dominantColors: List<String> = emptyList(), // Hex color codes
    val detectedLabels: List<String> = emptyList(), // Raw labels from Vision API
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String = "",
    val outfitReferences: List<String> = emptyList() // List of outfit IDs this item appears in
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

// Helper data class for Vision API detection results
data class DetectedClothingItem(
    val category: ClothingCategory,
    val labels: List<String>,
    val colors: List<String>,
    val boundingBox: BoundingBox?,
    val confidence: Float
)

data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)