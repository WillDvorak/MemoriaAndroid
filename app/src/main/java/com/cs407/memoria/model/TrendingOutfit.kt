package com.cs407.memoria.model

data class TrendingOutfit(
    val id: String,
    val imageUrl: String,
    val username: String,
    val likes: Int
)

data class TrendingOutfitSection(
    val title: String,
    val trendingOutfits: List<TrendingOutfit>
)