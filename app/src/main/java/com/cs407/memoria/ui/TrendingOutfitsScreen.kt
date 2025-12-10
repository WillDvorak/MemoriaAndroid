package com.cs407.memoria.ui

import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.cs407.memoria.model.TrendingOutfit
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.cs407.memoria.model.TrendingOutfitSection
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar


@Composable
fun OutfitSectionRow(
    section: TrendingOutfitSection,
    onOutfitClick: (TrendingOutfit) -> Unit = {},
    onLikeClick: (TrendingOutfit) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(section.trendingOutfits, key = { it.id }) { outfit ->
                TrendingOutfitCard(
                    outfit = outfit,
                    onClick = onOutfitClick,
                    onLikeClick = onLikeClick
                )
            }
        }
    }
}


@Composable
fun TrendingOutfitCard(
    outfit: TrendingOutfit,
    modifier: Modifier = Modifier,
    onClick: (TrendingOutfit) -> Unit = {},
    onLikeClick: (TrendingOutfit) -> Unit = {}
) {
    Card(
        modifier = modifier
            .width(180.dp)
            .clickable { onClick(outfit) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image section
            AsyncImage(
                model = outfit.imageUrl,
                contentDescription = "Outfit by ${outfit.username}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )

            // Username + likes row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = outfit.username,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick(outfit) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.FavoriteBorder,
                        contentDescription = "Like",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = outfit.likes.toString(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingOutfitsScreen(
    sections: List<TrendingOutfitSection>,
    onOutfitClick: (TrendingOutfit) -> Unit = {},
    onLikeClick: (TrendingOutfit) -> Unit = {},
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                title = { Text("Trending Outfits") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(sections, key = { it.title }) { section ->
                OutfitSectionRow(
                    section = section,
                    onOutfitClick = onOutfitClick,
                    onLikeClick = onLikeClick
                )
            }
        }
    }
}

