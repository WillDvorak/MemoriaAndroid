package com.cs407.memoria.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.model.Outfit

@Composable
fun OutfitDetailScreen(
    outfit: Outfit,
    clothingItems: List<ClothingItem>,
    onBackClick: () -> Unit,
    onRenameItem: ((String, String) -> Unit)? = null
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<ClothingItem?>(null) }

    if (showRenameDialog && itemToRename != null) {
        RenameItemDialog(
            currentName = itemToRename!!.description,
            onConfirm = { newName ->
                onRenameItem?.invoke(itemToRename!!.id, newName)
                showRenameDialog = false
                itemToRename = null
            },
            onDismiss = {
                showRenameDialog = false
                itemToRename = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Outfit Details",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onBackClick,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text("Back to Wardrobe")
        }

        // Outfit Image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {
            // Decode base64 to bitmap
            val imageBytes = Base64.decode(outfit.imageUrl, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Outfit photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Image unavailable")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Clothing Items in this Outfit
        Text(
            text = "Items in this Outfit:",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (clothingItems.isEmpty()) {
            Text("No items detected")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(clothingItems) { item ->
                    ClothingItemCard(
                        item = item,
                        onRenameClick = if (onRenameItem != null) {
                            {
                                itemToRename = item
                                showRenameDialog = true
                            }
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameItemDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Item") },
        text = {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = { Text("Item Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(textFieldValue.trim()) },
                enabled = textFieldValue.trim().isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ClothingItemCard(
    item: ClothingItem,
    onRenameClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item thumbnail
            if (item.imageUrl.isNotEmpty()) {
                Card(
                    modifier = Modifier.size(80.dp)
                ) {
                    val bitmap = remember(item.imageUrl) {
                        try {
                            val imageBytes = Base64.decode(item.imageUrl, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = item.description,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?")
                        }
                    }
                }
            }

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = item.category.name.replace("_", " "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (item.outfitReferences.size > 1) {
                    Text(
                        text = "Used in ${item.outfitReferences.size} outfits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Rename button
            if (onRenameClick != null) {
                IconButton(onClick = onRenameClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Rename item",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
