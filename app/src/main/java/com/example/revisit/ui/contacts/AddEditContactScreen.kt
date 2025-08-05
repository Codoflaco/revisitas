package com.example.revisit.ui.contacts

import android.Manifest
import android.annotation.SuppressLint
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.revisit.R
import com.example.revisit.data.local.ContactEntity
import com.example.revisit.util.DateTimeUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@SuppressLint("StringFormatInvalid")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
@Composable
fun AddEditContactScreen(
    navController: NavController,
    viewModel: ContactViewModel,
    onNavigateBack: () -> Unit,
    contactId: Int? = null,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var territoryString by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Estado para la URI de la imagen. Se guarda como String en la entidad.
    var imageUriString by remember { mutableStateOf("") }
    // URI temporal para la foto tomada por la cámara antes de confirmación
    var tempCameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    // Controla la visibilidad del diálogo de confirmación de foto
    var showPhotoConfirmationDialog by remember { mutableStateOf(false) }


    // --- INICIO: Lógica para TOMAR FOTO CON CÁMARA ---
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // tempCameraPhotoUri ya tiene la URI de la foto tomada.
            // Mostrar diálogo de confirmación.
            showPhotoConfirmationDialog = true
        } else {
            // La toma de la foto fue cancelada o falló.
            tempCameraPhotoUri = null // Limpiar la URI temporal.
            Toast.makeText(context, context.getString(R.string.photo_capture_cancelled), Toast.LENGTH_SHORT).show()
        }
    }

    // Función para crear una URI para la foto de la cámara.
    fun createImageFileUri(): Uri? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return try {
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Asegúrate que coincida con tu AndroidManifest.xml
                imageFile
            )
        } catch (ex: IOException) {
            Log.e("AddEditContactScreen", "Error creando archivo de imagen", ex)
            Toast.makeText(context, context.getString(R.string.error_preparing_camera), Toast.LENGTH_SHORT).show()
            null
        }
    }

    // Permiso para la CÁMARA.
    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var showCameraPermissionRationaleDialog by remember { mutableStateOf(false) }

    if (showCameraPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionRationaleDialog = false },
            title = { Text(stringResource(id = R.string.camera_permission_required_title)) },
            text = { Text(stringResource(id = R.string.camera_permission_rationale)) },
            confirmButton = {
                TextButton(onClick = {
                    showCameraPermissionRationaleDialog = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text(stringResource(id = R.string.grant_permission))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionRationaleDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
    // --- FIN: Lógica para TOMAR FOTO CON CÁMARA ---


    // --- INICIO: Diálogo de Confirmación de Foto ---
    if (showPhotoConfirmationDialog && tempCameraPhotoUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPhotoConfirmationDialog = false
                tempCameraPhotoUri = null // Descartar si se cierra el diálogo sin confirmar.
            },
            title = { Text(stringResource(id = R.string.confirm_photo_title)) },
            text = {
                AsyncImage(
                    model = tempCameraPhotoUri,
                    contentDescription = stringResource(id = R.string.photo_preview_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Ajusta según necesites
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    imageUriString = tempCameraPhotoUri.toString() // Guardar la URI confirmada como String.
                    showPhotoConfirmationDialog = false
                    // tempCameraPhotoUri = null; // Opcional: limpiar tempCameraPhotoUri aquí.
                    Toast.makeText(context, context.getString(R.string.photo_selected), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(id = R.string.select_photo_button)) // "Seleccionar Foto"
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoConfirmationDialog = false
                    tempCameraPhotoUri = null // Descartar la foto.
                    Toast.makeText(context, context.getString(R.string.photo_discarded), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }
    // --- FIN: Diálogo de Confirmación de Foto ---


    // Estados para latitud y longitud
    var latitudeState by remember { mutableStateOf<Double?>(null) }
    var longitudeState by remember { mutableStateOf<Double?>(null) }

    var creationOrFirstVisitTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val creationTimePickerState = rememberTimePickerState(
        initialHour = DateTimeUtils.getHourDevice(creationOrFirstVisitTimestamp),
        initialMinute = DateTimeUtils.getMinuteDevice(creationOrFirstVisitTimestamp),
        is24Hour = DateTimeUtils.isSystem24Hour(context)
    )
    var showCreationTimeDialog by remember { mutableStateOf(false) }

    var nextVisitTimestampState by remember { mutableLongStateOf(DateTimeUtils.getDefaultNextVisitDateTime()) }
    val nextVisitDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = nextVisitTimestampState
    )
    var showNextVisitDateDialog by remember { mutableStateOf(false) }

    val nextVisitTimePickerState = rememberTimePickerState(
        initialHour = DateTimeUtils.getHourDevice(nextVisitTimestampState),
        initialMinute = DateTimeUtils.getMinuteDevice(nextVisitTimestampState),
        is24Hour = DateTimeUtils.isSystem24Hour(context)
    )
    var showNextVisitTimeDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var initialDataLoaded by remember { mutableStateOf(false) }
    var currentContact by remember { mutableStateOf<ContactEntity?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val creationDateTimeDisplay by remember(creationOrFirstVisitTimestamp) {
        derivedStateOf { DateTimeUtils.formatDateTimeForDisplay(creationOrFirstVisitTimestamp) }
    }
    val nextVisitDateTimeDisplay by remember(nextVisitTimestampState) {
        derivedStateOf { DateTimeUtils.formatDateTimeForDisplay(nextVisitTimestampState) }
    }

    // --- Carga inicial del contacto ---
    LaunchedEffect(key1 = contactId, key2 = initialDataLoaded) {
        if (contactId != null && !initialDataLoaded) {
            isLoading = true
            val contact = viewModel.getContact(contactId)
            currentContact = contact
            contact?.let {
                name = it.name
                lastName = it.lastName ?: ""
                phoneNumber = it.phoneNumber ?: ""
                address = it.address ?: ""
                territoryString = it.territory?.toString() ?: ""
                notes = it.notes ?: ""
                imageUriString = it.imageUri ?: "" // Cargar URI de imagen como String
                latitudeState = it.latitude
                longitudeState = it.longitude
                creationOrFirstVisitTimestamp = it.creationOrFirstVisitTimestamp
                creationTimePickerState.hour = DateTimeUtils.getHourDevice(it.creationOrFirstVisitTimestamp)
                creationTimePickerState.minute = DateTimeUtils.getMinuteDevice(it.creationOrFirstVisitTimestamp)
                nextVisitTimestampState = it.nextVisitTimestamp
                nextVisitDatePickerState.selectedDateMillis = it.nextVisitTimestamp
                nextVisitTimePickerState.hour = DateTimeUtils.getHourDevice(it.nextVisitTimestamp)
                nextVisitTimePickerState.minute = DateTimeUtils.getMinuteDevice(it.nextVisitTimestamp)
            }
            initialDataLoaded = true
            isLoading = false
        } else if (contactId == null && !initialDataLoaded) {
            val now = System.currentTimeMillis()
            creationOrFirstVisitTimestamp = now
            creationTimePickerState.hour = DateTimeUtils.getHourDevice(now)
            creationTimePickerState.minute = DateTimeUtils.getMinuteDevice(now)

            val defaultNext = DateTimeUtils.getDefaultNextVisitDateTime()
            nextVisitTimestampState = defaultNext
            nextVisitDatePickerState.selectedDateMillis = defaultNext
            nextVisitTimePickerState.hour = DateTimeUtils.getHourDevice(defaultNext)
            nextVisitTimePickerState.minute = DateTimeUtils.getMinuteDevice(defaultNext)

            currentContact = null
            imageUriString = "" // Limpiar la URI de imagen para nuevo contacto
            initialDataLoaded = true
        }
    }

    // --- Lógica para recibir resultados del MapPickerScreen ---
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val pickedLatState = savedStateHandle?.getLiveData<Double>("picked_latitude")?.observeAsState()
    val pickedLngState = savedStateHandle?.getLiveData<Double>("picked_longitude")?.observeAsState()

    LaunchedEffect(pickedLatState?.value, pickedLngState?.value) {
        val lat = pickedLatState?.value
        val lng = pickedLngState?.value

        if (lat != null && lng != null) {
            latitudeState = lat
            longitudeState = lng
            isLoading = true

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses: List<Address>? = geocoder.getFromLocation(lat, lng, 1)

                    if (addresses != null && addresses.isNotEmpty()) {
                        val fetchedAddress = addresses[0]
                        val fullAddress =
                            (0..fetchedAddress.maxAddressLineIndex).joinToString(separator = ", ") {
                                fetchedAddress.getAddressLine(it)
                            }
                        withContext(Dispatchers.Main) {
                            address = fullAddress
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, R.string.no_address_found_for_location, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: IOException) {
                    Log.e("AddEditContactScreen", "Geocoder IOException", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.geocoding_service_unavailable, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("AddEditContactScreen", "Geocoder IllegalArgumentException", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, R.string.invalid_lat_lng_for_geocoding, Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        savedStateHandle.remove<Double>("picked_latitude")
                        savedStateHandle.remove<Double>("picked_longitude")
                    }
                }
            }
        }
    }

    // --- DIÁLOGO DE CONFIRMACIÓN DE ELIMINACIÓN ---
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(stringResource(id = R.string.confirm_deletion_title)) },
            text = { Text(stringResource(id = R.string.confirm_deletion_message, name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        currentContact?.let { contactToDelete ->
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    viewModel.delete(contactToDelete) // Asumiendo que viewModel.delete existe y funciona
                                    Toast.makeText(context, context.getString(R.string.contact_deleted_successfully, name), Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    Log.e("DeleteContactError", "Error deleting contact: ${e.message}", e)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_deleting_contact, e.localizedMessage ?: context.getString(R.string.unknown_error)),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false }
                ) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
    }

    if (showCreationTimeDialog) {
        TimePickerDialog(
            title = stringResource(id = R.string.select_creation_time_dialog_title),
            state = creationTimePickerState,
            onDismiss = { showCreationTimeDialog = false },
            onConfirm = {
                creationOrFirstVisitTimestamp = DateTimeUtils.combineDateAndTime(
                    creationOrFirstVisitTimestamp,
                    creationTimePickerState.hour,
                    creationTimePickerState.minute
                )
                showCreationTimeDialog = false
            }
        )
    }

    if (showNextVisitDateDialog) {
        DatePickerDialog(
            onDismissRequest = { showNextVisitDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showNextVisitDateDialog = false
                    nextVisitDatePickerState.selectedDateMillis?.let { selectedMillisUTC ->
                        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                            timeInMillis = selectedMillisUTC
                        }
                        val localCalendar = Calendar.getInstance().apply {
                            set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH))
                        }
                        val hour = DateTimeUtils.getHourDevice(nextVisitTimestampState)
                        val minute = DateTimeUtils.getMinuteDevice(nextVisitTimestampState)
                        localCalendar.set(Calendar.HOUR_OF_DAY, hour)
                        localCalendar.set(Calendar.MINUTE, minute)
                        nextVisitTimestampState = localCalendar.timeInMillis
                    }
                }) { Text(stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showNextVisitDateDialog = false }) {
                    Text(stringResource(id = R.string.cancel))
                }
            }
        )
        {
            DatePicker(state = nextVisitDatePickerState)
        }
    }

    if (showNextVisitTimeDialog) {
        TimePickerDialog(
            title = stringResource(id = R.string.select_next_visit_time_dialog_title),
            state = nextVisitTimePickerState,
            onDismiss = { showNextVisitTimeDialog = false },
            onConfirm = {
                nextVisitTimestampState = DateTimeUtils.combineDateAndTime(
                    nextVisitTimestampState,
                    nextVisitTimePickerState.hour,
                    nextVisitTimePickerState.minute
                )
                showNextVisitTimeDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (contactId == null) stringResource(id = R.string.add_new_contact)
                        else stringResource(id = R.string.edit_contact),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, R.string.name_cannot_be_empty, Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            val territory = territoryString.toIntOrNull()
                            val contactName = name.trim()
                            val contactLastName = lastName.trim().ifEmpty { null }
                            val contactPhoneNumber = phoneNumber.trim().ifEmpty { null }
                            val contactAddress = address.trim().ifEmpty { null }
                            val contactTerritory = territory
                            val contactNotes = notes.trim().ifEmpty { null }
                            // Usar imageUriString aquí
                            val contactImageUri = imageUriString.trim().ifEmpty { null }

                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    viewModel.saveOrUpdateContact(
                                        id = contactId,
                                        name = contactName,
                                        lastName = contactLastName,
                                        phoneNumber = contactPhoneNumber,
                                        address = contactAddress,
                                        territory = contactTerritory,
                                        notes = contactNotes,
                                        imageUri = contactImageUri, // Pasa el String
                                        creationOrFirstVisitTimestamp = creationOrFirstVisitTimestamp,
                                        nextVisitTimestamp = nextVisitTimestampState,
                                        pickedLatitude = latitudeState, // Guardar latitud
                                        pickedLongitude = longitudeState, // Guardar longitud
                                        lastInteractionTimestamp = System.currentTimeMillis() // Asumo que esto se sigue necesitando
                                    )
                                    val message = if (contactId == null) {
                                        context.getString(R.string.contact_created_successfully, contactName)
                                    } else {
                                        context.getString(R.string.contact_updated_successfully, contactName)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.error_saving_contact, e.localizedMessage ?: context.getString(R.string.unknown_error)),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    isLoading = false
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(id = R.string.save_contact))
                        }
                    }

                    if (contactId != null && !isLoading) {
                        IconButton(onClick = {
                            showDeleteConfirmationDialog = true
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(id = R.string.delete_contact))
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            if (isLoading && initialDataLoaded && contactId != null) { // Condición original
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (!initialDataLoaded && contactId != null) { // Condición original
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // La sección de imagen superior ha sido eliminada.

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(id = R.string.contact_name_label) + "*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text(stringResource(id = R.string.contact_lastname_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.PersonOutline, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text(stringResource(id = R.string.contact_phone_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = {
                            address = it
                            if (latitudeState != null || longitudeState != null) {
                                latitudeState = null
                                longitudeState = null
                                Toast.makeText(context, R.string.map_location_cleared_due_to_address_change, Toast.LENGTH_SHORT).show()
                            }
                        },
                        label = { Text(stringResource(id = R.string.contact_address_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    var route = "mapPicker"
                                    if (latitudeState != null && longitudeState != null) {
                                        route += "?initialLat=${latitudeState}&initialLng=${longitudeState}"
                                    } else if (currentContact?.latitude != null && currentContact?.longitude != null) {
                                        route += "?initialLat=${currentContact?.latitude}&initialLng=${currentContact?.longitude}"
                                    }
                                    navController.navigate(route)
                                }) {
                                    Icon(
                                        Icons.Filled.Map,
                                        contentDescription = stringResource(id = R.string.pick_location_on_map)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = {
                                    if (cameraPermissionState.status.isGranted) {
                                        val uri = createImageFileUri()
                                        if (uri != null) {
                                            tempCameraPhotoUri = uri // Guardar la URI temporalmente
                                            cameraLauncher.launch(uri)
                                        }
                                    } else if (cameraPermissionState.status.shouldShowRationale) {
                                        showCameraPermissionRationaleDialog = true
                                    } else {
                                        cameraPermissionState.launchPermissionRequest()
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt, // O AddAPhoto
                                        contentDescription = stringResource(id = R.string.take_photo_desc)
                                    )
                                }
                            }
                        }
                    )
                    OutlinedTextField(
                        value = territoryString,
                        onValueChange = { territoryString = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(id = R.string.contact_territory_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) } // Considerar cambiar este ícono si es confuso con el de dirección
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(id = R.string.creation_or_first_visit_label),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = creationDateTimeDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.next_visit_label), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = nextVisitDateTimeDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { showNextVisitDateDialog = true }) {
                            Icon(Icons.Filled.DateRange, contentDescription = stringResource(id = R.string.edit_next_visit_date))
                        }
                        IconButton(onClick = { showNextVisitTimeDialog = true }) {
                            Icon(Icons.Filled.AccessTime, contentDescription = stringResource(id = R.string.edit_next_visit_time))
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(stringResource(id = R.string.contact_notes_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 100.dp),
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) }
                    )

                    // Mostrar la imagen seleccionada/tomada (opcional, pero útil para el usuario)
                    if (imageUriString.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(id = R.string.contact_photo_label), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUriString, // Cargar desde el String de la URI
                            contentDescription = stringResource(id = R.string.contact_photo_desc),
                            modifier = Modifier
                                .size(150.dp) // Puedes ajustar este tamaño
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    // Opcional: Permitir cambiar la foto o verla más grande
                                    // Podrías abrir un diálogo que muestre la imagen y un botón para "Cambiar" o "Eliminar" foto.
                                    // Para eliminar: imageUriString = ""
                                    // Para cambiar: re-lanzar el selector o la cámara.
                                },
                            contentScale = ContentScale.Crop
                        )
                        // Botón para eliminar la foto actual
                        TextButton(onClick = { imageUriString = "" }) {
                            Text(stringResource(id = R.string.remove_photo_button))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog( // Tu Composable original
    title: String,
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.wrapContentSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(id = R.string.ok))
                    }
                }
            }
        }
    }
}