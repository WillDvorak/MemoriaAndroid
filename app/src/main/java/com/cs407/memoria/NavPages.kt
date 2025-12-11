package com.cs407.memoria

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.ui.DuplicateConfirmationDialog
import com.cs407.memoria.ui.OutfitDetailScreen
import com.cs407.memoria.ui.OutfitSuggestionScreen
import com.cs407.memoria.ui.RatingDialog
import com.cs407.memoria.ui.SignInScreen
import com.cs407.memoria.ui.TrendingOutfitsScreen
import com.cs407.memoria.ui.WardrobeScreen
import com.cs407.memoria.viewmodel.AuthViewModel
import com.cs407.memoria.viewmodel.OutfitViewModel
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
            val tempItem = com.cs407.memoria.model.ClothingItem(
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
                onCreateNew = { customName ->
                    // Pass the customName from the dialog
                    outfitViewModel.onUserCreatesNewItem(customName)
                },
                onDismiss = {
                    outfitViewModel.onDismissDuplicateDialog()
                }
            )
        }

        // Show rating dialog after outfit upload is complete
        val outfitPendingRating by outfitViewModel.outfitPendingRating.collectAsState()
        outfitPendingRating?.let { outfit ->
            RatingDialog(
                currentRating = outfit.rating,
                onRatingSelected = { rating ->
                    outfitViewModel.rateOutfit(outfit.id, rating)
                },
                onDismiss = {
                    outfitViewModel.dismissRatingDialog()
                },
                onSkip = {
                    outfitViewModel.skipRating()
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
                    onNavigateToWardrobe = {
                        navController.navigate("wardrobe")
                    },
                    onNavigateToSuggestions = {
                        navController.navigate("suggestions")
                    },
                    onNavigateToTrending = {
                        navController.navigate("trending")
                    }
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
                // Get the latest version of the outfit from the list (in case rating was updated)
                val outfits by outfitViewModel.outfits.collectAsState()
                val currentOutfit = selectedOutfit?.let { selected ->
                    outfits.find { it.id == selected.id } ?: selected
                }

                currentOutfit?.let { outfit ->
                    val clothingItems = outfitViewModel.getClothingItemsForOutfit(outfit)
                    val userId = currentUser?.uid

                    if (userId != null) {
                        OutfitDetailScreen(
                            outfit = outfit,
                            clothingItems = clothingItems,
                            onBackClick = {
                                navController.popBackStack()
                            },
                            onRenameItem = { itemId, newName ->
                                outfitViewModel.renameClothingItem(itemId, newName)
                            },
                            onDeleteClick = {
                                outfitViewModel.deleteOutfit(outfit, userId)
                                navController.popBackStack() // go back after deletion
                            },
                            onRateOutfit = { rating ->
                                outfitViewModel.rateOutfit(outfit.id, rating)
                            }
                        )
                    }
                }
            }

            composable("suggestions") {
                val suggestion by outfitViewModel.currentSuggestion.collectAsState()
                val suggestedItems by outfitViewModel.suggestedItems.collectAsState()
                val thumbnail by outfitViewModel.suggestionThumbnail.collectAsState()
                val isLoading by outfitViewModel.isSuggestionLoading.collectAsState()
                val errorMessage by outfitViewModel.suggestionError.collectAsState()
                val userId = currentUser?.uid

                // Load data when entering the screen
                LaunchedEffect(userId) {
                    userId?.let { uid ->
                        outfitViewModel.loadOutfits(uid)
                        outfitViewModel.loadClothingItems(uid)
                        // Generate initial suggestion
                        outfitViewModel.generateSuggestion(uid)
                    }
                }

                // Clean up when leaving
                DisposableEffect(Unit) {
                    onDispose {
                        outfitViewModel.resetSuggestionSession()
                    }
                }

                OutfitSuggestionScreen(
                    suggestion = suggestion,
                    suggestedItems = suggestedItems,
                    compositeThumbnail = thumbnail,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onGenerateNew = {
                        userId?.let { outfitViewModel.generateSuggestion(it) }
                    },
                    onSaveSuggestion = { rating ->
                        userId?.let { outfitViewModel.saveSuggestion(it, rating) }
                    },
                    onDismiss = {
                        outfitViewModel.dismissSuggestion()
                        userId?.let { outfitViewModel.generateSuggestion(it) }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable("trending") {
                TrendingOutfitsScreen(
                    sections = TrendingSampleData.sections,
                    onOutfitClick = { outfit ->
                        // TODO: Navigate to a detail page for trending outfits if needed
                        Log.d(TAG, "Clicked on trending outfit: ${outfit.id}")
                    },
                    onLikeClick = { outfit ->
                        // TODO: Implement like functionality
                        Log.d(TAG, "Liked outfit: ${outfit.id}")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
