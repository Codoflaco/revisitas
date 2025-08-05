package com.example.revisit.ui.itinerary

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.revisit.R
import com.example.revisit.data.local.ContactEntity
import com.example.revisit.ui.contacts.ContactViewModel
import com.example.revisit.ui.util.VisitStatusColorUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import android.graphics.Color as AndroidColor
import com.example.revisit.ui.util.createMarkerWithLabelBitmap

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryMapScreen(
    navController: NavController,
    contactIdsString: String?,
    viewModel: ContactViewModel
) {
    val currentContext = LocalContext.current

    var contactsToShowOnMap by remember { mutableStateOf<List<ContactEntity>>(emptyList()) }
    var isLoadingContacts by remember { mutableStateOf(true) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                currentContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var initialCameraAdjustmentDone by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.159163900964074, -74.2551983477263), 5f)

    }
    var isMapLoaded by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("MapDebug", "Permiso de ubicación CONCEDIDO.")
                hasLocationPermission = true

            } else {
                Log.d("MapDebug", "Permiso de ubicación DENEGADO.")
                hasLocationPermission = false
            }
        }
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(currentContext) }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            Log.d("MapDebug", "Permiso no concedido. Solicitando permiso de ubicación...")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    LaunchedEffect(hasLocationPermission, isMapLoaded) {
        if (hasLocationPermission && isMapLoaded) {
            Log.d("MapDebug", "Intentando obtener la ubicación del usuario (LE hasLocationPermission, isMapLoaded)...")
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            userLocation = LatLng(location.latitude, location.longitude)
                            Log.d("MapDebug", "Ubicación del usuario obtenida: $userLocation")
                        } else {
                            Log.d("MapDebug", "Última ubicación conocida es null.")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("MapDebug", "Error al obtener la ubicación: ${e.message}", e)
                    }
            } catch (e: SecurityException) {
                Log.e("MapDebug", "Excepción de seguridad al obtener la ubicación (ya chequeado?): ${e.message}", e)
            }
        } else {
            if (!hasLocationPermission) Log.d("MapDebug", "LE hasLocationPermission: No hay permiso de ubicación.")
            if (!isMapLoaded) Log.d("MapDebug", "LE hasLocationPermission: Mapa aún no cargado.")
        }
    }

    LaunchedEffect(contactIdsString, viewModel) {
        Log.d("MapDebug", "Primer LE (Contactos): Cargando contactos. contactIdsString: $contactIdsString")
        isLoadingContacts = true
        val tempContacts: List<ContactEntity> =
            if (!contactIdsString.isNullOrBlank() && contactIdsString.uppercase() != "ALL") {
                val idsList = contactIdsString.split(',')
                    .mapNotNull { it.trim().toIntOrNull() }
                if (idsList.isNotEmpty()) {
                    viewModel.getContactsByIds(idsList)
                } else {
                    Log.w("MapDebug", "contactIdsString no era nulo pero no contenía IDs válidos: '$contactIdsString'")
                    emptyList()
                }
            } else {
                viewModel.allContactsSortedByName.value
            }

        contactsToShowOnMap = tempContacts.filter { it.latitude != null && it.longitude != null }
        Log.d("MapDebug", "Primer LE (Contactos): Contactos filtrados para el mapa: ${contactsToShowOnMap.size} de ${tempContacts.size}")

        if (tempContacts.isNotEmpty() && tempContacts.size != contactsToShowOnMap.size) {
            Log.d("MapDebug", "Primer LE (Contactos): ${tempContacts.size - contactsToShowOnMap.size} contactos no tienen coordenadas.")
        }
        isLoadingContacts = false
        Log.d("MapDebug", "Primer LE (Contactos): Carga de contactos finalizada. isLoadingContacts = false")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.itinerary_map_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            val showLoadingIndicator = isLoadingContacts
            if (showLoadingIndicator) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (contactsToShowOnMap.isEmpty() && userLocation == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_contacts_or_location_to_display_on_map),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.contact_verification_and_valid_address),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    if (!hasLocationPermission) {
                        Button(
                            onClick = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(stringResource(R.string.grant_location_permission))
                        }
                    }
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        mapToolbarEnabled = true,
                        myLocationButtonEnabled = hasLocationPermission
                    ),
                    properties = MapProperties(
                        isMyLocationEnabled = hasLocationPermission
                        // mapStyleOptions = MapStyleOptions.loadRawResourceStyle(currentContext, R.raw.map_style)
                    ),
                    onMapLoaded = {
                        if (!isMapLoaded) {
                            isMapLoaded = true
                            Log.d("MapDebug", "GoogleMap onMapLoaded CALLED.")
                        }
                    }
                ) {
                    // val currentContextForMarkers = LocalContext.current // Ya tenemos currentContext definido arriba

                    contactsToShowOnMap.forEach { contact ->
                        val statusComposeColor = VisitStatusColorUtil.getVisitStatusColor(
                            nextVisitTimestamp = contact.nextVisitTimestamp
                        )
                        val customIconWithLabel = remember(contact.id, contact.name, contact.lastName, statusComposeColor) {
                            Log.d("MarkerBitmap", "Creando/obteniendo bitmap para: ${contact.name} (ID: ${contact.id}) con color: $statusComposeColor")
                            try {
                                createMarkerWithLabelBitmap(
                                    context = currentContext,
                                    name = contact.name,
                                    lastName = contact.lastName ?: "",
                                    markerIconResId = R.drawable.ic_map_pin,
                                    markerTintColor = statusComposeColor,
                                    labelTextColor = AndroidColor.BLACK,
                                    labelBackgroundColor = AndroidColor.WHITE,
                                    iconWidth = 70,
                                    iconHeight = 70,
                                    labelOffsetY = 5
                                )
                            } catch (e: Exception) {
                                Log.e("MarkerBitmap", "Error creando bitmap para ${contact.name}: ${e.message}", e)
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            }
                        }

                        Marker(
                            state = MarkerState(
                                position = LatLng(contact.latitude!!, contact.longitude!!)
                            ),
                            title = "${contact.name} ${contact.lastName ?: ""}".trim(),
                            snippet = contact.address ?: "",
                            icon = customIconWithLabel,
                            anchor = Offset(0.5f, 0.95f),
                            onClick = {
                                Log.d("MapClick", "Clic en marcador etiquetado: ${contact.name}, ID: ${contact.id}")
                                navController.navigate("contactDetail/${contact.id}")
                                true
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(contactsToShowOnMap, userLocation, isMapLoaded, isLoadingContacts, initialCameraAdjustmentDone) {
            Log.d("MapDebug", "Cámara LE: INICIO. isMapLoaded=$isMapLoaded, isLoadingContacts=$isLoadingContacts, initialCameraAdjustmentDone(entrada)=$initialCameraAdjustmentDone, contacts=${contactsToShowOnMap.size}, userLoc=$userLocation")
            if (!isMapLoaded || isLoadingContacts) {
                if(!isMapLoaded) Log.d("MapDebug", "Cámara LE: Mapa no cargado.")
                if(isLoadingContacts) Log.d("MapDebug", "Cámara LE: Contactos cargando.")
                return@LaunchedEffect
            }

            val pointsToConsider = mutableListOf<LatLng>()
            contactsToShowOnMap.forEach { contact ->
                pointsToConsider.add(LatLng(contact.latitude!!, contact.longitude!!))
            }
            userLocation?.let { loc -> pointsToConsider.add(loc) }
            Log.d("MapDebug", "Cámara LE: pointsToConsider.size = ${pointsToConsider.size}")


            if (pointsToConsider.isEmpty()) {
                if (!initialCameraAdjustmentDone) {
                    Log.d("MapDebug", "Cámara LE: Sin puntos. Moviendo a Default (España) sin animación.")
                    val defaultLatLng = LatLng(40.416775, -3.703790)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 5f))
                    initialCameraAdjustmentDone = true
                    Log.d("MapDebug", "Cámara LE: DefaultEspaña. initialCameraAdjustmentDone(después)=$initialCameraAdjustmentDone")
                }else {
                    Log.d("MapDebug", "Cámara LE: Sin puntos PERO ajuste inicial YA HECHO. No se mueve.")
                }
                return@LaunchedEffect
            }

            val cameraUpdate = if (pointsToConsider.size == 1) {
                Log.d("MapDebug", "Cámara LE: Un solo punto. Zoom a ${pointsToConsider.first()}")
                CameraUpdateFactory.newLatLngZoom(pointsToConsider.first(), 15f) // Zoom para un solo punto
            } else {
                Log.d("MapDebug", "Cámara LE: Múltiples puntos. Creando bounds.")
                val boundsBuilder = LatLngBounds.Builder()
                pointsToConsider.forEach { boundsBuilder.include(it) }
                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150) // Padding de 150px
            }

            try {
                if (!initialCameraAdjustmentDone) {
                    Log.d("MapDebug", "Cámara LE: MOVIMIENTO INICIAL de cámara.")
                    cameraPositionState.animate(cameraUpdate)
                    initialCameraAdjustmentDone = true
                    Log.d("MapDebug", "Cámara LE: MovimientoInicial. initialCameraAdjustmentDone(después)=$initialCameraAdjustmentDone")
                } else {
                    if (!cameraPositionState.isMoving) {
                        Log.d("MapDebug", "Cámara LE: ANIMANDO cámara a nuevos puntos.")
                        cameraPositionState.animate(cameraUpdate, 1500)
                    } else {
                        Log.d("MapDebug", "Cámara LE: Cámara ya se está moviendo, no se animará.")
                    }
                }
            } catch (e: IllegalStateException) {
                Log.e("MapDebug", "Cámara LE: Error al mover/animar cámara (IllegalStateException): ${e.message}")
                if (!initialCameraAdjustmentDone) {
                    try {
                        Log.d("MapDebug", "Cámara LE: FALLBACK MOVIMIENTO INICIAL con puntos. initialCameraAdjustmentDone(antes)=$initialCameraAdjustmentDone")
                        cameraPositionState.animate(cameraUpdate)
                        initialCameraAdjustmentDone = true
                        Log.d("MapDebug", "Cámara LE: FallbackMovimiento. initialCameraAdjustmentDone(después)=$initialCameraAdjustmentDone")

                    } catch (moveEx: Exception) {
                        Log.e("MapDebug", "Cámara LE: Error también en move() fallback inicial: ${moveEx.message}")
                    }
                }
            }
        }
    }
}

