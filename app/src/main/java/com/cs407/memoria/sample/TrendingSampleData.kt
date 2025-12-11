package com.cs407.memoria.sample

import com.cs407.memoria.model.TrendingOutfit
import com.cs407.memoria.model.TrendingOutfitSection

object TrendingSampleData {

    val sections: List<TrendingOutfitSection> = listOf(
        TrendingOutfitSection(
            title = "Trending Men's Outfits",
            trendingOutfits = listOf(
                TrendingOutfit(
                    id = "m1",
                    imageUrl = "https://i.pinimg.com/736x/f2/06/64/f206646caed7d4d7413817657e986c60.jpg",
                    username = "menswear_guy",
                    likes = 82
                ),
                TrendingOutfit(
                    id = "m2",
                    imageUrl = "https://i.pinimg.com/736x/cb/52/75/cb5275b47f7b70e763f39ca83aa324a9.jpg",
                    username = "streetstyle_jay",
                    likes = 153
                ),
                TrendingOutfit(
                    id = "m3",
                    imageUrl = "https://i.pinimg.com/736x/13/53/8a/13538a6c087baeb17724a0a8320045ea.jpg",
                    username = "dapper_dan",
                    likes = 402
                ),
                TrendingOutfit(
                    id = "m4",
                    imageUrl = "https://i.pinimg.com/736x/07/67/a7/0767a7ee91f4f9a98d096e4cd9d28769.jpg",
                    username = "casual_fitz",
                    likes = 92
                ),
                TrendingOutfit(
                    id = "m5",
                    imageUrl = "https://i.pinimg.com/736x/73/8e/08/738e082f2d02be05f100f2094d2267aa.jpg",
                    username = "johnWearsClothes",
                    likes = 112
                )
            )
        ),
        TrendingOutfitSection(
            title = "Trending Women's Outfits",
            trendingOutfits = listOf(
                TrendingOutfit(
                    id = "w1",
                    imageUrl = "https://i.pinimg.com/736x/32/19/90/321990cea1be77dbce564608a45b7bf5.jpg",
                    username = "fashion_lillian",
                    likes = 210
                ),
                TrendingOutfit(
                    id = "w2",
                    imageUrl = "https://i.pinimg.com/736x/97/15/d9/9715d9f5ec361d5ee129af325dcee31f.jpg",
                    username = "cozyfits",
                    likes = 142
                ),
                TrendingOutfit(
                    id = "w3",
                    imageUrl = "https://i.pinimg.com/736x/1b/a6/1b/1ba61b76ce6780a99a62bd3c111495ed.jpg",
                    username = "favoriteFitz",
                    likes = 154
                ),
                TrendingOutfit(
                    id = "w4",
                    imageUrl = "https://i.pinimg.com/736x/d8/25/da/d825da9d8f7937d3ed2019038cd307f5.jpg",
                    username = "iheartclothes",
                    likes = 123
                ),
                TrendingOutfit(
                    id = "w5",
                    imageUrl = "https://i.pinimg.com/736x/22/e7/4d/22e74deb8602290eaeb167a04ec41ea2.jpg",
                    username = "fashionLover",
                    likes = 182
                )
            )
        ),
        TrendingOutfitSection(
            title = "Trending Winter Outfits",
            trendingOutfits = listOf(
                TrendingOutfit(
                    id = "win1",
                    imageUrl = "https://i.pinimg.com/736x/11/24/32/112432203f57ced49cd6e4bfedfa3f8b.jpg",
                    username = "snowdayfits",
                    likes = 215
                ),
                TrendingOutfit(
                    id = "win2",
                    imageUrl = "https://i.pinimg.com/736x/2d/9f/ae/2d9faed7cb3ed7f835a646c6deac3059.jpg",
                    username = "bellaFashion",
                    likes = 305
                ),
                TrendingOutfit(
                    id = "win3",
                    imageUrl = "https://i.pinimg.com/236x/a8/8b/ed/a88bed783d3ffbbae88a685d4f40bb11.jpg",
                    username = "needmoreclothes",
                    likes = 198
                ),
                TrendingOutfit(
                    id = "win4",
                    imageUrl = "https://i.pinimg.com/736x/c6/e9/d0/c6e9d0d62e1ac8356ab88f265718bd92.jpg",
                    username = "dudeFits",
                    likes = 132
                ),
                TrendingOutfit(
                    id = "win5",
                    imageUrl = "https://i.pinimg.com/736x/1a/7c/ae/1a7cae355d10e346c43a95d35ab98669.jpg",
                    username = "winterman",
                    likes = 98
                )
            )
        )
    )
}
