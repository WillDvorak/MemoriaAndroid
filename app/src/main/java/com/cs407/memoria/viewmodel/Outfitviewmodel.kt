package com.cs407.memoria.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.memoria.api.VisionApiService
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.repository.OutfitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OutfitViewModel(private val apiKey: String) : ViewModel() {
    private val repository = OutfitRepository()
    private val visionApi = VisionApiService(apiKey)

    private val _outfits = MutableStateFlow<List<Outfit>>(emptyList())
    val outfits: StateFlow<List<Outfit>> = _outfits

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    companion object {
        private const val TAG = "OutfitViewModel"
    }

    fun uploadOutfit(context: Context, imageUri: Uri, userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Starting outfit upload for user: $userId")

                // 1. Convert image to base64 and store in Realtime Database
                val base64Image = repository.uploadOutfitImage(context, imageUri)
                Log.d(TAG, "Image converted to base64")

                // 2. Detect clothing items using the same base64 image
                Log.d(TAG, "Detecting clothing items...")
                val detectedItems = visionApi.detectClothing(base64Image, userId)
                Log.d(TAG, "Detected ${detectedItems.size} items")

                // 3. Save outfit to Realtime Database
                val outfit = Outfit(
                    imageUrl = base64Image,  // Store base64 directly
                    detectedItems = detectedItems,
                    userId = userId
                )
                repository.saveOutfit(outfit)

                Log.d(TAG, "Outfit saved successfully")

                // 4. Refresh outfit list
                loadOutfits(userId)

            } catch (e: Exception) {
                Log.e(TAG, "Error uploading outfit", e)
                _error.value = "Failed to upload outfit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOutfits(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                _outfits.value = repository.getOutfitsForUser(userId)
                Log.d(TAG, "Loaded ${_outfits.value.size} outfits")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading outfits", e)
                _error.value = "Failed to load outfits: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
