package com.cs407.memoria.api

import android.util.Base64
import android.util.Log
import com.cs407.memoria.model.ClothingCategory
import com.cs407.memoria.model.ClothingItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class VisionApiService(private val apiKey: String) {

    companion object {
        private const val TAG = "VisionApiService"
        private const val VISION_API_URL = "https://vision.googleapis.com/v1/images:annotate"
    }

    suspend fun detectClothing(imageBase64: String, userId: String): List<ClothingItem> = withContext(Dispatchers.IO) {
        try {
            val requestBody = buildRequestBody(imageBase64)
            val response = makeApiRequest(requestBody)
            return@withContext parseClothingItems(response, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting clothing", e)
            emptyList()
        }
    }

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

    private fun parseClothingItems(response: String, userId: String): List<ClothingItem> {
        val detectedItems = mutableListOf<ClothingItem>()
        val jsonResponse = JSONObject(response)

        val responses = jsonResponse.optJSONArray("responses") ?: return emptyList()
        if (responses.length() == 0) return emptyList()

        val firstResponse = responses.getJSONObject(0)

        // Parse label annotations
        val labels = firstResponse.optJSONArray("labelAnnotations")
        val labelSet = mutableSetOf<String>()
        labels?.let {
            for (i in 0 until it.length()) {
                val label = it.getJSONObject(i).getString("description").lowercase()
                labelSet.add(label)
                Log.d(TAG, "Label detected: $label")
            }
        }

        // Parse object localizations
        val objects = firstResponse.optJSONArray("localizedObjectAnnotations")
        objects?.let {
            for (i in 0 until it.length()) {
                val obj = it.getJSONObject(i)
                val name = obj.getString("name").lowercase()
                Log.d(TAG, "Object detected: $name")
                labelSet.add(name)
            }
        }

        // Categorize detected items
        val categories = mapLabelsToCategories(labelSet)
        categories.forEach { category ->
            detectedItems.add(
                ClothingItem(
                    id = "",
                    imageUrl = "",
                    category = category,
                    userId = userId
                )
            )
        }

        Log.d(TAG, "Detected ${detectedItems.size} clothing items")
        return detectedItems
    }

    private fun mapLabelsToCategories(labels: Set<String>): Set<ClothingCategory> {
        val categories = mutableSetOf<ClothingCategory>()

        // Define keywords for each category
        val shirtKeywords = setOf("shirt", "top", "t-shirt", "tshirt", "blouse", "sweater", "hoodie", "sweatshirt", "jersey", "polo")
        val pantsKeywords = setOf("pants", "trousers", "jeans", "shorts", "skirt", "leggings", "bottom")
        val shoesKeywords = setOf("shoe", "shoes", "footwear", "sneaker", "boot", "sandal", "heel")
        val dressKeywords = setOf("dress", "gown", "frock")
        val jacketKeywords = setOf("jacket", "coat", "blazer", "cardigan", "outerwear")
        val accessoryKeywords = setOf("hat", "cap", "bag", "purse", "belt", "scarf", "glasses", "sunglasses", "watch", "jewelry")

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

        // If no specific categories found, add OTHER
        if (categories.isEmpty()) {
            categories.add(ClothingCategory.OTHER)
        }

        return categories
    }
}
