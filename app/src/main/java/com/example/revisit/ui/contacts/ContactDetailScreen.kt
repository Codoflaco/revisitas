package com.example.revisit.ui.contacts

import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.revisit.R
import com.example.revisit.data.local.ContactEntity
import com.example.revisit.util.DateTimeUtils
import com.example.revisit.ui.util.VisitStatusColorUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.IOException
import android.util.Log
import androidx.compose.foundation.layout.RowScope
import com.example.revisit.ui.util.createMarkerWithLabelBitmap
import androidx.compose.material.icons.automirrored.filled.Chat
import android.content.Intent
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.filled.Call
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.example.revisit.util.PhoneNumberHelper


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    contactId: Int,
    viewModel: ContactViewModel,
) {
    val context = LocalContext.current

    var defaultDeviceRegion by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(key1 = context) {
        defaultDeviceRegion = PhoneNumberHelper.getDeviceCountryCode(context)
        Log.d("ContactDetailScreen", "Región por defecto del dispositivo: $defaultDeviceRegion")
    }

    var contact by remember { mutableStateOf<ContactEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var contactLatLng by remember { mutableStateOf<LatLng?>(null) }
    var geocodingErrorMessage by remember { mutableStateOf<String?>(null) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLatState = savedStateHandle?.getLiveData<Double>("picked_latitude")?.observeAsState()
    val pickedLngState = savedStateHandle?.getLiveData<Double>("picked_longitude")?.observeAsState()

    LaunchedEffect(pickedLatState?.value, pickedLngState?.value, contact) {
        val lat = pickedLatState?.value
        val lng = pickedLngState?.value

        Log.d("ContactDetailScreen_Picker", "Picker Effect Triggered. Lat: $lat, Lng: $lng, Contact Loaded: ${contact != null}, Contact ID: ${contact?.id}")

        if (lat != null && lng != null) {
            val currentContact = contact

            if (currentContact != null && currentContact.id != 0) {
                Log.d("ContactDetailScreen_Picker", "Attempting to save coordinates for contact ID: ${currentContact.id}. Lat: $lat, Lng: $lng")
                viewModel.updateContactCoordinates(currentContact.id, lat, lng)
                contactLatLng = LatLng(lat, lng)

                savedStateHandle.remove<Double>("picked_latitude")
                savedStateHandle.remove<Double>("picked_longitude")
                Log.d("ContactDetailScreen_Picker", "Coordinates processed and removed from SavedStateHandle.")
            } else {
                if (currentContact == null) {
                    Log.w("ContactDetailScreen_Picker", "Received coordinates (Lat: $lat, Lng: $lng) but CONTACT IS NULL. Coordinates not saved yet. Waiting for contact to load or effect to re-trigger.")
                } else {
                    Log.w("ContactDetailScreen_Picker", "Received coordinates (Lat: $lat, Lng: $lng) but contact ID is invalid (${currentContact.id}). Coordinates not saved.")
                    savedStateHandle.remove<Double>("picked_latitude")
                    savedStateHandle.remove<Double>("picked_longitude")
                }
            }
        } else if (contact != null && (pickedLatState?.value != null || pickedLngState?.value != null)) {
            Log.d("ContactDetailScreen_Picker", "Picker Effect re-triggered due to contact change while lat/lng present. Lat: ${pickedLatState?.value}, Lng: ${pickedLngState?.value}. Attempting to process now.")
        }
    }

    LaunchedEffect(key1 = contactId) {
        isLoading = true
        geocodingErrorMessage = null
        Log.d("ContactDetailScreen_Loader", "Attempting to load contact details for ID: $contactId. isLoading = true.")

        try {
            val fetchedContact = viewModel.getContact(contactId)
            Log.d("ContactDetailScreen_Loader", "Fetched contact from ViewModel for ID $contactId: ${if(fetchedContact == null) "NULL" else "VALID (ID: ${fetchedContact.id})"}")
            contact = fetchedContact

            if (fetchedContact != null) {
                if (fetchedContact.latitude != null && fetchedContact.longitude != null) {
                    contactLatLng = LatLng(fetchedContact.latitude, fetchedContact.longitude)
                    Log.d("ContactDetailScreen_Loader", "Using existing coordinates from fetched contact: $contactLatLng")
                } else if (!fetchedContact.address.isNullOrBlank()) {
                    Log.d("ContactDetailScreen_Loader", "No existing coordinates. Attempting to geocode address: '${fetchedContact.address}'")
                    try {
                        val geocoder = Geocoder(context)
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocationName(fetchedContact.address, 1)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val location = addresses[0]
                            contactLatLng = LatLng(location.latitude, location.longitude)
                            Log.d("ContactDetailScreen_Loader", "Geocoding successful: $contactLatLng")
                        } else {
                            geocodingErrorMessage = "Dirección no encontrada por geocodificador."
                            contactLatLng = null
                            Log.w("ContactDetailScreen_Loader", "Geocoding failed: Address '${fetchedContact.address}' not found.")
                        }
                    } catch (e: IOException) {
                        geocodingErrorMessage = "Servicio de geocodificación no disponible."
                        contactLatLng = null
                        Log.e("ContactDetailScreen_Loader", "Geocoding IOException for address '${fetchedContact.address}'", e)
                    } catch (e: IllegalArgumentException) {
                        geocodingErrorMessage = "Argumento inválido para geocodificación (dirección malformada?)."
                        contactLatLng = null
                        Log.e("ContactDetailScreen_Loader", "Geocoding IllegalArgumentException for address '${fetchedContact.address}'", e)
                    }
                } else {
                    contactLatLng = null
                    geocodingErrorMessage = "No hay dirección ni coordenadas para mostrar en el mapa."
                    Log.d("ContactDetailScreen_Loader", "No coordinates or address available for contact ID $contactId.")
                }
            } else {
                contactLatLng = null
                Log.w("ContactDetailScreen_Loader", "Contact with ID $contactId was not found in the database.")
            }
        } catch (e: Exception) {
            Log.e("ContactDetailScreen_Loader", "An unexpected error occurred while loading contact details for ID $contactId", e)
            contact = null
            contactLatLng = null
            geocodingErrorMessage = "Error crítico al cargar detalles."
        } finally {
            isLoading = false
            Log.d("ContactDetailScreen_Loader", "Finished loading attempt for contact ID $contactId. isLoading = false. Contact is ${if(contact == null) "NULL" else "NOT NULL"}. contactLatLng is ${if(contactLatLng == null) "NULL" else "SET"}.")
        }
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
                            textAlign = TextAlign.End, // Manteniendo tu TextAlign original
                            style = MaterialTheme.typography.headlineSmall,
                            color = colorScheme.onBackground,
                            // modifier = Modifier.weight(1f) // Podrías necesitar ajustar esto si el título es largo y los iconos se desplazan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.back))
                    }
                },
                actions = {
                    // --- INICIO: IconButton para ver foto ---
                    IconButton(onClick = {
                        val currentContact = contact // Captura el estado actual
                        if (currentContact != null && !currentContact.imageUri.isNullOrBlank()
                            && // (1)
                            currentContact.imageUri != "null"
                            ) {
                            // Asumiendo que 'imageUri' es el campo en ContactEntity
                            val encodedImageUri = Uri.encode(currentContact.imageUri)
                            Log.d("PhotoDebug", "URI Original: '${currentContact.imageUri}'")
                            Log.d("PhotoDebug", "URI Codificada para Navegación: '$encodedImageUri'")
                            navController.navigate("photoViewScreen/$encodedImageUri")
                        } else {
                            val toastMessage = if (currentContact != null) {
                                context.getString(R.string.no_photo_for_contact_toast, currentContact.name)
                            } else {
                                context.getString(R.string.contact_not_loaded_toast)
                            }
                            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = stringResource(id = R.string.view_contact_photo_desc)
                        )
                    }
                    // --- FIN: IconButton para ver foto ---

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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando detalles del contacto...") // Puedes usar R.string.loading_contact_details
                }
            } else if (contact == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Contacto no encontrado.") // Puedes usar R.string.contact_not_found
                }
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

//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.spacedBy(16.dp),
//                        verticalAlignment = androidx . compose . ui . Alignment.Top // Alinea los DetailEntryStyled po
//                    ) {
//                        currentContact.phoneNumber?.takeIf { it.isNotBlank() }?.let {
//                            DetailEntryStyled(
//                                label = stringResource(id = R.string.phone_number_label),
//                                value = it,
//                                modifier = Modifier.weight(1f)
//                            )
//                        } ?: Spacer(Modifier.weight(1f))
//
//
//
//                        currentContact.territory?.toString()?.takeIf { it.isNotBlank() }?.let {
//                            DetailEntryStyled(
//                                label = stringResource(id = R.string.territory_label),
//                                value = it,
//                                modifier = Modifier.weight(1f)
//                            )
//                        } ?: Spacer(Modifier.weight(1f))
//                    }

                    // --- INICIO: Fila modificada para Teléfono y Territorio ---
                    Row(
                        modifier = Modifier.fillMaxWidth()
                        .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        //verticalAlignment = Alignment.Top // Manteniendo tu alineación original
                    ) {
                        currentContact.phoneNumber?.takeIf { it.isNotBlank() }?.let { phoneNumber ->
                            // Estado para controlar la visibilidad del menú de mensajes
                            var showMessageOptions by remember { mutableStateOf(false) }

                            DetailEntryStyled(
                                label = stringResource(id = R.string.phone_number_label),
                                value = phoneNumber,
                                modifier = Modifier.weight(0.8f) // Peso ajustado para teléfono
                                .fillMaxHeight(),
                                trailingIcons = {
                                    IconButton(onClick = {

                                        // Validar ANTES de intentar la acción
                                        if (!PhoneNumberHelper.isValidPhoneNumber(phoneNumber, defaultDeviceRegion)) {
                                            Toast.makeText(context, context.getString(R.string.invalid_phone_number_format), Toast.LENGTH_LONG).show()
                                            // Podrías retornar o no hacer nada si el número no es válido para marcar
                                            // Por ahora, intentaremos igual con el número formateado/limpio por getNumberForDialingOrSms
                                        }
                                        Log.d("DIAL_DEBUG", "Paso 1: Número original de la fuente: '$phoneNumber'")
                                        Log.d("DIAL_DEBUG", "Paso 2: Región por defecto a usar: '$defaultDeviceRegion''")

                                        val dialableNumber = PhoneNumberHelper.getNumberForDialingOrSms(phoneNumber, defaultDeviceRegion)
                                        Log.d("ContactDetailScreen", "Intentando marcar: $dialableNumber (original: $phoneNumber)")
                                        Log.d("DIAL_DEBUG", "Paso 3: Número devuelto por getNumberForDialingOrSms: '$dialableNumber'")

                                        if (dialableNumber.isNotBlank()) { // Buena práctica añadir esta comprobación
                                            val scheme = "tel"
                                            // Usar Uri.fromParts para asegurar que el '+' (si está presente en dialableNumber)
                                            // se maneje de forma que el marcador lo reconozca.
                                            val uri: Uri = Uri.fromParts(scheme, dialableNumber, null)

                                            Log.d("DIAL_DEBUG", "Paso 4 (Usando Uri.fromParts): URI.toString(): '${uri.toString()}'")

                                            val intent = Intent(Intent.ACTION_DIAL, uri)
                                            try {
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Log.e("ContactDetailScreen", "Error al intentar ACTION_DIAL para '$dialableNumber' (URI: '$uri')", e)
                                                Toast.makeText(context, context.getString(R.string.error_no_dialer_app), Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Log.w("DIAL_DEBUG", "dialableNumber está vacío, no se puede marcar.")
                                            Toast.makeText(context, "Número inválido para marcar.", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            Icons.Filled.Call,
                                            contentDescription = stringResource(id = R.string.action_call_desc) // Necesitarás este string
                                        )
                                    }
                                    Box { // <--- ENVOLVER IconButton EN UN Box PARA ANCLAR EL MENÚ
                                        IconButton(onClick = {
                                            showMessageOptions = true // <--- Mostrar el menú
                                        }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Chat, // O Icons.AutoMirrored.Filled.Chat si usas iconos automirrored
                                                contentDescription = stringResource(id = R.string.action_message_desc)
                                            )
                                        }

                                        // DropdownMenu para las opciones de mensaje
                                        DropdownMenu(
                                            expanded = showMessageOptions,
                                            onDismissRequest = { showMessageOptions = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("SMS") },
                                                onClick = {
                                                    showMessageOptions = false // Ocultar menú
                                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                                                    try {
                                                        context.startActivity(smsIntent)
                                                    } catch (e: Exception) {
                                                        Log.e("ContactDetailScreen", "Error al intentar ACTION_SENDTO (SMS) para $phoneNumber", e)
                                                        Toast.makeText(context, context.getString(R.string.error_no_messaging_app), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("WhatsApp") },
                                                onClick = {
                                                    showMessageOptions = false // Ocultar menú
                                                    // Formato de URI para WhatsApp: "https://wa.me/codigoPaisNumero"
                                                    // Necesitarás una forma de obtener/asumir el código de país si no está en phoneNumber
                                                    // Por ahora, asumiré que phoneNumber puede no tenerlo y WhatsApp podría manejarlo o no.
                                                    // Una solución más robusta implicaría limpiar/formatear phoneNumber.
                                                    val strippedPhoneNumber = phoneNumber.filter { it.isDigit() } // Quita no dígitos
                                                    val whatsappUri = Uri.parse("https://wa.me/$strippedPhoneNumber")
                                                    val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri)
                                                    // No es necesario whatsappIntent.setPackage("com.whatsapp") explícitamente,
                                                    // ACTION_VIEW con la URI correcta es más flexible si WhatsApp Business u otros están instalados.
                                                    // Sin embargo, si quieres forzar solo "com.whatsapp", puedes añadirlo.
                                                    // whatsappIntent.setPackage("com.whatsapp") // Descomentar si quieres forzar WhatsApp estándar

                                                    try {
                                                        context.startActivity(whatsappIntent)
                                                    } catch (e: Exception) {
                                                        Log.e("ContactDetailScreen", "Error al intentar abrir WhatsApp para $strippedPhoneNumber", e)
                                                        Toast.makeText(context, context.getString(R.string.error_whatsapp_not_installed), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            )
                        } ?: Spacer(Modifier.weight(0.8f).fillMaxHeight()) // Mantener el espacio si no hay teléfono

                        currentContact.territory?.toString()?.takeIf { it.isNotBlank() }?.let {
                            DetailEntryStyled(
                                label = stringResource(id = R.string.territory_label),
                                value = it,
                                modifier = Modifier.weight(0.2f)
                                    .fillMaxHeight()// Peso ajustado para territorio
                            )
                        } ?: Spacer(Modifier.weight(0.2f).fillMaxHeight()) // Mantener el espacio si no hay territorio
                    }
                    // --- FIN: Fila modificada para Teléfono y Territorio ---
                    Spacer(modifier = Modifier.height(16.dp))

                    currentContact.address?.takeIf { it.isNotBlank() }?.let {addressValue ->
                        DetailEntryStyled(
                            label = stringResource(id = R.string.address_label),
                            value = addressValue
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

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
                            label = stringResource(id = R.string.next_visit_label),
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
                    val defaultFallbackLocation = LatLng(41.159209110957576, -74.2551922625656)
                    Log.d("ContactDetail_Debug", "Valor de defaultFallbackLocation ANTES de rememberCameraPositionState: $defaultFallbackLocation")

                    val cameraPositionState = rememberCameraPositionState {
                        Log.d("ContactDetail_Debug", "INIT_CAM - Entrando a fromLatLngZoom. mapTargetLocation: $mapTargetLocation, defaultFallbackLocation: $defaultFallbackLocation")

                        position = CameraPosition.fromLatLngZoom(
                            mapTargetLocation ?: defaultFallbackLocation,
                            if (mapTargetLocation != null) 15f else 10f
                        )
                        Log.d("ContactDetail_Debug", "INIT_CAM - Posición ASIGNADA: ${position.target}, Zoom: ${position.zoom}")
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
                                    .background(colorScheme.errorContainer.copy(alpha = 0.3f))
                                    .border(
                                        1.dp,
                                        colorScheme.error,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = geocodingErrorMessage!!,
                                    textAlign = TextAlign.Center,
                                    color = colorScheme.onErrorContainer
                                )
                            }
                        }

                        currentContact.address.isNullOrBlank() && mapTargetLocation == null -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)), // Manteniendo tu clip original
                                cameraPositionState = cameraPositionState,
                                uiSettings = MapUiSettings(
                                    zoomControlsEnabled = true,
                                    scrollGesturesEnabled = true,
                                    zoomGesturesEnabled = true,
                                    tiltGesturesEnabled = true
                                )
                            ) {
                                val currentContext = LocalContext.current // Cambiado a currentContext para evitar shadowing
                                val statusComposeColor = VisitStatusColorUtil.getVisitStatusColor(
                                    nextVisitTimestamp = currentContact.nextVisitTimestamp
                                )
                                val labelTextColorInt = colorScheme.onSurfaceVariant.toArgb()
                                val labelBackgroundColorInt = colorScheme.surfaceVariant.toArgb()

                                val customIconWithLabel = remember(
                                    currentContact.id,
                                    currentContact.name,
                                    currentContact.lastName,
                                    statusComposeColor,
                                    labelTextColorInt,
                                    labelBackgroundColorInt
                                ) {
                                    try {
                                        createMarkerWithLabelBitmap(
                                            context = currentContext, // Usando currentContext
                                            name = currentContact.name,
                                            lastName = currentContact.lastName ?: "",
                                            markerIconResId = R.drawable.ic_map_pin,
                                            markerTintColor = statusComposeColor,
                                            labelTextColor = labelTextColorInt,
                                            labelBackgroundColor = labelBackgroundColorInt,
                                            iconWidth = 70,
                                            iconHeight = 70,
                                            labelOffsetY = 0
                                        )
                                    } catch (e: Exception) {
                                        Log.e("ContactDetailScreen", "Error creating custom marker bitmap", e)
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                    }
                                }

                                Marker(
                                    state = MarkerState(position = mapTargetLocation),
                                    icon = customIconWithLabel,
                                    anchor = Offset(0.5f, 0.95f)
                                )
                            }
                        }
                        else -> { // Fallback por si isLoading es true pero no hay error ni dirección, etc.
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.1f))
                                    .border(
                                        1.dp,
                                        Color.Gray.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                // Podrías tener un Text("Cargando mapa...") aquí si isLoadingMap es true
                                // o un Text(stringResource(id = R.string.map_unavailable)) si no hay datos para el mapa.
                                // Por ahora, mantendré el genérico.
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
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val labelHorizontalPadding = 6.dp
    val labelVerticalPadding = 1.dp
    val outlineColor = colorScheme.outline.copy(alpha = 0.8f)
    val cornerRadius = 8.dp
    val borderWidth = 1.dp
    val labelHeightEstimate = 18.dp
    val labelBackgroundColor = colorScheme.background
    val minEntryHeight = 52.dp // Prueba

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = labelHeightEstimate / 2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .defaultMinSize(minHeight = minEntryHeight)
                .border(
                    BorderStroke(borderWidth, outlineColor),
                    RoundedCornerShape(cornerRadius)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp), // Cambiado el padding vertical a 8.dp como en tu original
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                modifier = Modifier.weight(1f)
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary,
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
fun DetailEntryStyled(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null, // Se mantiene por ahora
    trailingIcons: (@Composable RowScope.() -> Unit)? = null, // NUEVO PARÁMETRO
) {
    val labelHorizontalPadding = 6.dp
    val labelVerticalPadding = 1.dp
    val outlineColor = colorScheme.outline.copy(alpha = 0.8f)
    val cornerRadius = 8.dp
    val borderWidth = 1.dp
    val labelHeightEstimate = 18.dp
    val labelBackgroundColor = colorScheme.background
    val minEntryHeight = 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = labelHeightEstimate / 2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minEntryHeight)
                .border(
                    BorderStroke(borderWidth, outlineColor),
                    RoundedCornerShape(cornerRadius)
                )
                // Ajustar padding para acomodar iconos si están presentes
                .padding(
                    start = 12.dp,
                    end = if (trailingIcons != null || trailingIcon != null) 4.dp else 12.dp, // Menos padding al final si hay iconos
                    top = 8.dp,
                    bottom = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                modifier = Modifier.weight(1f) // El texto toma el espacio disponible
            )

            // Lógica para mostrar los nuevos iconos múltiples primero
            if (trailingIcons != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(start = 8.dp) // Espacio entre el texto y los iconos
                ) {
                    trailingIcons() // Ejecuta el Composable que contiene los IconButton
                }
            }
            // Fallback al antiguo trailingIcon si el nuevo no se provee y el antiguo sí
            else if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary,
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
    val outlineColor = colorScheme.outline.copy(alpha = 0.8f)
    val cornerRadius = 8.dp
    val borderWidth = 1.dp
    val labelHeightEstimate = 18.dp
    val labelBackgroundColor = colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // Manteniendo tu padding original aquí
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
                .padding(horizontal = 12.dp, vertical = 12.dp) // Manteniendo tu padding original aquí
        ) {
            val scrollState = rememberScrollState()
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp), // Manteniendo tu estilo
                modifier = Modifier.verticalScroll(scrollState)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 14.sp),
            fontWeight = FontWeight.Medium,
            color = colorScheme.primary,
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


