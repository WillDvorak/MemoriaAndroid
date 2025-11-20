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
    outfits: List<Outfit>,
    isLoading: Boolean,
    onOutfitClick: (Outfit) -> Unit,
    onBackClick: () -> Unit,
    onAddFromGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Top row: Add-from-library on the left, Back on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBackClick) {
                Text("Back")
            }
            Button(onClick = onAddFromGallery) {
                Text("Add from Library")
            }

        }

        Text(
            text = "My Wardrobe",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            outfits.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No outfits yet. Add one from the library or take a photo!")
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(outfits) { outfit ->
                        OutfitCard(
                            outfit = outfit,
                            onClick = { onOutfitClick(outfit) }
                        )
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

