package com.cs407.memoria.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.model.Outfit
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OutfitRepository {
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val TAG = "OutfitRepository"
    }

    suspend fun uploadOutfitImage(context: Context, imageUri: Uri): String {
        // Convert image to base64
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes() ?: byteArrayOf()
        inputStream?.close()

        val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
        Log.d(TAG, "Image converted to base64, size: ${base64Image.length} chars")

        return base64Image
    }

    suspend fun saveOutfit(outfit: Outfit): String {
        val outfitsRef = database.getReference("outfits")
        val newOutfitRef = outfitsRef.push()
        val outfitId = newOutfitRef.key ?: throw Exception("Failed to generate outfit ID")

        val outfitWithId = outfit.copy(id = outfitId)

        newOutfitRef.setValue(outfitWithId).await()
        Log.d(TAG, "Outfit saved: $outfitId")

        return outfitId
    }

    suspend fun getOutfitsForUser(userId: String): List<Outfit> = suspendCancellableCoroutine { continuation ->
        val outfitsRef = database.getReference("outfits")
        val query = outfitsRef.orderByChild("userId").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val outfits = mutableListOf<Outfit>()
                for (childSnapshot in snapshot.children) {
                    childSnapshot.getValue(Outfit::class.java)?.let { outfit ->
                        outfits.add(outfit)
                    }
                }
                // Sort by timestamp descending (newest first)
                outfits.sortByDescending { it.timestamp }
                Log.d(TAG, "Loaded ${outfits.size} outfits for user: $userId")
                continuation.resume(outfits)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading outfits", error.toException())
                continuation.resumeWithException(error.toException())
            }
        })
    }

    suspend fun getOutfitById(outfitId: String): Outfit? = suspendCancellableCoroutine { continuation ->
        val outfitRef = database.getReference("outfits/$outfitId")

        outfitRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val outfit = snapshot.getValue(Outfit::class.java)
                continuation.resume(outfit)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading outfit", error.toException())
                continuation.resumeWithException(error.toException())
            }
        })
    }
}