package com.cs407.memoria.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RatingDialog(
    currentRating: Int?,
    onRatingSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSkip: (() -> Unit)? = null // Optional skip button for post-upload flow
) {
    var selectedRating by remember { mutableStateOf(currentRating ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Rate this Outfit")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "How would you rate this outfit?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Star rating row
                StarRatingBar(
                    rating = selectedRating,
                    onRatingChange = { selectedRating = it },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Show selected rating text
                Text(
                    text = when (selectedRating) {
                        0 -> "Tap a star to rate"
                        1 -> "Poor"
                        2 -> "Fair"
                        3 -> "Good"
                        4 -> "Great"
                        5 -> "Amazing!"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRatingSelected(selectedRating) },
                enabled = selectedRating > 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onSkip != null) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    starSize: Int = 40
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        for (i in 1..maxRating) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier
                    .size(starSize.dp)
                    .clickable { onRatingChange(i) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
fun StarRatingDisplay(
    rating: Int?,
    modifier: Modifier = Modifier,
    starSize: Int = 16
) {
    if (rating != null && rating > 0) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (i <= rating) Color(0xFFFFD700) else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(starSize.dp)
                )
            }
        }
    }
}