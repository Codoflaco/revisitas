package com.example.revisit.ui.screens

import android.location.Geocoder
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.revisit.R
import com.example.revisit.data.db.ContactEntity
import com.example.revisit.ui.ContactViewModel
import com.example.revisit.ui.util.DateTimeUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    contactId: Int,
    viewModel: ContactViewModel
) {
    val context = LocalContext.current
    var contact by remember { mutableStateOf<ContactEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var contactLatLng by remember { mutableStateOf<LatLng?>(null) }
    var geocodingErrorMessage by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(key1 = contactId) {
        isLoading = true
        val fetchedContact = viewModel.getContact(contactId)
        contact = fetchedContact
        if (fetchedContact?.latitude != null && fetchedContact.longitude != null) {
            contactLatLng = LatLng(fetchedContact.latitude, fetchedContact.longitude)
            isLoading = false
        } else if (!fetchedContact?.address.isNullOrBlank()) {
            geocodingErrorMessage = null
            try {
                val geocoder = Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(fetchedContact.address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    contactLatLng = LatLng(location.latitude, location.longitude)
                } else {
                    geocodingErrorMessage = "Dirección no encontrada o inválida."
                }
            } catch (_: IOException) {
                geocodingErrorMessage = "Servicio de geocodificación no disponible."
            } catch (_: IllegalArgumentException) {
                geocodingErrorMessage = "La dirección proporcionada no es válida."
            }
        } else {
            contactLatLng = null
            geocodingErrorMessage = if (fetchedContact != null) "No hay dirección especificada." else null
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val contactName = contact?.name ?: ""
                        val contactLastName = contact?.lastName?.takeIf { it.isNotBlank() } ?: ""
                        val titleText = if (contactLastName.isNotEmpty()) {
                            "$contactName $contactLastName"
                        } else {
                            contactName
                        }.ifEmpty { stringResource(id = R.string.contact_detail_title) }

                        Text(
                            text = titleText,
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                actions = {
                    contact?.let {
                        IconButton(onClick = { navController.navigate("addEditContact/${it.id}") }) {
                            Icon(Icons.Filled.Edit, stringResource(id = R.string.edit_contact))
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading && contact == null) {
                // ...
            } else if (contact == null) {
                // ...
            } else {
                val currentContact = contact!!

                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailEntryStyled(
                            label = stringResource(id = R.string.name_label),
                            value = currentContact.name,
                            modifier = Modifier.weight(1f)

                        )
                        currentContact.lastName?.takeIf { it.isNotBlank() }?.let {
                            DetailEntryStyled(
                                label = stringResource(id = R.string.last_name_label),
                                value = it,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Teléfono y Territorio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        currentContact.phoneNumber?.takeIf { it.isNotBlank() }?.let {
                            DetailEntryStyled(
                                label = stringResource(id =R.string.phone_number_label),
                                value = it,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(Modifier.weight(1f))

                        currentContact.territory?.toString()?.takeIf { it.isNotBlank() }?.let {
                            DetailEntryStyled(
                                label = stringResource(id = R.string.territory_label),
                                value = it,
                                modifier = Modifier.weight(1f)
                            )
                        } ?: Spacer(Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Address
                    currentContact.address?.takeIf { it.isNotBlank() }?.let {
                        DetailEntryStyled(label = stringResource(id = R.string.address_label), value = it)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Notes
                    currentContact.notes?.takeIf { it.isNotBlank() }?.let {
                        NotesDetailItemWithInternalScroll(
                            label = stringResource(id = R.string.notes_label),
                            value = it
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailEntryStyled(
                            label = stringResource(id = R.string.creation_or_first_visit_label),
                            value = DateTimeUtils.formatDateTimeForDisplay(currentContact.creationOrFirstVisitTimestamp),
                            modifier = Modifier.weight(1f)
                        )
                        DetailEntryStyled(
                            label = stringResource(id = R.string.next_visit_label), // Etiqueta más corta
                            value = DateTimeUtils.formatDateTimeForDisplay(currentContact.nextVisitTimestamp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    val mapTargetLocation = contactLatLng
                    val defaultFallbackLocation = LatLng(37.4220, -122.0840)

                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(
                            mapTargetLocation ?: defaultFallbackLocation,
                            if (mapTargetLocation != null) 15f else 10f
                        )
                    }

                    LaunchedEffect(mapTargetLocation) {
                        val locationToAnimate = mapTargetLocation ?: defaultFallbackLocation
                        val zoomToAnimate = if (mapTargetLocation != null) 15f else 10f
                        cameraPositionState.animate(
                            update = CameraUpdateFactory.newLatLngZoom(locationToAnimate, zoomToAnimate),
                            durationMs = 1000
                        )
                    }

                    when {
                        geocodingErrorMessage != null && !currentContact.address.isNullOrBlank() && mapTargetLocation == null -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                                    .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = geocodingErrorMessage!!,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        currentContact.address.isNullOrBlank() && mapTargetLocation == null -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.1f))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.no_address_to_display_on_map),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        mapTargetLocation != null -> {
                            GoogleMap(
                                modifier = Modifier.fillMaxSize().matchParentSize(),
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = true,
                                    scrollGesturesEnabled = true,
                                    zoomGesturesEnabled = true,
                                    tiltGesturesEnabled = true
                                )
                            ) {
                                Marker(
                                    state = MarkerState(position = mapTargetLocation),
                                    title = currentContact.name,
                                    snippet = currentContact.address
                                )
                            }
                        }
                        else -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.1f))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.map_unavailable),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall
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
fun DetailEntryStyled(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val labelHorizontalPadding = 6.dp
    val labelVerticalPadding = 1.dp
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    val cornerRadius = 8.dp
    val borderWidth = 1.dp
    val labelHeightEstimate = 18.dp
    val labelBackgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = labelHeightEstimate / 2)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(borderWidth, outlineColor),
                    RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = 12.dp,
                    y = -(labelHeightEstimate / 2)

                )
                .zIndex(1f)
                .background(labelBackgroundColor)
                .padding(horizontal = labelHorizontalPadding, vertical = labelVerticalPadding)
        )
    }
}

@Composable
fun NotesDetailItemWithInternalScroll(label: String, value: String) {
    val labelHorizontalPadding = 6.dp
    val labelVerticalPadding = 1.dp
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
    val cornerRadius = 8.dp
    val borderWidth = 1.dp
    val labelHeightEstimate = 18.dp
    val labelBackgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(top = labelHeightEstimate / 2)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp, max = 150.dp)
                .border(
                    borderWidth,
                    outlineColor,
                    RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                modifier = Modifier.verticalScroll(scrollState)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = 12.dp,
                    y = -(labelHeightEstimate / 2)
                )
                .zIndex(1f)
                .background(labelBackgroundColor)
                .padding(horizontal = labelHorizontalPadding, vertical = labelVerticalPadding)
        )
    }
}
