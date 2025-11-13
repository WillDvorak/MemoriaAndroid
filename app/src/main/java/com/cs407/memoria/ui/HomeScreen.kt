package com.cs407.memoria.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun HomeScreen(
    onViewWardrobe: () -> Unit,
    onAddOutfit: (Uri) -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
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
            // Photo saved to storage; add to shared list
            onAddOutfit(capturedImageUri!!)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Memoria",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (hasCameraPermission) {
            Button(
                onClick = {
                    val uri = createImageUri(context)
                    capturedImageUri = uri
                    cameraLauncher.launch(uri)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Take Outfit Photo")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onViewWardrobe()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("View Wardrobe")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { /* Navigate to recommendations screen later */ },
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

private fun createImageUri(
    context: android.content.Context
): Uri {
    val imageFile = File(
        context.filesDir,
        "outfit_${System.currentTimeMillis()}.jpg"
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}



