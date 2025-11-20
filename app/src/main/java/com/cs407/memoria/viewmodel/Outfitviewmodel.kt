package com.cs407.memoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.memoria.api.VisionApiService
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.model.DetectedClothingItem
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.repository.ClothingItemRepository
import com.cs407.memoria.repository.OutfitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutfitViewModel(private val apiKey: String) : ViewModel() {
    private val outfitRepository = OutfitRepository()
    private val itemRepository = ClothingItemRepository()
    private val visionApi = VisionApiService(apiKey)

    private val _outfits = MutableStateFlow<List<Outfit>>(emptyList())
    val outfits: StateFlow<List<Outfit>> = _outfits

    private val _clothingItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    val clothingItems: StateFlow<List<ClothingItem>> = _clothingItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Duplicate detection state
    private val _pendingItems = MutableStateFlow<List<DetectedClothingItem>>(emptyList())
    val pendingItems: StateFlow<List<DetectedClothingItem>> = _pendingItems

    private val _similarItemsForConfirmation = MutableStateFlow<Pair<DetectedClothingItem, List<Pair<ClothingItem, Float>>>?>(null)
    val similarItemsForConfirmation: StateFlow<Pair<DetectedClothingItem, List<Pair<ClothingItem, Float>>>?> = _similarItemsForConfirmation

    private var currentOutfitImageBase64: String? = null
    private var currentUserId: String? = null
    private var processedItemIds = mutableListOf<String>()
    private var currentTopicName: String? = null
    private var currentNote: String? = null

    companion object {
        private const val TAG = "OutfitViewModel"
    }

    fun uploadOutfit(context: Context, imageUri: Uri, userId: String, topicName: String, note: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                processedItemIds.clear()

                Log.d(TAG, "Starting outfit upload for user: $userId")

                // 1. Convert image to base64
                val base64Image = outfitRepository.uploadOutfitImage(context, imageUri)
                Log.d(TAG, "Image converted to base64")

                // Store for later use
                currentOutfitImageBase64 = base64Image
                currentUserId = userId
                currentTopicName = topicName
                currentNote = note

                // 2. Detect clothing items using Vision API
                Log.d(TAG, "Detecting clothing items...")
                val detectedItems = visionApi.detectClothing(base64Image, userId)
                Log.d(TAG, "Detected ${detectedItems.size} items")

                if (detectedItems.isEmpty()) {
                    _error.value = "No clothing items detected in this image"
                    _isLoading.value = false
                    return@launch
                }

                // 3. Store detected items for processing
                _pendingItems.value = detectedItems

                // 4. Start processing items one by one
                processNextItem()

            } catch (e: Exception) {
                Log.e(TAG, "Error uploading outfit", e)
                _error.value = "Failed to upload outfit: ${e.message}"
                _isLoading.value = false
            }
        }
    }


    private suspend fun finalizeOutfit() {
        try {
            Log.d(TAG, "Finalizing outfit with ${processedItemIds.size} items")

            // Create outfit with references to clothing items
            val outfit = Outfit(
                imageUrl = currentOutfitImageBase64!!,
                clothingItemIds = processedItemIds.toList(),
                userId = currentUserId!!,
                        topicName = currentTopicName ?: "",
                note = currentNote ?: ""

            )

            val outfitId = outfitRepository.saveOutfit(outfit)
            Log.d(TAG, "Outfit saved: $outfitId")

            // Add outfit reference to each clothing item
            for (itemId in processedItemIds) {
                itemRepository.addOutfitReference(itemId, outfitId)
            }

            // Refresh lists
            loadOutfits(currentUserId!!)
            loadClothingItems(currentUserId!!)

            // Clear state
            currentOutfitImageBase64 = null
            currentUserId = null
            currentTopicName = null
            currentNote = null
            processedItemIds.clear()
            _pendingItems.value = emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing outfit", e)
            _error.value = "Error saving outfit: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    private fun processNextItem() {
        viewModelScope.launch {
            val pending = _pendingItems.value
            if (pending.isEmpty()) {
                // All items processed, save the outfit
                finalizeOutfit()
                return@launch
            }

            val nextItem = pending.first()
            Log.d(TAG, "Processing item: ${nextItem.category}")

            // Create a temporary ClothingItem for similarity checking
            val tempItem = createClothingItemFromDetection(nextItem)

            // Check for similar items
            val similarItems = itemRepository.findSimilarItems(tempItem, currentUserId!!)

            if (similarItems.isNotEmpty()) {
                Log.d(TAG, "Found ${similarItems.size} similar items")
                // Show confirmation dialog
                _similarItemsForConfirmation.value = Pair(nextItem, similarItems)
            } else {
                Log.d(TAG, "No similar items found, creating new")
                // No duplicates, create new item
                createNewItem(nextItem)
            }
        }
    }

    fun onUserConfirmsExistingItem(existingItem: ClothingItem) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "User confirmed existing item: ${existingItem.id}")
                processedItemIds.add(existingItem.id)

                // Remove processed item from pending
                _pendingItems.value = _pendingItems.value.drop(1)
                _similarItemsForConfirmation.value = null

                // Process next item
                processNextItem()
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming existing item", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun onUserCreatesNewItem() {
        viewModelScope.launch {
            try {
                val pending = _pendingItems.value
                if (pending.isNotEmpty()) {
                    val item = pending.first()
                    Log.d(TAG, "User chose to create new item")
                    createNewItem(item)
                }
                _similarItemsForConfirmation.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new item", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    fun onDismissDuplicateDialog() {
        // User cancelled, skip this item
        viewModelScope.launch {
            _pendingItems.value = _pendingItems.value.drop(1)
            _similarItemsForConfirmation.value = null
            processNextItem()
        }
    }

    private suspend fun createNewItem(detectedItem: DetectedClothingItem) {
        try {
            // Crop image for this item
            val croppedImage = itemRepository.cropItemImage(
                currentOutfitImageBase64!!,
                detectedItem.boundingBox
            )

            // Generate description
            val description = itemRepository.generateDescription(
                detectedItem.category,
                detectedItem.colors,
                detectedItem.labels
            )

            // Create new clothing item
            val newItem = ClothingItem(
                imageUrl = croppedImage,
                category = detectedItem.category,
                description = description,
                dominantColors = detectedItem.colors,
                detectedLabels = detectedItem.labels,
                userId = currentUserId!!,
                outfitReferences = emptyList()
            )

            val itemId = itemRepository.saveClothingItem(newItem)
            processedItemIds.add(itemId)
            Log.d(TAG, "Created new item: $itemId - $description")

            // Remove from pending and process next
            _pendingItems.value = _pendingItems.value.drop(1)
            processNextItem()

        } catch (e: Exception) {
            Log.e(TAG, "Error creating new item", e)
            _error.value = "Error creating item: ${e.message}"
            _isLoading.value = false
        }
    }



    private fun createClothingItemFromDetection(detected: DetectedClothingItem): ClothingItem {
        val description = itemRepository.generateDescription(
            detected.category,
            detected.colors,
            detected.labels
        )

        return ClothingItem(
            category = detected.category,
            description = description,
            dominantColors = detected.colors,
            detectedLabels = detected.labels,
            userId = currentUserId!!
        )
    }

    fun loadOutfits(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _outfits.value = outfitRepository.getOutfitsForUser(userId)
                Log.d(TAG, "Loaded ${_outfits.value.size} outfits")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading outfits", e)
                _error.value = "Failed to load outfits: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadClothingItems(userId: String) {
        viewModelScope.launch {
            try {
                _clothingItems.value = itemRepository.getClothingItemsForUser(userId)
                Log.d(TAG, "Loaded ${_clothingItems.value.size} clothing items")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading clothing items", e)
            }
        }
    }

    fun getClothingItemsForOutfit(outfit: Outfit): List<ClothingItem> {
        return _clothingItems.value.filter { item ->
            outfit.clothingItemIds.contains(item.id)
        }
    }

    fun deleteOutfit(outfit: Outfit, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Remove outfit reference from clothing items that use it
                val itemsToUpdate = _clothingItems.value.filter { item ->
                    outfit.id.isNotEmpty() && item.outfitReferences.contains(outfit.id)
                }

                for (item in itemsToUpdate) {
                    val updatedRefs = item.outfitReferences.filter { it != outfit.id }
                    val updatedItem = item.copy(outfitReferences = updatedRefs)
                    itemRepository.updateClothingItem(updatedItem)
                }

                // Delete the outfit itself
                outfitRepository.deleteOutfit(outfit.id)

                // Refresh lists for this user
                loadOutfits(userId)
                loadClothingItems(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting outfit", e)
                _error.value = "Error deleting outfit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

}
