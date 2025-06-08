package com.example.revisit.ui.screens

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.revisit.data.db.ContactEntity
import com.example.revisit.ui.ContactViewModel
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.setValue
import com.example.revisit.ui.util.DateTimeUtils
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.unit.Dp
import com.example.revisit.ui.util.VisitStatusColorUtil
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.revisit.R
import com.example.revisit.ui.util.BackupUtils
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset



@Composable
fun VisitStatusIndicator(statusColor: Color, size: Dp = 12.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = statusColor, shape = MaterialTheme.shapes.small)
    )
}
@Composable
fun SwipeToDeleteWrapper(
    //contact: ContactEntity,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val deleteThreshold = with(density) { 120.dp.toPx() }

    var showDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (offsetX.value < 0) {
            val progress = kotlin.math.abs(offsetX.value) / deleteThreshold
            val alpha = kotlin.math.min(progress, 1f)

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Red.copy(alpha = alpha * 0.8f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (progress > 0.3f) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_contact),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.delete),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newOffset = offsetX.value + delta
                            offsetX.snapTo(newOffset.coerceAtMost(0f))
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (!deleted && offsetX.value < -deleteThreshold) {
                                showDialog = true
                            } else {
                                offsetX.animateTo(0f)
                            }
                        }
                    }
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                content()

                if (offsetX.value == 0f) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(id = R.string.swipe_to_delete),
                        tint = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 0.dp)
                    )
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    scope.launch { offsetX.animateTo(0f) }
                },
                title = { Text(stringResource(id = R.string.confirm_deletion_title)) },
                text = { Text( stringResource(id = R.string.confirm_deletion_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        deleted = true
                        showDialog = false
                        scope.launch { offsetX.snapTo(0f) }
                    }) {
                        Text(stringResource(id = R.string.delete), color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDialog = false
                        scope.launch { offsetX.animateTo(0f) }
                    }) {
                        Text(stringResource(id = R.string.cancel))
                    }
                }
            )
        }
    }
}


@Composable
fun FilterControls(
    territoryFilter: String,
    onTerritoryChange: (String) -> Unit,
    nextVisitDateFilterDisplay: String,
    onNextVisitDateClick: () -> Unit,
    onClearFilters: () -> Unit
) {
    val commonFieldHeight = 56.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.filter_contacts_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = territoryFilter,
                onValueChange = onTerritoryChange,
                label = {
                    Text(
                        stringResource(id = R.string.territory_label),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = commonFieldHeight),
                singleLine = true,
                shape = MaterialTheme.shapes.small,

            )

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .defaultMinSize(minHeight = commonFieldHeight)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable(onClick = onNextVisitDateClick)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (nextVisitDateFilterDisplay.isNotBlank()) nextVisitDateFilterDisplay else stringResource(id = R.string.next_visit_label),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (nextVisitDateFilterDisplay.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = stringResource(id = R.string.select_date_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onClearFilters) {
                Icon(Icons.Filled.Close, stringResource(id = R.string.clear_filters))
            }
        }
    }
}

@Composable
fun ContactList(
    contacts: List<ContactEntity>,
    paddingValues: PaddingValues,
    viewModel: ContactViewModel,
    onContactClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val itemSpacing = 4.dp


    Box(modifier = modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_contacts_found),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .wrapContentSize(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 0.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(contacts, key = { contact -> contact.id }) { contact ->
                    SwipeToDeleteWrapper(
                        //contact = contact,
                        onDelete = {
                            coroutineScope.launch {
                                viewModel.delete(contact)
                            }
                        }
                    ) {
                        ContactItem(
                            contact = contact,
                            onClick = { onContactClick(contact.id) },
//                            onDelete = {
//                                coroutineScope.launch {
//                                    viewModel.delete(contact)
//                                }
//                            }
                        )
                    }
                }
            }
        }

        val currentCanScrollForward = listState.canScrollForward
        if (currentCanScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(id = R.string.scroll_for_more),
                    modifier = Modifier
                        .size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: ContactEntity,
    onClick: () -> Unit,
    //onDelete: () -> Unit
) {
    val itemShape = RoundedCornerShape(4.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.background,
        shadowElevation =1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(itemShape)
                .border(
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    ),
                    shape = MaterialTheme.shapes.medium,
                )
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VisitStatusIndicator(
                    statusColor = VisitStatusColorUtil.getVisitStatusColor(
                        nextVisitTimestamp = contact.nextVisitTimestamp,
                        referenceStartDateForNextVisit = contact.nextVisitLastSetTimestamp
                    )
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        color = MaterialTheme.colorScheme.onSurface,

                                text = "${contact.name} ${contact.lastName ?: ""}".trim(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = "${stringResource(R.string.territory_label)} ${contact.territory?.toString() ?: stringResource(R.string.not_available)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp
                        )
                        Text(
                            modifier = Modifier.weight(2f),
                            text ="${stringResource(R.string.next_visit_label)} ${DateTimeUtils.formatDateTimeForDisplay(
                                contact.nextVisitTimestamp
                            )}",
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactScreen(
    viewModel: ContactViewModel,
    onAddContact: () -> Unit,
    onContactClick: (Int) -> Unit,
    onShowMapClick: (contactIds: String) -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val contactsByName by viewModel.allContactsSortedByName.collectAsState(initial = emptyList())
    var territoryFilter by remember { mutableStateOf("") }
    var nextVisitDateFilterTimestamp by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = nextVisitDateFilterTimestamp
    )
    val nextVisitDateDisplay = if (nextVisitDateFilterTimestamp != null) {
        remember(nextVisitDateFilterTimestamp) {
            DateTimeUtils.formatDateForDisplay(nextVisitDateFilterTimestamp)
        }
    } else {
        stringResource(id = R.string.next_visit_label_filter)
    }

    val filteredContacts by remember(
        contactsByName,
        territoryFilter,
        nextVisitDateFilterTimestamp
    ) {
        derivedStateOf {
            if (territoryFilter.isBlank() && nextVisitDateFilterTimestamp == null) {
                contactsByName
            } else {
                contactsByName.filter { contact ->
                    val territoryMatches = if (territoryFilter.isNotBlank()) {
                        contact.territory?.toString()
                            ?.contains(territoryFilter, ignoreCase = true) == true
                    } else {
                        true
                    }

                    val dateMatches = if (nextVisitDateFilterTimestamp != null) {
                        contact.nextVisitTimestamp.let { visitTime ->
                            val filterStartOfDay =
                                DateTimeUtils.getStartOfDayUTCTimestamp(nextVisitDateFilterTimestamp!!)
                            val filterEndOfDay =
                                DateTimeUtils.getEndOfDayUTCTimestamp(nextVisitDateFilterTimestamp!!)
                            visitTime in filterStartOfDay..filterEndOfDay
                        }
                    } else {
                        true
                    }
                    territoryMatches && dateMatches
                }
            }
        }
    }



    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDatePickerDialog = false
                        datePickerState.selectedDateMillis?.let { selectedMillisUTC ->

                            val localDateUTC = Instant.ofEpochMilli(selectedMillisUTC)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val startOfDayDeviceZone = localDateUTC.atStartOfDay(ZoneId.systemDefault())
                            val correctedTimestamp = startOfDayDeviceZone.toInstant().toEpochMilli()

                            nextVisitDateFilterTimestamp = correctedTimestamp
                        }
                    }
                ) { Text(stringResource(id = R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text(stringResource(id = R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    val contacts = BackupUtils.restoreFromJsonFile(context, it)
                    contacts?.let {

                        viewModel.insertAllContacts(it)
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    actions = {

                        IconButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    val contacts = viewModel.getAllContacts()
                                    val uri = BackupUtils.backupToJsonFile(context, contacts)
                                    if (uri != null) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(
                                                shareIntent,
                                                "Share Backup File"
                                            )
                                        )
                                    } else {
                                        Log.e("Backup", "Failed to create backup file")
                                    }
                                } catch (e: Exception) {
                                    Log.e("Backup", "Error during backup", e)
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Backup")
                        }

                        IconButton(onClick = {
                            launcher.launch(arrayOf("application/json"))
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restore")
                        }



                        IconButton(onClick = {
                  val contactIds = filteredContacts.map { it.id }.joinToString(",")
                   onShowMapClick(contactIds)
               }) {
                            Icon(Icons.Filled.Map, contentDescription = stringResource(id = R.string.show_map))
                        }

                        IconButton(onClick = onAddContact) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(id = R.string.add_new_contact))
                        }
                    }
                )
                FilterControls(
                    territoryFilter = territoryFilter,
                    onTerritoryChange = { territoryFilter = it },
                    nextVisitDateFilterDisplay = nextVisitDateDisplay,
                    onNextVisitDateClick = {
                        showDatePickerDialog = true
                    },
                    onClearFilters = {
                        territoryFilter = ""
                        nextVisitDateFilterTimestamp = null
                        datePickerState.selectedDateMillis = null
                    }
                )
            }
        }
    ) { paddingValuesFromScaffold ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromScaffold)
        ) {
            ContactList(
                contacts = filteredContacts,
                viewModel = viewModel,
                onContactClick = onContactClick,
                paddingValues = PaddingValues(0.dp),
                modifier = Modifier.weight(1f)
            )

        }
    }
}
