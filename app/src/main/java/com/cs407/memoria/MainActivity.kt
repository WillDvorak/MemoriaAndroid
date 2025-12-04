package com.cs407.memoria

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cs407.memoria.model.Outfit
import com.cs407.memoria.ui.DuplicateConfirmationDialog
import com.cs407.memoria.ui.OutfitDetailScreen
import com.cs407.memoria.ui.SignInScreen
import com.cs407.memoria.ui.WardrobeScreen
import com.cs407.memoria.utils.TestImageHelper
import com.cs407.memoria.viewmodel.AuthViewModel
import com.cs407.memoria.viewmodel.OutfitViewModel
import java.io.File

private const val TAG = "MainActivity"

private const val VISION_API_KEY = "AIzaSyD3Nc3lyPs3YvquiavFs5j67-WD4n-ySro"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = viewModel()
                    val outfitViewModel: OutfitViewModel = viewModel { OutfitViewModel(VISION_API_KEY) }
                    val currentUser by authViewModel.currentUser.collectAsState()

                    var currentScreen by remember { mutableStateOf("home") }
                    var selectedOutfit by remember { mutableStateOf<Outfit?>(null) }

                    Log.d(TAG, "Current user state: ${currentUser?.uid}")

                    if (currentUser == null) {
                        Log.d(TAG, "Showing SignInScreen")
                        SignInScreen(
                            authViewModel = authViewModel,
                            onSignInSuccess = {
                                Log.d(TAG, "onSignInSuccess callback triggered")
                            }
                        )
                    } else {
                        // Show duplicate confirmation dialog if needed
                        val similarItems by outfitViewModel.similarItemsForConfirmation.collectAsState()
                        similarItems?.let { (detectedItem, similar) ->
                            // Create temporary ClothingItem for display
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
                                    outfitViewModel.onUserCreatesNewItem(customName)
                                },
                                onDismiss = {
                                    outfitViewModel.onDismissDuplicateDialog()
                                }
                            )
                        }

                        when (currentScreen) {
                            "home" -> HomeScreen(
                                authViewModel = authViewModel,
                                outfitViewModel = outfitViewModel,
                                onNavigateToWardrobe = { currentScreen = "wardrobe" }
                            )
                            "wardrobe" -> {
                                val outfits by outfitViewModel.outfits.collectAsState()
                                val isLoading by outfitViewModel.isLoading.collectAsState()

                                LaunchedEffect(Unit) {
                                    currentUser?.uid?.let {
                                        outfitViewModel.loadOutfits(it)
                                        outfitViewModel.loadClothingItems(it)
                                    }
                                }

                                WardrobeScreen(
                                    outfits = outfits,
                                    isLoading = isLoading,
                                    onOutfitClick = { outfit ->
                                        selectedOutfit = outfit
                                        currentScreen = "detail"
                                    },
                                    onBackClick = { currentScreen = "home" }
                                )
                            }
                            "detail" -> {
                                selectedOutfit?.let { outfit ->
                                    val clothingItems = outfitViewModel.getClothingItemsForOutfit(outfit)
                                    OutfitDetailScreen(
                                        outfit = outfit,
                                        clothingItems = clothingItems,
                                        onBackClick = { currentScreen = "wardrobe" },
                                        onRenameItem = { itemId, newName ->
                                            outfitViewModel.renameClothingItem(itemId, newName)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun HomeScreen(
        authViewModel: AuthViewModel,
        outfitViewModel: OutfitViewModel,
        onNavigateToWardrobe: () -> Unit
    ) {
        val context = LocalContext.current
        val currentUser by authViewModel.currentUser.collectAsState()
        val isLoading by outfitViewModel.isLoading.collectAsState()
        val error by outfitViewModel.error.collectAsState()

        var hasCameraPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasCameraPermission = isGranted
        }

        // Camera launcher
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && capturedImageUri != null) {
                // Photo was captured successfully, now upload it
                currentUser?.uid?.let { userId ->
                    outfitViewModel.uploadOutfit(context, capturedImageUri!!, userId)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sign out button at the top right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { authViewModel.signOut() }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Memoria",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
                Text("Processing outfit...", modifier = Modifier.padding(bottom = 16.dp))
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (hasCameraPermission) {
                // TEST BUTTON - Uses drawable image instead of camera
                Button(
                    onClick = {
                        currentUser?.uid?.let { userId ->
                            // Replace R.drawable.guy with your actual drawable name
                            val testImageUri = TestImageHelper.getUriFromDrawable(
                                context,
                                R.drawable.guy  // Change this to your drawable name
                            )
                            testImageUri?.let { uri ->
                                Log.d(TAG, "Testing with drawable image: $uri")
                                outfitViewModel.uploadOutfit(context, uri, userId)
                            } ?: run {
                                Log.e(TAG, "Failed to create test image URI")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("TEST: Use Drawable Image")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val uri = createImageUri()
                        capturedImageUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    Text("Take Outfit Photo")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToWardrobe,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("View Wardrobe")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Navigate to recommendations screen */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Get Outfit Suggestions")
                }
            } else {
                Text(
                    text = "Camera permission is required to take photos",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Grant Camera Permission")
                }
            }
        }
    }

    //save photo in local app storage as timestamp
    private fun createImageUri(): Uri {
        val imageFile = File(filesDir, "outfit_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            imageFile
        )
    }
}