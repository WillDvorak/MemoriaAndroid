package com.cs407.memoria.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.memoria.api.VisionApiService
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.model.DetectedClothingItem
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.model.OutfitSuggestion
import com.cs407.memoria.repository.ClothingItemRepository
import com.cs407.memoria.repository.OutfitRepository
import com.cs407.memoria.utils.SuggestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Rating state - outfit waiting for rating after upload
    private val _outfitPendingRating = MutableStateFlow<Outfit?>(null)
    val outfitPendingRating: StateFlow<Outfit?> = _outfitPendingRating

    // Suggestion state
    private val _currentSuggestion = MutableStateFlow<OutfitSuggestion?>(null)
    val currentSuggestion: StateFlow<OutfitSuggestion?> = _currentSuggestion

    private val _suggestedItems = MutableStateFlow<List<ClothingItem>>(emptyList())
    val suggestedItems: StateFlow<List<ClothingItem>> = _suggestedItems

    private val _suggestionThumbnail = MutableStateFlow<Bitmap?>(null)
    val suggestionThumbnail: StateFlow<Bitmap?> = _suggestionThumbnail

    private val _suggestionError = MutableStateFlow<String?>(null)
    val suggestionError: StateFlow<String?> = _suggestionError

    private val _isSuggestionLoading = MutableStateFlow(false)
    val isSuggestionLoading: StateFlow<Boolean> = _isSuggestionLoading

    // Track combinations suggested this session to avoid repeats
    private val suggestedCombinationsThisSession = mutableSetOf<Set<String>>()

    private var currentOutfitImageBase64: String? = null
    private var currentUserId: String? = null
    private var currentTopicName: String? = null
    private var currentNote: String? = null
    private var processedItemIds = mutableListOf<String>()
    private var customItemName: String? = null

    companion object {
        private const val TAG = "OutfitViewModel"
    }

    // ==================== SUGGESTION METHODS ====================

    /**
     * Generate a new outfit suggestion based on highly-rated outfits.
     */
    fun generateSuggestion(userId: String) {
        viewModelScope.launch {
            try {
                _isSuggestionLoading.value = true
                _suggestionError.value = null
                _currentSuggestion.value = null
                _suggestedItems.value = emptyList()
                _suggestionThumbnail.value = null

                Log.d(TAG, "Generating suggestion for user: $userId")

                // Make sure we have the latest data
                if (_outfits.value.isEmpty()) {
                    _outfits.value = outfitRepository.getOutfitsForUser(userId)
                }
                if (_clothingItems.value.isEmpty()) {
                    _clothingItems.value = itemRepository.getClothingItemsForUser(userId)
                }

                val result = withContext(Dispatchers.Default) {
                    SuggestionEngine.generateSuggestion(
                        outfits = _outfits.value,
                        clothingItems = _clothingItems.value,
                        userId = userId,
                        excludeCombinations = suggestedCombinationsThisSession
                    )
                }

                when (result) {
                    is SuggestionEngine.SuggestionResult.Success -> {
                        _currentSuggestion.value = result.suggestion
                        _suggestedItems.value = result.items

                        // Add to session exclusions
                        suggestedCombinationsThisSession.add(result.suggestion.clothingItemIds.toSet())

                        // Generate composite thumbnail
                        val thumbnail = withContext(Dispatchers.Default) {
                            SuggestionEngine.createCompositeThumbnail(result.items)
                        }
                        _suggestionThumbnail.value = thumbnail

                        Log.d(TAG, "Suggestion generated successfully")
                    }

                    is SuggestionEngine.SuggestionResult.NotEnoughItems -> {
                        _suggestionError.value = result.message
                        Log.d(TAG, "Not enough items: ${result.message}")
                    }

                    is SuggestionEngine.SuggestionResult.NoNewCombinations -> {
                        _suggestionError.value = result.message
                        Log.d(TAG, "No new combinations: ${result.message}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error generating suggestion", e)
                _suggestionError.value = "Error generating suggestion: ${e.message}"
            } finally {
                _isSuggestionLoading.value = false
            }
        }
    }

    /**
     * Save the current suggestion as an outfit in the wardrobe.
     */
    fun saveSuggestion(userId: String, rating: Int) {
        viewModelScope.launch {
            try {
                _isSuggestionLoading.value = true

                val suggestion = _currentSuggestion.value ?: return@launch
                val thumbnail = _suggestionThumbnail.value ?: return@launch

                Log.d(TAG, "Saving suggestion with rating: $rating")

                // Convert thumbnail to base64
                val thumbnailBase64 = withContext(Dispatchers.Default) {
                    SuggestionEngine.bitmapToBase64(thumbnail)
                }

                // Create and save the outfit
                val outfit = Outfit(
                    imageUrl = thumbnailBase64,
                    clothingItemIds = suggestion.clothingItemIds,
                    userId = userId,
                    topicName = "Suggested Outfit",
                    note = "Created from suggestion",
                    rating = rating
                )

                val outfitId = outfitRepository.saveOutfit(outfit)
                Log.d(TAG, "Suggestion saved as outfit: $outfitId")

                // Add outfit reference to each clothing item
                for (itemId in suggestion.clothingItemIds) {
                    itemRepository.addOutfitReference(itemId, outfitId)
                }

                // Refresh outfits list
                loadOutfits(userId)

                // Clear suggestion state
                clearSuggestion()

                Log.d(TAG, "Suggestion saved successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error saving suggestion", e)
                _suggestionError.value = "Error saving suggestion: ${e.message}"
            } finally {
                _isSuggestionLoading.value = false
            }
        }
    }

    /**
     * Dismiss the current suggestion without saving.
     */
    fun dismissSuggestion() {
        // Keep the combination in exclusions so it won't be suggested again this session
        clearSuggestion()
    }

    /**
     * Clear all suggestion state.
     */
    fun clearSuggestion() {
        _currentSuggestion.value = null
        _suggestedItems.value = emptyList()
        _suggestionThumbnail.value?.recycle()
        _suggestionThumbnail.value = null
        _suggestionError.value = null
    }

    /**
     * Reset session exclusions (called when navigating away from suggestions).
     */
    fun resetSuggestionSession() {
        suggestedCombinationsThisSession.clear()
        clearSuggestion()
    }

    // ==================== ORIGINAL OUTFIT METHODS ====================

    // Original uploadOutfit function (for backward compatibility)
    fun uploadOutfit(context: Context, imageUri: Uri, userId: String) {
        uploadOutfit(context, imageUri, userId, "", "")
    }

    // New uploadOutfit function with topicName and note parameters
    fun uploadOutfit(
        context: Context,
        imageUri: Uri,
        userId: String,
        topicName: String,
        note: String
    ) {
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

            // Create outfit with references to clothing items (no rating yet)
            val outfit = Outfit(
                imageUrl = currentOutfitImageBase64!!,
                clothingItemIds = processedItemIds.toList(),
                userId = currentUserId!!,
                topicName = currentTopicName ?: "",
                note = currentNote ?: "",
                rating = null // Will be set after user rates
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

            // Set the outfit for pending rating (will show rating dialog)
            val savedOutfit = outfit.copy(id = outfitId)
            _outfitPendingRating.value = savedOutfit

            // Clear processing state (but keep outfit pending for rating)
            currentOutfitImageBase64 = null
            currentTopicName = null
            currentNote = null
            processedItemIds.clear()
            _pendingItems.value = emptyList()
            customItemName = null

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
                // Show confirmation dialog with existing items (which have their custom names)
                _similarItemsForConfirmation.value = Pair(nextItem, similarItems)
            } else {
                Log.d(TAG, "No similar items found, creating new")
                // No duplicates, create new item
                createNewItem(nextItem, null)
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

    // Overloaded function with customName parameter
    fun onUserCreatesNewItem(customName: String) {
        viewModelScope.launch {
            try {
                val pending = _pendingItems.value
                if (pending.isNotEmpty()) {
                    val item = pending.first()

                    Log.d(TAG, "User chose to create new item with name: $customName")
                    createNewItem(item, customName)
                }
                _similarItemsForConfirmation.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error creating new item", e)
                _error.value = "Error: ${e.message}"
            }
        }
    }

    // Overloaded function without customName parameter (uses auto-generated name)
    fun onUserCreatesNewItem() {
        viewModelScope.launch {
            try {
                val pending = _pendingItems.value
                if (pending.isNotEmpty()) {
                    val item = pending.first()

                    Log.d(TAG, "User chose to create new item with auto-generated name")
                    createNewItem(item, null)
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

    private suspend fun createNewItem(detectedItem: DetectedClothingItem, customName: String?) {
        try {
            // Crop image for this item
            val croppedImage = itemRepository.cropItemImage(
                currentOutfitImageBase64!!,
                detectedItem.boundingBox
            )

            // Use custom name if provided, otherwise generate description
            val description = if (!customName.isNullOrBlank()) {
                customName
            } else {
                itemRepository.generateDescription(
                    detectedItem.category,
                    detectedItem.colors,
                    detectedItem.labels
                )
            }

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

    fun renameClothingItem(itemId: String, newName: String) {
        viewModelScope.launch {
            try {
                // Update in Firebase
                itemRepository.updateItemDescription(itemId, newName)

                // Update local state so all outfits see the new name
                _clothingItems.value = _clothingItems.value.map { item ->
                    if (item.id == itemId) item.copy(description = newName) else item
                }

                Log.d(TAG, "Item renamed: $itemId -> $newName")
            } catch (e: Exception) {
                Log.e(TAG, "Error renaming item", e)
                _error.value = "Failed to rename item: ${e.message}"
            }
        }
    }

    // Rate an outfit (used for both post-upload and editing)
    fun rateOutfit(outfitId: String, rating: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Rating outfit: $outfitId with $rating stars")

                // Update in Firebase
                outfitRepository.updateOutfitRating(outfitId, rating)

                // Update local state
                _outfits.value = _outfits.value.map { outfit ->
                    if (outfit.id == outfitId) outfit.copy(rating = rating) else outfit
                }

                // Clear pending rating if this was the pending outfit
                if (_outfitPendingRating.value?.id == outfitId) {
                    _outfitPendingRating.value = null
                }

                Log.d(TAG, "Outfit rated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error rating outfit", e)
                _error.value = "Failed to rate outfit: ${e.message}"
            }
        }
    }

    // Skip rating for pending outfit
    fun skipRating() {
        _outfitPendingRating.value = null
        // Keep userId for potential future use
    }

    // Dismiss rating dialog without saving
    fun dismissRatingDialog() {
        _outfitPendingRating.value = null
    }

    // Delete outfit function
    fun deleteOutfit(outfit: Outfit, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Delete from repository
                outfitRepository.deleteOutfit(outfit.id)
                Log.d(TAG, "Outfit deleted: ${outfit.id}")

                // Refresh the outfits list
                loadOutfits(userId)

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting outfit", e)
                _error.value = "Failed to delete outfit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
