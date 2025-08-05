package com.example.revisit.ui.photo // Ajusta el paquete según tu estructura

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.revisit.R // Asegúrate que la R es accesible desde aquí

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewScreen(
    navController: NavController,
    imageUriString: String? // La URI viene codificada
) {
    // Decodificar la URI antes de usarla
    val decodedImageUri = remember(imageUriString) {
        imageUriString?.let { Uri.decode(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.photo_view_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (decodedImageUri != null) {
                AsyncImage(
                    model = decodedImageUri, // Usar la URI decodificada
                    contentDescription = stringResource(id = R.string.contact_photo_full_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit // O la escala que prefieras
                )
            } else {
                Text(stringResource(id = R.string.image_not_available))
            }
        }
    }
}
