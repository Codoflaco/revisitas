package com.example.revisit.ui.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.revisit.R
import com.example.revisit.data.db.ContactEntity
import com.example.revisit.ui.ContactViewModel
import com.example.revisit.ui.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

@SuppressLint("StringFormatInvalid")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    viewModel: ContactViewModel,
    onNavigateBack: () -> Unit,
    contactId: Int? = null
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var territoryString by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var profile by remember { mutableStateOf("") }

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
                profile = it.profile ?: ""
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

            currentContact = null // Asegurarse de que no haya un contacto actual
            initialDataLoaded = true
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
//                                try {
//                                    viewModel.delete(contactToDelete)
//                                    Toast.makeText(context, context.getString(R.string.contact_deleted_successfully, name), Toast.LENGTH_SHORT).show()
//                                    onNavigateBack()
//                                onNavigateBack()
//                                } catch (e: Exception) {
//                                    Log.e("DeleteContactError", "Error deleting contact: ${e.message}", e)
//                                    Toast.makeText(
//                                        context,
//                                        context.getString(R.string.error_deleting_contact, e.localizedMessage ?: context.getString(R.string.unknown_error)),
//                                        Toast.LENGTH_LONG
//                                    ).show()
//                                } finally {
//                                    isLoading = false
//                                }
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
            DatePicker(
                state = nextVisitDatePickerState,
                // Opcional: podrías querer limitar las fechas seleccionables
                // dateValidator = { utcTimeMillis ->
                //    // Por ejemplo, no permitir fechas pasadas para la próxima visita
                //    utcTimeMillis >= System.currentTimeMillis() - (24 * 60 * 60 * 1000) // Permitir hoy
                // }
            )
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
                                style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )

                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {

                    if (isLoading && contactId == null) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else if (!isLoading) {
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
                            val contactProfile = profile.trim().ifEmpty { null }

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
                                        profile = contactProfile,
                                        creationOrFirstVisitTimestamp = creationOrFirstVisitTimestamp,
                                        nextVisitTimestamp = nextVisitTimestampState,
                                        lastInteractionTimestamp = System.currentTimeMillis()
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
            if (isLoading && initialDataLoaded && contactId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (!initialDataLoaded && contactId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                        onValueChange = { address = it },
                        label = { Text(stringResource(id = R.string.contact_address_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) }
                    )
                    OutlinedTextField(
                        value = territoryString,
                        onValueChange = { territoryString = it.filter { char -> char.isDigit() } },
                        label = { Text(stringResource(id = R.string.contact_territory_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Map, contentDescription = null) }
                    )

                    Text(stringResource(id = R.string.creation_or_first_visit_label), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(creationDateTimeDisplay, style = MaterialTheme.typography.titleMedium)
//                        IconButton(onClick = { showCreationTimeDialog = true }) {
//                            Icon(Icons.Filled.EditCalendar, contentDescription = stringResource(id = R.string.edit_creation_time))
//                        }
                    }

                    Text(stringResource(id = R.string.next_visit_label), style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nextVisitDateTimeDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
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
//                    OutlinedTextField(
//                        value = profile,
//                        onValueChange = { profile = it },
//                        label = { Text(stringResource(id = R.string.contact_profile_label)) },
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .defaultMinSize(minHeight = 100.dp),
//                        leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null) }
//                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.wrapContentSize(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
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