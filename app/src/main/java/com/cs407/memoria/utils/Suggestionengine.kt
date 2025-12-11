package com.cs407.memoria.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Base64
import android.util.Log
import com.cs407.memoria.model.ClothingCategory
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.model.OutfitSuggestion
import java.io.ByteArrayOutputStream

/**
 * Engine for generating outfit suggestions based on highly-rated outfits.
 * Creates novel combinations that don't already exist in the wardrobe.
 */
object SuggestionEngine {
    private const val TAG = "SuggestionEngine"
    private const val MIN_RATING = 4 // Minimum rating (4-5 stars)

    /**
     * Result of attempting to generate a suggestion
     */
    sealed class SuggestionResult {
        data class Success(
            val suggestion: OutfitSuggestion,
            val items: List<ClothingItem>
        ) : SuggestionResult()

        data class NotEnoughItems(val message: String) : SuggestionResult()
        data class NoNewCombinations(val message: String) : SuggestionResult()
    }

    /**
     * Generate a new outfit suggestion from highly-rated outfits.
     *
     * @param outfits All outfits in the user's wardrobe
     * @param clothingItems All clothing items in the user's wardrobe
     * @param userId The current user's ID
     * @param excludeCombinations Set of item ID combinations to exclude (already suggested this session)
     * @return SuggestionResult indicating success or the reason for failure
     */
    fun generateSuggestion(
        outfits: List<Outfit>,
        clothingItems: List<ClothingItem>,
        userId: String,
        excludeCombinations: Set<Set<String>> = emptySet()
    ): SuggestionResult {
        Log.d(TAG, "Generating suggestion from ${outfits.size} outfits and ${clothingItems.size} items")

        // 1. Filter to highly-rated outfits (4-5 stars)
        val highlyRatedOutfits = outfits.filter { outfit ->
            outfit.rating != null && outfit.rating >= MIN_RATING
        }

        Log.d(TAG, "Found ${highlyRatedOutfits.size} highly-rated outfits")

        if (highlyRatedOutfits.isEmpty()) {
            return SuggestionResult.NotEnoughItems(
                "You need at least one outfit rated 4 or 5 stars to get suggestions. " +
                        "Add more outfits to your wardrobe and rate them!"
            )
        }

        // 2. Get all item IDs from highly-rated outfits
        val highlyRatedItemIds = highlyRatedOutfits.flatMap { it.clothingItemIds }.toSet()

        // 3. Get the actual items and categorize them
        val highlyRatedItems = clothingItems.filter { it.id in highlyRatedItemIds }

        val shirts = highlyRatedItems.filter { it.category == ClothingCategory.SHIRT }
        val pants = highlyRatedItems.filter { it.category == ClothingCategory.PANTS }
        val shoes = highlyRatedItems.filter { it.category == ClothingCategory.SHOES }

        Log.d(TAG, "Categorized items - Shirts: ${shirts.size}, Pants: ${pants.size}, Shoes: ${shoes.size}")

        // 4. Check if we have enough items for a complete outfit
        if (shirts.isEmpty() || pants.isEmpty() || shoes.isEmpty()) {
            val missing = mutableListOf<String>()
            if (shirts.isEmpty()) missing.add("shirts")
            if (pants.isEmpty()) missing.add("pants")
            if (shoes.isEmpty()) missing.add("shoes")

            return SuggestionResult.NotEnoughItems(
                "You need at least one ${missing.joinToString(", ")} from highly-rated outfits. " +
                        "Add more outfits with these items and rate them 4 or 5 stars!"
            )
        }

        // 5. Get existing combinations to avoid
        val existingCombinations = outfits.map { outfit ->
            outfit.clothingItemIds.toSet()
        }.toSet()

        // 6. Generate all possible new combinations
        val possibleCombinations = mutableListOf<Triple<ClothingItem, ClothingItem, ClothingItem>>()

        for (shirt in shirts) {
            for (pant in pants) {
                for (shoe in shoes) {
                    val combinationIds = setOf(shirt.id, pant.id, shoe.id)

                    // Check if this combination already exists in wardrobe
                    val existsInWardrobe = existingCombinations.any { existing ->
                        combinationIds.all { it in existing }
                    }

                    // Check if this was already suggested this session
                    val alreadySuggested = combinationIds in excludeCombinations

                    if (!existsInWardrobe && !alreadySuggested) {
                        possibleCombinations.add(Triple(shirt, pant, shoe))
                    }
                }
            }
        }

        Log.d(TAG, "Found ${possibleCombinations.size} possible new combinations")

        if (possibleCombinations.isEmpty()) {
            return SuggestionResult.NoNewCombinations(
                "You've seen all possible outfit combinations from your highly-rated items! " +
                        "Add more outfits to your wardrobe and rate them to get new suggestions."
            )
        }

        // 7. Randomly select one combination
        val selected = possibleCombinations.random()
        val (shirt, pant, shoe) = selected

        val suggestion = OutfitSuggestion(
            id = "suggestion_${System.currentTimeMillis()}",
            clothingItemIds = listOf(shirt.id, pant.id, shoe.id),
            shirtId = shirt.id,
            pantsId = pant.id,
            shoesId = shoe.id,
            userId = userId
        )

        Log.d(TAG, "Generated suggestion: ${suggestion.id}")

        return SuggestionResult.Success(
            suggestion = suggestion,
            items = listOf(shirt, pant, shoe)
        )
    }

    /**
     * Create a composite thumbnail from multiple clothing item images.
     * Arranges items vertically: shirt on top, pants in middle, shoes at bottom.
     *
     * @param items List of clothing items (should be shirt, pants, shoes)
     * @return Composite bitmap or null if creation fails
     */
    fun createCompositeThumbnail(items: List<ClothingItem>): Bitmap? {
        try {
            if (items.isEmpty()) return null

            // Decode all item images
            val bitmaps = items.mapNotNull { item ->
                try {
                    val imageBytes = Base64.decode(item.imageUrl, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding image for ${item.id}", e)
                    null
                }
            }

            if (bitmaps.isEmpty()) return null

            // Calculate composite dimensions
            val targetWidth = 400
            val itemHeight = 200
            val totalHeight = itemHeight * bitmaps.size
            val padding = 8

            // Create composite bitmap
            val composite = Bitmap.createBitmap(targetWidth, totalHeight + (padding * (bitmaps.size - 1)), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(composite)

            // Fill with white background
            canvas.drawColor(android.graphics.Color.WHITE)

            // Draw each item
            var yOffset = 0
            bitmaps.forEachIndexed { index, bitmap ->
                // Scale bitmap to fit width while maintaining aspect ratio
                val scaledBitmap = scaleBitmapToFit(bitmap, targetWidth - (padding * 2), itemHeight - (padding * 2))

                // Center horizontally
                val xOffset = (targetWidth - scaledBitmap.width) / 2

                canvas.drawBitmap(scaledBitmap, xOffset.toFloat(), yOffset.toFloat() + padding, Paint())

                yOffset += itemHeight + padding

                // Recycle scaled bitmap if it's different from original
                if (scaledBitmap != bitmap) {
                    scaledBitmap.recycle()
                }
            }

            // Recycle original bitmaps
            bitmaps.forEach { it.recycle() }

            return composite
        } catch (e: Exception) {
            Log.e(TAG, "Error creating composite thumbnail", e)
            return null
        }
    }

    /**
     * Scale a bitmap to fit within given dimensions while maintaining aspect ratio.
     */
    private fun scaleBitmapToFit(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val widthRatio = maxWidth.toFloat() / width
        val heightRatio = maxHeight.toFloat() / height
        val ratio = minOf(widthRatio, heightRatio)

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Convert a bitmap to base64 string for storage.
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}