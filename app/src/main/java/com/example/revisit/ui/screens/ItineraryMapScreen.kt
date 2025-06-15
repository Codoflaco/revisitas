package com.example.revisit.ui.screens // o tu paquete de UI

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.example.revisit.R
import com.example.revisit.data.db.ContactEntity
import com.example.revisit.ui.ContactViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material3.MaterialTheme
import com.example.revisit.ui.util.VisitStatusColorUtil
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.ui.graphics.Color
import com.example.revisit.ui.theme.VisitStatusAppColors
import android.util.Log


@Composable
private fun mapComposeColorToHue(composeColor: Color): Float {

    MaterialTheme.colorScheme.tertiary
    MaterialTheme.colorScheme.error
    MaterialTheme.colorScheme.primary


    return when (composeColor) {
        VisitStatusAppColors.DueSoon -> BitmapDescriptorFactory.HUE_YELLOW // Amarillo
        VisitStatusAppColors.Overdue -> BitmapDescriptorFactory.HUE_RED      // Rojo
        VisitStatusAppColors.Today -> BitmapDescriptorFactory.HUE_ORANGE   // Naranja
        VisitStatusAppColors.DueFar -> BitmapDescriptorFactory.HUE_GREEN     // Verde
        VisitStatusAppColors.ColorNoDate -> BitmapDescriptorFactory.HUE_AZURE // O el HUE que prefieras para "sin fecha"
        else -> {
            BitmapDescriptorFactory.HUE_CYAN
        }
    }
}

@Suppress("ControlFlowWithEmptyBody", "ControlFlowWithEmptyBody")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryMapScreen(
    navController: NavController,
    contactIdsString: String?,
    viewModel: ContactViewModel
) {

    var contactsToShowOnMap by remember { mutableStateOf<List<ContactEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val defaultCameraPosition = LatLng(41.15931265586186, -74.25517434297078)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 10f)
    }
    var isMapLoaded by remember { mutableStateOf(false) }

    // PRIMER LaunchedEffect: Solo para cargar datos
    LaunchedEffect(contactIdsString, viewModel) {
        Log.d("MapDebug", "Primer LE: Cargando contactos. contactIdsString: $contactIdsString")
        isLoading = true
        val tempContacts: List<ContactEntity>
        if (!contactIdsString.isNullOrEmpty()) {
            val idsList = contactIdsString.split(',')
                .mapNotNull { it.trim().toIntOrNull() }
            tempContacts = if (idsList.isNotEmpty()) {
                viewModel.getContactsByIds(idsList)
            } else {
                emptyList()
            }
        } else {
            // Asumo que viewModel.allContactsSortedByName es un StateFlow o similar
            // y que ya estás recolectándolo en el ViewModel o aquí si fuera necesario
            tempContacts = viewModel.allContactsSortedByName.value // Asegúrate que esto tenga el valor más reciente
        }

        contactsToShowOnMap = tempContacts.filter { it.latitude != null && it.longitude != null }
        Log.d("MapDebug", "Primer LE: Contactos filtrados para el mapa: ${contactsToShowOnMap.size}")

        if (tempContacts.isNotEmpty() && tempContacts.size != contactsToShowOnMap.size) {
            Log.d("MapDebug", "Primer LE: Algunos contactos no tienen coordenadas.")
            // Aquí podrías mostrar un Toast o un mensaje al usuario si lo deseas
        }
        // viewModel.getContact(5) // Eliminar si era solo una prueba

        // NO hay manipulación de la cámara aquí
        isLoading = false
        Log.d("MapDebug", "Primer LE: Carga de contactos finalizada. isLoading = false")
    }


//    LaunchedEffect(contactIdsString, viewModel) {
//        isLoading = true
//
//        val tempContacts: List<ContactEntity>
//        if (!contactIdsString.isNullOrEmpty()) {
//            val idsList = contactIdsString.split(',')
//                .mapNotNull { it.trim().toIntOrNull() }
//            tempContacts = if (idsList.isNotEmpty()) {
//                viewModel.getContactsByIds(idsList)
//            } else {
//                emptyList()
//            }
//        } else {
//            tempContacts = viewModel.allContactsSortedByName.value
//            tempContacts.forEachIndexed { index, contact ->
//            }
//        }
//
//        contactsToShowOnMap = tempContacts.filter { it.latitude != null && it.longitude != null }
//        contactsToShowOnMap.forEachIndexed { index, contact ->
//        }
//
//        if (tempContacts.isNotEmpty() && tempContacts.size != contactsToShowOnMap.size) {
//        }
//        viewModel.getContact(5)
//        if (contactsToShowOnMap.isNotEmpty()) {
//            val boundsBuilder = LatLngBounds.Builder()
//            contactsToShowOnMap.forEach { contact ->
//                boundsBuilder.include(LatLng(contact.latitude!!, contact.longitude!!))
//            }
//            coroutineScope.launch {
//                try {
//                    if (contactsToShowOnMap.size > 1) {
//                        cameraPositionState.animate(
//                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
//                            1000
//                        )
//                    } else {
//                        val singleContact = contactsToShowOnMap.first()
//                        cameraPositionState.animate(
//                            CameraUpdateFactory.newLatLngZoom(
//                                LatLng(
//                                    singleContact.latitude!!,
//                                    singleContact.longitude!!
//                                ), 15f
//                            ),
//                            1000
//                        )
//                    }
//                } catch (_: IllegalStateException) {
//                    if (contactsToShowOnMap.isNotEmpty()) {
//                        if (contactsToShowOnMap.size == 1) {
//                            contactsToShowOnMap.firstOrNull()?.let {
//                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
//                                    LatLng(
//                                        it.latitude!!,
//                                        it.longitude!!
//                                    ), 15f
//                                )
//                            }
//                        } else {
//                        }
//                    }
//                }
//            }
//        } else {
//            cameraPositionState.position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f)
//        }
//        isLoading = false
//    }

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
            if (isLoading && contactsToShowOnMap.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        mapToolbarEnabled = true
                    ),
                    properties = MapProperties(),
                    onMapLoaded = {
                        isMapLoaded = true
                    }
                ) {
                    contactsToShowOnMap.forEach { contact ->

                        val statusColor = VisitStatusColorUtil.getVisitStatusColor(
                            nextVisitTimestamp = contact.nextVisitTimestamp
                        )
                        val markerHue = mapComposeColorToHue(statusColor)
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    contact.latitude!!,
                                    contact.longitude!!
                                )
                            ),
                            title = "${contact.name} ${contact.lastName ?: ""}".trim(),
                            snippet = contact.address,
                            icon = BitmapDescriptorFactory.defaultMarker(markerHue)
                        )
                    }
                }

                if (isLoading && contactsToShowOnMap.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                if (!isLoading && contactsToShowOnMap.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_contacts_to_display_on_map),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // SEGUNDO LaunchedEffect: Para la lógica del mapa una vez cargado y con contactos
        LaunchedEffect(contactsToShowOnMap, isMapLoaded) {
            Log.d("MapDebug", "Segundo LE: Disparado. isMapLoaded: $isMapLoaded, contactsToShowOnMap: ${contactsToShowOnMap.size}")
            if (!isMapLoaded) {
                Log.d("MapDebug", "Segundo LE: Mapa no cargado aún. Saliendo.")
                // El mapa aún no está cargado, no hacer nada con la cámara todavía
                return@LaunchedEffect
            }

            // El mapa SÍ está cargado (isMapLoaded es true)
            if (contactsToShowOnMap.isNotEmpty()) {
                Log.d("MapDebug", "Segundo LE: Mapa cargado y hay contactos. Preparando animación.")
                val boundsBuilder = LatLngBounds.Builder()
                contactsToShowOnMap.forEach { contact ->
                    boundsBuilder.include(LatLng(contact.latitude!!, contact.longitude!!))
                }

                // Usar el scope del LaunchedEffect para la corutina
                // o el rememberCoroutineScope si prefieres, aunque el del LE es más autocontenido aquí.
                try {
                    if (contactsToShowOnMap.size > 1) {
                        Log.d("MapDebug", "Segundo LE: Animando a múltiples contactos.")
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
                            1000
                        )
                    } else {
                        val singleContact = contactsToShowOnMap.first()
                        Log.d("MapDebug", "Segundo LE: Animando a un solo contacto.")
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(singleContact.latitude!!, singleContact.longitude!!), 15f
                            ),
                            1000
                        )
                    }
                } catch (e: IllegalStateException) {
                    Log.e("MapDebug", "Segundo LE: Error al animar la cámara: ${e.message}")
                    // Fallback si la animación falla (aunque con isMapLoaded=true, es menos probable)
                    if (contactsToShowOnMap.isNotEmpty()) { // Doble check
                        if (contactsToShowOnMap.size == 1) {
                            contactsToShowOnMap.firstOrNull()?.let {
                                Log.d("MapDebug", "Segundo LE: Fallback - Estableciendo posición para un contacto.")
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(it.latitude!!, it.longitude!!), 15f
                                )
                            }
                        } else {
                            // Podrías intentar un cameraPositionState.move() con los bounds si la animación falla
                            Log.d("MapDebug", "Segundo LE: Fallback - Múltiples contactos, considerar move().")
                            try {
                                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
                            } catch (moveEx: Exception) {
                                Log.e("MapDebug", "Segundo LE: Error también en move() fallback: ${moveEx.message}")
                            }
                        }
                    }
                }
            } else { // isMapLoaded es true, pero no hay contactos
                Log.d("MapDebug", "Segundo LE: Mapa cargado pero no hay contactos. Estableciendo posición por defecto.")
                cameraPositionState.position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f)
            }
        }


//        LaunchedEffect(contactsToShowOnMap, isMapLoaded) {
//            if (isMapLoaded && contactsToShowOnMap.isNotEmpty()) {
//                val boundsBuilder = LatLngBounds.Builder()
//                contactsToShowOnMap.forEach { contact ->
//                    boundsBuilder.include(LatLng(contact.latitude!!, contact.longitude!!))
//                }
//                coroutineScope.launch {
//                    try {
//                        if (contactsToShowOnMap.size > 1) {
//                            cameraPositionState.animate(
//                                CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
//                                1000
//                            )
//                        } else {
//                            val singleContact = contactsToShowOnMap.first()
//                            cameraPositionState.animate(
//                                CameraUpdateFactory.newLatLngZoom(
//                                    LatLng(
//                                        singleContact.latitude!!,
//                                        singleContact.longitude!!
//                                    ), 15f
//                                ),
//                                1000
//                            )
//                        }
//                    } catch (e: IllegalStateException) {
//                        if (contactsToShowOnMap.isNotEmpty()) {
//                            if (contactsToShowOnMap.size == 1) {
//                                contactsToShowOnMap.firstOrNull()?.let {
//                                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
//                                        LatLng(
//                                            it.latitude!!,
//                                            it.longitude!!
//                                        ), 15f
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            } else if (isMapLoaded && contactsToShowOnMap.isEmpty()) { // Si el mapa está cargado pero no hay contactos
//                cameraPositionState.position =
//                    CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f)
//            }
//
//        }
    }
}
