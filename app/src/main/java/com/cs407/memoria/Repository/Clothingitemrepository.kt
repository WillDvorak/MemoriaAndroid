package com.cs407.memoria.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.cs407.memoria.model.BoundingBox
import com.cs407.memoria.model.ClothingCategory
import com.cs407.memoria.model.ClothingItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ClothingItemRepository {
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "ClothingItemRepository"
        private const val SIMILARITY_THRESHOLD = 0.7f // 70% similarity to be considered a match
    }

    /**
     * Crop an item from the outfit image using bounding box coordinates of ClothingItem
     * from vision api response
     */
    fun cropItemImage(outfitImageBase64: String, boundingBox: BoundingBox?): String {
        return try {
            if (boundingBox == null) {
                return outfitImageBase64 // Return full image if no bounding box
            }

            // Decode base64 to bitmap
            val imageBytes = Base64.decode(outfitImageBase64, Base64.DEFAULT)
            val originalBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return outfitImageBase64

            // Calculate crop coordinates
            val width = originalBitmap.width
            val height = originalBitmap.height
            val left = (boundingBox.left * width).toInt().coerceIn(0, width)
            val top = (boundingBox.top * height).toInt().coerceIn(0, height)
            val right = (boundingBox.right * width).toInt().coerceIn(0, width)
            val bottom = (boundingBox.bottom * height).toInt().coerceIn(0, height)
            val cropWidth = (right - left).coerceAtLeast(1)
            val cropHeight = (bottom - top).coerceAtLeast(1)

            // Crop the bitmap
            val croppedBitmap = Bitmap.createBitmap(
                originalBitmap,
                left,
                top,
                cropWidth,
                cropHeight
            )

            // Convert back to base64
            val outputStream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            val croppedBytes = outputStream.toByteArray()

            // Clean up
            originalBitmap.recycle()
            croppedBitmap.recycle()

            Base64.encodeToString(croppedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping image", e)
            outfitImageBase64 // Return original on error
        }
    }

    /**
     * Generate a description for a clothing item based on its attributes
     */
    fun generateDescription(
        category: ClothingCategory,
        colors: List<String>,
        labels: List<String>
    ): String {
        val colorDescriptor = if (colors.isNotEmpty()) {
            getColorName(colors[0]) // Use dominant color
        } else {
            ""
        }

        // Find most descriptive label, avoiding generic terms
        val descriptiveLabel = labels.firstOrNull { label ->
            !label.contains(category.name, ignoreCase = true) &&
                    !label.contains("sleeve", ignoreCase = true) && // Filter out "sleeve"
                    !label.contains("clothing", ignoreCase = true) &&
                    !label.contains("apparel", ignoreCase = true) &&
                    !label.contains("wear", ignoreCase = true) &&
                    !label.contains("fashion", ignoreCase = true) &&
                    !label.contains("style", ignoreCase = true) &&
                    label.length > 3
        } ?: ""

        val categoryName = category.name.lowercase().replace("_", " ")

        return when {
            colorDescriptor.isNotEmpty() && descriptiveLabel.isNotEmpty() ->
                "$colorDescriptor $descriptiveLabel $categoryName"
            colorDescriptor.isNotEmpty() ->
                "$colorDescriptor $categoryName"
            descriptiveLabel.isNotEmpty() ->
                "$descriptiveLabel $categoryName"
            else ->
                categoryName
        }.trim().replaceFirstChar { it.uppercase() }
    }

    /**
     * Convert hex color to readable name
     */
    private fun getColorName(hexColor: String): String {
        // Remove # if present
        val hex = hexColor.removePrefix("#")

        // Convert to RGB
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)

        // Simple color name matching
        return when {
            r > 200 && g > 200 && b > 200 -> "White"
            r < 50 && g < 50 && b < 50 -> "Black"
            r > 150 && g < 100 && b < 100 -> "Red"
            r < 100 && g > 150 && b < 100 -> "Green"
            r < 100 && g < 100 && b > 150 -> "Blue"
            r > 150 && g > 150 && b < 100 -> "Yellow"
            r > 150 && g < 100 && b > 150 -> "Purple"
            r > 200 && g > 100 && b < 100 -> "Orange"
            r > 100 && g > 100 && b > 100 -> "Gray"
            r > 100 && g > 50 && b < 50 -> "Brown"
            else -> ""
        }
    }

    /**
     * Find potential duplicate in wardrobe items for a new item
     */
    suspend fun findSimilarItems(
        newItem: ClothingItem,
        userId: String
    ): List<Pair<ClothingItem, Float>> = suspendCancellableCoroutine { continuation ->
        val itemsRef = database.getReference("clothingItems")
        val query = itemsRef.orderByChild("userId").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val similarItems = mutableListOf<Pair<ClothingItem, Float>>()

                for (childSnapshot in snapshot.children) {
                    childSnapshot.getValue(ClothingItem::class.java)?.let { existingItem ->
                        val similarity = calculateSimilarity(newItem, existingItem)
                        if (similarity >= SIMILARITY_THRESHOLD) {
                            similarItems.add(Pair(existingItem, similarity))
                        }
                    }
                }

                // Sort by similarity (highest first)
                similarItems.sortByDescending { it.second }
                Log.d(TAG, "Found ${similarItems.size} similar items")
                continuation.resume(similarItems)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error finding similar items", error.toException())
                continuation.resumeWithException(error.toException())
            }
        })
    }

    /**
     * Calculate similarity score between two clothing items (0.0 to 1.0)
     * based on color, label, and description
     */
    private fun calculateSimilarity(item1: ClothingItem, item2: ClothingItem): Float {
        // Must be same category
        if (item1.category != item2.category) {
            return 0f
        }

        var score = 0f
        var totalWeight = 0f

        // Compare colors (weight: 0.4)
        if (item1.dominantColors.isNotEmpty() && item2.dominantColors.isNotEmpty()) {
            val colorMatch = item1.dominantColors.intersect(item2.dominantColors.toSet()).size
            val totalColors = item1.dominantColors.size.coerceAtLeast(item2.dominantColors.size)
            val colorScore = colorMatch.toFloat() / totalColors

            score += colorScore * 0.4f
            totalWeight += 0.4f
        }

        // Compare labels (weight: 0.4)
        if (item1.detectedLabels.isNotEmpty() && item2.detectedLabels.isNotEmpty()) {
            val labelMatch = item1.detectedLabels.intersect(item2.detectedLabels.toSet()).size
            val totalLabels = item1.detectedLabels.size.coerceAtLeast(item2.detectedLabels.size)
            val labelScore = labelMatch.toFloat() / totalLabels

            score += labelScore * 0.4f
            totalWeight += 0.4f
        }

        // Compare descriptions (weight: 0.2)
        val desc1Words = item1.description.lowercase().split(" ").toSet()
        val desc2Words = item2.description.lowercase().split(" ").toSet()
        if (desc1Words.isNotEmpty() && desc2Words.isNotEmpty()) {
            val descMatch = desc1Words.intersect(desc2Words).size
            val totalWords = desc1Words.size.coerceAtLeast(desc2Words.size)
            val descScore = descMatch.toFloat() / totalWords

            score += descScore * 0.2f
            totalWeight += 0.2f
        }

        return if (totalWeight > 0f) score / totalWeight else 0f
    }


    /**
     * Save a new clothing item to the database
     */
    suspend fun saveClothingItem(item: ClothingItem): String {
        val itemsRef = database.getReference("clothingItems")
        val newItemRef = itemsRef.push()
        val itemId = newItemRef.key ?: throw Exception("Failed to generate item ID")

        val itemWithId = item.copy(id = itemId)
        newItemRef.setValue(itemWithId).await()
        Log.d(TAG, "Clothing item saved: $itemId")

        return itemId
    }

    /**
     * Update an existing clothing item (e.g., add outfit reference or rename)
     */
    suspend fun updateClothingItem(item: ClothingItem) {
        val itemRef = database.getReference("clothingItems/${item.id}")
        itemRef.setValue(item).await()
        Log.d(TAG, "Clothing item updated: ${item.id}")
    }

    /**
     * Update just the description of a clothing item
     */
    suspend fun updateItemDescription(itemId: String, newDescription: String) {
        val itemRef = database.getReference("clothingItems/$itemId/description")
        itemRef.setValue(newDescription).await()
        Log.d(TAG, "Item description updated: $itemId -> $newDescription")
    }

    /**
     * Add an outfit reference to an existing item
     */
    suspend fun addOutfitReference(itemId: String, outfitId: String) {
        val itemRef = database.getReference("clothingItems/$itemId")

        itemRef.get().await().getValue(ClothingItem::class.java)?.let { item ->
            val updatedReferences = item.outfitReferences.toMutableList()
            if (!updatedReferences.contains(outfitId)) {
                updatedReferences.add(outfitId)
                updateClothingItem(item.copy(outfitReferences = updatedReferences))
            }
        }
    }

    /**
     * Get all clothing items for a user
     */
    suspend fun getClothingItemsForUser(userId: String): List<ClothingItem> =
        suspendCancellableCoroutine { continuation ->
            val itemsRef = database.getReference("clothingItems")
            val query = itemsRef.orderByChild("userId").equalTo(userId)

            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val items = mutableListOf<ClothingItem>()
                    for (childSnapshot in snapshot.children) {
                        childSnapshot.getValue(ClothingItem::class.java)?.let { item ->
                            items.add(item)
                        }
                    }
                    items.sortByDescending { it.timestamp }
                    Log.d(TAG, "Loaded ${items.size} clothing items for user: $userId")
                    continuation.resume(items)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading clothing items", error.toException())
                    continuation.resumeWithException(error.toException())
                }
            })
        }

    /**
     * Get a specific clothing item by ID
     */
    suspend fun getClothingItemById(itemId: String): ClothingItem? =
        suspendCancellableCoroutine { continuation ->
            val itemRef = database.getReference("clothingItems/$itemId")

            itemRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val item = snapshot.getValue(ClothingItem::class.java)
                    continuation.resume(item)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Error loading clothing item", error.toException())
                    continuation.resumeWithException(error.toException())
                }
            })
        }
}
