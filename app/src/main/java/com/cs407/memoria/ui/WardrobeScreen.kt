package com.cs407.memoria.ui

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext

@Composable
fun WardrobeScreen(
    outfitImages: List<Uri>  // This will be your list from HomeScreen
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "My Wardrobe",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (outfitImages.isEmpty()) {
            Text("No outfits yet. Go take a photo on the Home screen!")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(outfitImages) { imageUri ->
                    OutfitCard(imageUri = imageUri)
                }
            }
        }
    }
}

@Composable
fun OutfitCard(imageUri: Uri) {
    val context = LocalContext.current

    // Decode the bitmap from the Uri (simple version)
    val bitmap = remember(imageUri) {
        context.contentResolver.openInputStream(imageUri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    }

    if (bitmap != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
                .border(BorderStroke(4.dp, Color.Black), )
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Outfit photo",
                modifier = Modifier.fillMaxSize().padding(10.dp),
                contentScale = ContentScale.Crop

            )
        }
    } else {
        // Optional: fallback UI if bitmap can't be decoded
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Could not load image")
            }
        }
    }
}

