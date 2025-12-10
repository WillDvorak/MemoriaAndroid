package com.cs407.memoria

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.ui.DuplicateConfirmationDialog
import com.cs407.memoria.ui.OutfitDetailScreen
import com.cs407.memoria.ui.SignInScreen
import com.cs407.memoria.ui.WardrobeScreen
import com.cs407.memoria.viewmodel.AuthViewModel
import com.cs407.memoria.viewmodel.OutfitViewModel
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.cs407.memoria.model.ClothingItem
import com.cs407.memoria.ui.TrendingOutfitsScreen
import com.cs407.memoria.sample.TrendingSampleData


private const val TAG = "NavPages"

private const val VISION_API_KEY = "AIzaSyD3Nc3lyPs3YvquiavFs5j67-WD4n-ySro"

@Composable
fun NavPages(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    outfitViewModel: OutfitViewModel = viewModel { OutfitViewModel(VISION_API_KEY) }
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val navController: NavHostController = rememberNavController()
    var selectedOutfit by remember { mutableStateOf<Outfit?>(null) }

    Log.d(TAG, "Current user state: ${currentUser?.uid}")

    if (currentUser == null) {
        Log.d(TAG, "Showing SignInScreen")
        SignInScreen(
            authViewModel = authViewModel,
            onSignInSuccess = {
                Log.d(TAG, "onSignInSuccess callback triggered")
                // Nothing special here; when currentUser changes, this composable will recompose
            }
        )
    } else {
        // Show duplicate confirmation dialog if needed
        val similarItems by outfitViewModel.similarItemsForConfirmation.collectAsState()
        similarItems?.let { (detectedItem, similar) ->
            val tempItem = ClothingItem(
                category = detectedItem.category,
                description = "New ${detectedItem.category.name.lowercase()}",
                dominantColors = detectedItem.colors,
                detectedLabels = detectedItem.labels
            )

            DuplicateConfirmationDialog(
                newItem = tempItem,
                similarItems = similar,
                onConfirmExisting = { existingItem ->
                    outfitViewModel.onUserConfirmsExistingItem(existingItem)
                },
                onCreateNew = {
                    outfitViewModel.onUserCreatesNewItem()
                },
                onDismiss = {
                    outfitViewModel.onDismissDuplicateDialog()
                }
            )
        }

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = modifier
        ) {
            composable("home") {
                HomeScreen(
                    authViewModel = authViewModel,
                    outfitViewModel = outfitViewModel,
                    navController = navController,
                )
            }

            composable("wardrobe") {
                val outfits by outfitViewModel.outfits.collectAsState()
                val isLoading by outfitViewModel.isLoading.collectAsState()

                val context = LocalContext.current
                val userId = currentUser?.uid

                // Gallery picker for adding an outfit from the photo library
                val galleryLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null && userId != null) {
                        outfitViewModel.uploadOutfit(
                            context = context,
                            imageUri = uri,
                            userId = userId,
                            topicName = "Wardrobe",
                            note = ""
                        )
                    }
                }

                LaunchedEffect(currentUser?.uid) {
                    currentUser?.uid?.let { uid ->
                        outfitViewModel.loadOutfits(uid)
                        outfitViewModel.loadClothingItems(uid)
                    }
                }

                WardrobeScreen(
                    outfits = outfits,
                    isLoading = isLoading,
                    onOutfitClick = { outfit ->
                        selectedOutfit = outfit
                        navController.navigate("detail")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    },
                    onAddFromGallery = {
                        galleryLauncher.launch("image/*")
                    }
                )
            }


            composable("detail") {
                selectedOutfit?.let { outfit ->
                    val clothingItems = outfitViewModel.getClothingItemsForOutfit(outfit)
                    val userId = currentUser?.uid

                    if (userId != null) {
                        OutfitDetailScreen(
                            outfit = outfit,
                            clothingItems = clothingItems,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onDeleteClick = {
                                outfitViewModel.deleteOutfit(outfit, userId)
                                navController.popBackStack() // go back after deletion
                            }
                        )
                    }
                }
            }
            
            composable ("trending_outfits") {

                TrendingOutfitsScreen(
                    sections = TrendingSampleData.sections,
                    onOutfitClick = { outfit ->
                        // e.g., navigate to detail page later
                        // navController.navigate("outfit_detail/${outfit.id}")
                    },
                    onLikeClick = { outfit ->
                        // TODO: handle like (update ViewModel, etc.)
                        // For now, maybe just log or show a Snackbar
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

        }
    }
}
