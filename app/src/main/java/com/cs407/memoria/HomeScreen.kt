package com.cs407.memoria

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.cs407.memoria.utils.TestImageHelper
import com.cs407.memoria.viewmodel.AuthViewModel
import com.cs407.memoria.viewmodel.OutfitViewModel
import java.io.File

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    outfitViewModel: OutfitViewModel,
    navController: NavController
) {
    val topics = listOf("Pets", "Gym", "School", "Outfits")
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val isLoading by outfitViewModel.isLoading.collectAsState()
    val error by outfitViewModel.error.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturedImageUri != null) {
            currentUser?.uid?.let { userId ->
                outfitViewModel.uploadOutfit(
                    context,
                    capturedImageUri!!,
                    userId,
                    topicName = selectedTopic,
                    note = ""
                )
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
        // Sign out button
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }


        Text(
            text = "Current topic: $selectedTopic",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topics.forEach { topic ->
                OutlinedButton(
                    onClick = { selectedTopic = topic },
                    enabled = !isLoading
                ) {
                    Text(topic)
                }
            }
        }

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
            // TEST: Use drawable image
            Button(
                onClick = {
                    currentUser?.uid?.let { userId ->
                        val testImageUri = TestImageHelper.getUriFromDrawable(
                            context,
                            R.drawable.guy
                        )
                        testImageUri?.let { uri ->
                            outfitViewModel.uploadOutfit(
                                context,
                                uri,
                                userId = userId,
                                topicName = selectedTopic,
                                note = ""
                            )
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
                    val uri = createImageUri(context)
                    capturedImageUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Text("Take Outfit Photo", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("wardrobe") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("View Wardrobe", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("trending_outfits") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "See Trending Outfits",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* TODO: Navigate to suggestions when you add that route */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Get Outfit Suggestions", fontSize = 24.sp)
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

/**
 * Creates a file URI for saving an outfit photo using FileProvider.
 */
fun createImageUri(context: Context): Uri {
    val imageFile = File(context.filesDir, "outfit_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
