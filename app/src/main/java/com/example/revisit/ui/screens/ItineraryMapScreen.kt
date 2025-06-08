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
import kotlinx.coroutines.launch

@Suppress("ControlFlowWithEmptyBody", "ControlFlowWithEmptyBody")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryMapScreen(
    navController: NavController,
    contactIdsString: String?,
    viewModel: ContactViewModel
) {
    val coroutineScope = rememberCoroutineScope()
    var contactsToShowOnMap by remember { mutableStateOf<List<ContactEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val defaultCameraPosition = LatLng(20.0, 0.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f)
    }

    LaunchedEffect(contactIdsString, viewModel) {
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
            tempContacts = viewModel.allContactsSortedByName.value
            tempContacts.forEachIndexed { index, contact ->
            }
        }

        contactsToShowOnMap = tempContacts.filter { it.latitude != null && it.longitude != null }
        contactsToShowOnMap.forEachIndexed { index, contact ->
        }

        if (tempContacts.isNotEmpty() && tempContacts.size != contactsToShowOnMap.size) {
        }
        viewModel.getContact(5)
        if (contactsToShowOnMap.isNotEmpty()) {
            val boundsBuilder = LatLngBounds.Builder()
            contactsToShowOnMap.forEach { contact ->
                boundsBuilder.include(LatLng(contact.latitude!!, contact.longitude!!))
            }
            coroutineScope.launch {
                try {
                    if (contactsToShowOnMap.size > 1) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150),
                            1000
                        )
                    } else {
                        val singleContact = contactsToShowOnMap.first()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(singleContact.latitude!!, singleContact.longitude!!), 15f),
                            1000
                        )
                    }
                } catch (e: IllegalStateException) {
                    if (contactsToShowOnMap.isNotEmpty()) {
                        if (contactsToShowOnMap.size == 1) {
                            contactsToShowOnMap.firstOrNull()?.let {
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(it.latitude!!, it.longitude!!), 15f)
                            }
                        } else {
                        }
                    }
                }
            }
        } else {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.itinerary_map_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
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
                    properties = MapProperties(
                    )
                ) {
                    contactsToShowOnMap.forEach { contact ->
                        Marker(
                            state = MarkerState(position = LatLng(contact.latitude!!, contact.longitude!!)),
                            title = "${contact.name} ${contact.lastName ?: ""}".trim(),
                            snippet = contact.address
                        )
                    }
                }

                if (isLoading && contactsToShowOnMap.isNotEmpty()) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }

                if (!isLoading && contactsToShowOnMap.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_contacts_to_display_on_map),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
