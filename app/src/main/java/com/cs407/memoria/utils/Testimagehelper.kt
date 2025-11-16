package com.cs407.memoria.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object TestImageHelper {
    private const val TAG = "TestImageHelper"

    /**
     * Convert a drawable resource to a Uri that can be used for testing
     * @param context The application context
     * @param drawableResId The resource ID of the drawable (e.g., R.drawable.test_outfit)
     * @return Uri of the temporary file, or null if conversion fails
     */
    fun getUriFromDrawable(context: Context, drawableResId: Int): Uri? {
        return try {
            // Load the drawable as a bitmap
            val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)

            // Create a temporary file
            val tempFile = File(context.filesDir, "test_outfit_${System.currentTimeMillis()}.jpg")

            // Write bitmap to file
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Get Uri from FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            Log.d(TAG, "Created test image URI: $uri")
            uri
        } catch (e: Exception) {
            Log.e(TAG, "Error creating URI from drawable", e)
            null
        }
    }

    /**
     * Convert a drawable resource directly to base64 string for API testing
     * @param context The application context
     * @param drawableResId The resource ID of the drawable
     * @return Base64 encoded string of the image
     */
    fun getBase64FromDrawable(context: Context, drawableResId: Int): String {
        val bitmap = BitmapFactory.decodeResource(context.resources, drawableResId)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }
}
