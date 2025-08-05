package com.example.revisit.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.revisit.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

const val DEFAULT_MAP_ZOOM = 15f
val FALLBACK_LATLNG = LatLng(34.0522, -118.2437)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onLocationSelected: (lat: Double, lng: Double) -> Unit,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initialLatitude?.let { lat -> initialLongitude?.let { lng -> LatLng(lat, lng) } } ?: FALLBACK_LATLNG,
            if (initialLatitude != null && initialLongitude != null) DEFAULT_MAP_ZOOM else 10f
        )
    }

    var isMyLocationEnabledState by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("MapPicker", "Location permission granted")
                isMyLocationEnabledState = true
                try {
                    // *** CORRECCIÓN AQUÍ ***
                    fusedLocationClient.lastLocation.addOnSuccessListener { locationResult ->
                        if (locationResult != null) {
                            val userLatLng = LatLng(locationResult.latitude, locationResult.longitude)
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_MAP_ZOOM))
                            Log.d("MapPicker", "Moved to last known location: $userLatLng")
                        } else {
                            Log.d("MapPicker", "Last known location is null, even after permission grant.")
                        }
                    }.addOnFailureListener { e ->
                        Log.e("MapPicker", "Error getting last location after permission grant", e)
                    }
                } catch (e: SecurityException) {
                    Log.e("MapPicker", "SecurityException after permission grant (should not happen)", e)
                }
            } else {
                Log.d("MapPicker", "Location permission denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MapPicker", "Location permission already granted on launch")
                isMyLocationEnabledState = true
                if (initialLatitude == null || initialLongitude == null) {
                    try {
                        // *** CORRECCIÓN AQUÍ ***
                        fusedLocationClient.lastLocation.addOnSuccessListener { locationResult ->
                            if (locationResult != null) {
                                val userLatLng = LatLng(locationResult.latitude, locationResult.longitude)
                                cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_MAP_ZOOM))
                                Log.d("MapPicker", "Moved to last known location: $userLatLng")
                            } else {
                                Log.d("MapPicker", "Last known location is null.")
                            }
                        }.addOnFailureListener { e ->
                            Log.e("MapPicker", "Error getting last location", e)
                        }
                    } catch (e: SecurityException) {
                        Log.e("MapPicker", "SecurityException getting last location", e)
                    }
                } else {
                    Log.d("MapPicker", "Using provided initial location: Lat=$initialLatitude, Lng=$initialLongitude")
                }
            }
            else -> {
                Log.d("MapPicker", "Location permission not granted, requesting.")
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.pick_location_on_map)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    selectedLatLng = latLng
                    Log.d("MapPicker", "Map clicked at: $latLng")
                },
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = isMyLocationEnabledState
                ),
                properties = MapProperties(
                    isMyLocationEnabled = isMyLocationEnabledState
                )
            ) {
                selectedLatLng?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = stringResource(R.string.selected_location_marker_title),
                        snippet = stringResource(R.string.tap_button_to_confirm_marker_snippet)
                    )
                }
            }

            if (selectedLatLng != null) {
                Button(
                    onClick = {
                        selectedLatLng?.let { onLocationSelected(it.latitude, it.longitude) }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(stringResource(R.string.confirm_location_button))
                }
            } else {
                Text(
                    text = stringResource(R.string.tap_on_map_to_select_location_instruction),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), shape = MaterialTheme.shapes.medium)
                        .padding(8.dp)
                )
            }
        }
    }
}
