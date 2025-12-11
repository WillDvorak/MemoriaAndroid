package com.cs407.memoria.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cs407.memoria.R

data class TestImage(
    val name: String,
    val resourceId: Int
)

@Composable
fun TestImageSelectorDialog(
    onImageSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // List of test images - add more entries as you add more drawables
    // Named 1, 2, 3, 4, etc. in drawable folder
    val testImages = remember {
        listOf(
            TestImage("Image 1", R.drawable.guy),
            TestImage("Image 2", R.drawable.test2),
            TestImage("Image 3", R.drawable.test3),
            TestImage("Image 4", R.drawable.test5),
            TestImage("Image 4", R.drawable.test4),
            TestImage("Image 4", R.drawable.test6),
            TestImage("Image 4", R.drawable.test7),
            TestImage("Image 4", R.drawable.test8),
            TestImage("Image 4", R.drawable.test1)

            // Add more images here as needed:
            // TestImage("Image 5", R.drawable.test_5),
            // TestImage("Image 6", R.drawable.test_6),
        ).filter { testImage ->
            // Filter out images that don't exist (in case some aren't added yet)
            try {
                context.resources.getDrawable(testImage.resourceId, null)
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Test Image")
        },
        text = {
            if (testImages.isEmpty()) {
                Text("No test images found. Please add images named test_1, test_2, etc. to the drawable folder.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testImages) { testImage ->
                        TestImageCard(
                            testImage = testImage,
                            onClick = {
                                onImageSelected(testImage.resourceId)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TestImageCard(
    testImage: TestImage,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val bitmap = remember(testImage.resourceId) {
        try {
            BitmapFactory.decodeResource(context.resources, testImage.resourceId)
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = testImage.name,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("?")
                }
            }

            Text(
                text = testImage.name,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}