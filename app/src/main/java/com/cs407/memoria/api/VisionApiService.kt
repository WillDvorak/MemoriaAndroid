package com.cs407.memoria.api

import android.util.Base64
import android.util.Log
import com.cs407.memoria.model.BoundingBox
import com.cs407.memoria.model.ClothingCategory
import com.cs407.memoria.model.DetectedClothingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

class VisionApiService(private val apiKey: String) {

    companion object {
        private const val TAG = "VisionApiService"
        private const val VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate"
        private const val OVERLAP_THRESHOLD = 0.5f // 50% overlap is considered duplicate
    }


    //main function
    suspend fun detectClothing(imageBase64: String, userId: String): List<DetectedClothingItem> =
        withContext(Dispatchers.IO) {
            try {
                val requestBody = buildRequestBody(imageBase64)
                val response = makeApiRequest(requestBody)
                return@withContext parseDetectedItems(response, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error detecting clothing", e)
                emptyList()
            }
        }


    /*
    sends content (image) and requests
    label (what the item is)
    object with label, confidence and location of item
    properties ( color)
     */
    private fun buildRequestBody(imageBase64: String): String {
        val jsonRequest = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("image", JSONObject().apply {
                        put("content", imageBase64)
                    })
                    put("features", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "LABEL_DETECTION")
                            put("maxResults", 20)
                        })
                        put(JSONObject().apply {
                            put("type", "OBJECT_LOCALIZATION")
                            put("maxResults", 20)
                        })
                        put(JSONObject().apply {
                            put("type", "IMAGE_PROPERTIES")
                            put("maxResults", 10)
                        })
                    })
                })
            })
        }
        return jsonRequest.toString()
    }

    private fun makeApiRequest(requestBody: String): String {
        val url = URL("$VISION_API_URL?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        OutputStreamWriter(connection.outputStream).use { it.write(requestBody) }

        val responseCode = connection.responseCode
        Log.d(TAG, "Response code: $responseCode")

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } else {
            val error = BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
            Log.e(TAG, "API Error: $error")
            throw Exception("API request failed: $responseCode")
        }
    }



    private fun parseDetectedItems(response: String, userId: String): List<DetectedClothingItem> {
        val detectedItems = mutableListOf<DetectedClothingItem>()
        val jsonResponse = JSONObject(response)

        val responses = jsonResponse.optJSONArray("responses") ?: return emptyList()
        if (responses.length() == 0) return emptyList()

        val firstResponse = responses.getJSONObject(0)

        // Extract dominant colors
        val colors = extractColors(firstResponse)
        Log.d(TAG, "Detected ${colors.size} colors")

        // Extract labels
        val labels = extractLabels(firstResponse)
        Log.d(TAG, "Detected ${labels.size} labels")

        // Extract objects with bounding boxes
        val objects = firstResponse.optJSONArray("localizedObjectAnnotations")
        objects?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                val name = obj.getString("name").lowercase()
                val confidence = obj.getDouble("score").toFloat()

                // Extract bounding box
                val boundingBox = extractBoundingBox(obj)

                Log.d(TAG, "Object detected: $name (confidence: $confidence)")

                // Try to categorize this object as clothing
                val category = categorizeName(name)
                if (category != null) {
                    // Find related labels for this object
                    val relatedLabels = findRelatedLabels(name, labels)

                    val newItem = DetectedClothingItem(
                        category = category,
                        labels = relatedLabels,
                        colors = colors,
                        boundingBox = boundingBox,
                        confidence = confidence
                    )

                    // Check if this is a duplicate (overlapping bounding box with same category)
                    if (!isDuplicateDetection(newItem, detectedItems)) {
                        detectedItems.add(newItem)
                    } else {
                        Log.d(TAG, "Skipping duplicate detection: $name")
                    }
                }
            }
        }

        // If no objects were detected try categorizing from labels alone
        if (detectedItems.isEmpty() && labels.isNotEmpty()) {
            val categories = categorizeFromLabels(labels)
            categories.forEach { category ->
                detectedItems.add(
                    DetectedClothingItem(
                        category = category,
                        labels = labels.filter { label ->
                            isRelevantLabel(label, category)
                        },
                        colors = colors,
                        boundingBox = null,
                        confidence = 0.5f
                    )
                )
            }
        }

        Log.d(TAG, "Detected ${detectedItems.size} unique clothing items")
        return detectedItems
    }



    /* Checks for Duplicates in the same photo so API doesn't return same item twice */
    private fun isDuplicateDetection(
        newItem: DetectedClothingItem,
        existingItems: List<DetectedClothingItem>
    ): Boolean {
        // If no bounding box, can't determine overlap
        if (newItem.boundingBox == null) return false

        for (existing in existingItems) {
            // Only check items of the same category
            if (existing.category != newItem.category) continue

            // If existing item has no bounding box, skip
            if (existing.boundingBox == null) continue

            // Calculate overlap
            val overlap = calculateOverlap(newItem.boundingBox, existing.boundingBox)
            if (overlap > OVERLAP_THRESHOLD) {
                return true
            }
        }

        return false
    }

    /**
     * Calculate the overlap ratio between two bounding boxes on image
     */
    private fun calculateOverlap(box1: BoundingBox, box2: BoundingBox): Float {
        // Calculate intersection
        val intersectLeft = maxOf(box1.left, box2.left)
        val intersectTop = maxOf(box1.top, box2.top)
        val intersectRight = minOf(box1.right, box2.right)
        val intersectBottom = minOf(box1.bottom, box2.bottom)

        // Check if there's an intersection
        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) {
            return 0f
        }

        val intersectionArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)

        // Calculate areas of both boxes
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)

        // Use the smaller area to calculate overlap ratio
        val smallerArea = minOf(box1Area, box2Area)

        return if (smallerArea > 0) {
            intersectionArea / smallerArea
        } else {
            0f
        }
    }

    private fun extractColors(response: JSONObject): List<String> {
        val colors = mutableListOf<String>()

        val imageProperties = response.optJSONObject("imagePropertiesAnnotation")
        val dominantColors = imageProperties?.optJSONObject("dominantColors")
        val colorArray = dominantColors?.optJSONArray("colors")

        colorArray?.let {
            for (i in 0 until minOf(it.length(), 5)) { // Take top 5 colors
                val colorObj = it.getJSONObject(i)
                val color = colorObj.getJSONObject("color")
                val r = color.optInt("red", 0)
                val g = color.optInt("green", 0)
                val b = color.optInt("blue", 0)

                // Convert to hex
                val hexColor = String.format("#%02X%02X%02X", r, g, b)
                colors.add(hexColor)
            }
        }

        return colors
    }

    private fun extractLabels(response: JSONObject): List<String> {
        val labels = mutableListOf<String>()

        val labelAnnotations = response.optJSONArray("labelAnnotations")
        labelAnnotations?.let {
            for (i in 0 until it.length()) {
                val label = it.getJSONObject(i).getString("description").lowercase()
                labels.add(label)
            }
        }

        return labels
    }

    private fun extractBoundingBox(obj: JSONObject): BoundingBox? {
        return try {
            val boundingPoly = obj.getJSONObject("boundingPoly")
            val normalizedVertices = boundingPoly.getJSONArray("normalizedVertices")

            if (normalizedVertices.length() < 4) return null

            // Get all vertices to find min/max
            val xCoords = mutableListOf<Float>()
            val yCoords = mutableListOf<Float>()

            for (i in 0 until normalizedVertices.length()) {
                val vertex = normalizedVertices.getJSONObject(i)
                xCoords.add(vertex.optDouble("x", 0.0).toFloat())
                yCoords.add(vertex.optDouble("y", 0.0).toFloat())
            }

            BoundingBox(
                left = xCoords.minOrNull() ?: 0f,
                top = yCoords.minOrNull() ?: 0f,
                right = xCoords.maxOrNull() ?: 1f,
                bottom = yCoords.maxOrNull() ?: 1f
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting bounding box", e)
            null
        }
    }

    private fun findRelatedLabels(objectName: String, allLabels: List<String>): List<String> {
        val related = mutableListOf<String>()
        val keywords = objectName.split(" ")

        for (label in allLabels) {
            for (keyword in keywords) {
                if (label.contains(keyword, ignoreCase = true)) {
                    related.add(label)
                    break
                }
            }
        }

        return related.ifEmpty { allLabels.take(5) } // Fallback to first 5 labels
    }

    private fun categorizeName(name: String): ClothingCategory? {
        val shirtKeywords = setOf("shirt", "top", "t-shirt", "tshirt", "blouse", "sweater",
            "hoodie", "sweatshirt", "jersey", "polo")
        val pantsKeywords = setOf("pants", "trousers", "jeans", "shorts", "skirt",
            "leggings", "bottom")
        val shoesKeywords = setOf("shoe", "shoes", "footwear", "sneaker", "boot",
            "sandal", "heel")
        val dressKeywords = setOf("dress", "gown", "frock")
        val jacketKeywords = setOf("jacket", "coat", "blazer", "cardigan", "outerwear")
        val accessoryKeywords = setOf("hat", "cap", "bag", "purse", "belt", "scarf",
            "glasses", "sunglasses", "watch", "jewelry")

        return when {
            shirtKeywords.any { name.contains(it) } -> ClothingCategory.SHIRT
            pantsKeywords.any { name.contains(it) } -> ClothingCategory.PANTS
            shoesKeywords.any { name.contains(it) } -> ClothingCategory.SHOES
            dressKeywords.any { name.contains(it) } -> ClothingCategory.DRESS
            jacketKeywords.any { name.contains(it) } -> ClothingCategory.JACKET
            accessoryKeywords.any { name.contains(it) } -> ClothingCategory.ACCESSORY
            else -> null // Not a clothing item
        }
    }

    // Helper to map api responses to specific categories
    private fun categorizeFromLabels(labels: List<String>): Set<ClothingCategory> {
        val categories = mutableSetOf<ClothingCategory>()

        val shirtKeywords = setOf("shirt", "top", "t-shirt", "tshirt", "blouse", "sweater",
            "hoodie", "sweatshirt", "jersey", "polo")
        val pantsKeywords = setOf("pants", "trousers", "jeans", "shorts", "skirt",
            "leggings", "bottom")
        val shoesKeywords = setOf("shoe", "shoes", "footwear", "sneaker", "boot",
            "sandal", "heel")
        val dressKeywords = setOf("dress", "gown", "frock")
        val jacketKeywords = setOf("jacket", "coat", "blazer", "cardigan", "outerwear")
        val accessoryKeywords = setOf("hat", "cap", "bag", "purse", "belt", "scarf",
            "glasses", "sunglasses", "watch", "jewelry")

        for (label in labels) {
            when {
                shirtKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.SHIRT)
                pantsKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.PANTS)
                shoesKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.SHOES)
                dressKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.DRESS)
                jacketKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.JACKET)
                accessoryKeywords.any { label.contains(it) } -> categories.add(ClothingCategory.ACCESSORY)
            }
        }

        if (categories.isEmpty()) {
            categories.add(ClothingCategory.OTHER)
        }

        return categories
    }

    private fun isRelevantLabel(label: String, category: ClothingCategory): Boolean {
        val categoryKeywords = when (category) {
            ClothingCategory.SHIRT -> setOf("shirt", "top", "sleeve", "collar", "cotton", "fabric")
            ClothingCategory.PANTS -> setOf("pants", "trousers", "jeans", "denim", "leg", "waist")
            ClothingCategory.SHOES -> setOf("shoe", "footwear", "sole", "lace", "leather")
            ClothingCategory.DRESS -> setOf("dress", "gown", "formal", "elegant")
            ClothingCategory.JACKET -> setOf("jacket", "coat", "outer", "sleeve", "zipper")
            ClothingCategory.ACCESSORY -> setOf("accessory", "fashion", "style")
            ClothingCategory.OTHER -> setOf("clothing", "apparel", "wear")
        }

        return categoryKeywords.any { label.contains(it, ignoreCase = true) }
    }
}
