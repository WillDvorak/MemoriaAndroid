package com.cs407.memoria

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.memoria.ui.HomeScreen
import com.cs407.memoria.ui.WardrobeScreen


@Composable
fun Memoria(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    // Shared state: outfit URIs for the whole app
    val outfitUris = remember { mutableStateListOf<Uri>() }

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(
                onViewWardrobe = {
                    navController.navigate("wardrobe")
                },
                onAddOutfit = { uri ->
                    outfitUris.add(uri)
                }
            )
        }

        composable("wardrobe") {
            WardrobeScreen(
                outfitImages = outfitUris
            )
        }
    }
}