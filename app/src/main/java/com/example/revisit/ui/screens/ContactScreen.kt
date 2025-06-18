package com.example.revisit.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload // Importado
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share // Importado
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider // Importado
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem // Importado
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet // Importado
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState // Importado
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.revisit.R
import com.example.revisit.data.db.ContactEntity
import com.example.revisit.ui.ContactViewModel
import com.example.revisit.ui.theme.VisitStatusAppColors
import com.example.revisit.ui.util.BackupUtils
import com.example.revisit.ui.util.DateTimeUtils
import com.example.revisit.ui.util.VisitStatusColorUtil
import kotlinx.coroutines.launch
import java.io.File // Para la función de compartir (revisar FileProvider)
import java.io.IOException // Para la función de compartir
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.roundToInt


@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

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
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    val deleteThreshold = with(density) { 120.dp.toPx() }

    var showDialog by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) } // Para evitar múltiples diálogos/deleciones

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp) // Añadido vertical padding para consistencia
    ) {
        // Fondo rojo para eliminar
        if (offsetX.value < 0) { // Solo muestra si se está deslizando hacia la izquierda
            val progress = kotlin.math.abs(offsetX.value) / deleteThreshold
            val alpha = kotlin.math.min(progress, 1f) // Limita alfa a 1

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(MaterialTheme.shapes.medium) // Usa la misma forma que el ContactItem
                    .background(Color.Red.copy(alpha = alpha * 0.8f)), // Ajusta la transparencia
                contentAlignment = Alignment.CenterEnd
            ) {
                if (progress > 0.3f) { // Muestra el ícono y texto solo después de cierto progreso
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.delete_contact),
                            tint = Color.White,
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

        // Contenido deslizable
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newOffset = offsetX.value + delta
                            // Permite deslizar solo hacia la izquierda (offsetX <= 0)
                            offsetX.snapTo(newOffset.coerceAtMost(0f))
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (!deleted && offsetX.value < -deleteThreshold) {
                                showDialog = true
                            } else {
                                offsetX.animateTo(0f) // Vuelve a la posición original si no se borra
                            }
                        }
                    }
                )
        ) {
            Box(modifier = Modifier.fillMaxWidth()) { // Contenedor para el ícono de chevron
                content() // Tu ContactItem

                // Ícono de "deslizar para eliminar"
                if (offsetX.value == 0f && !deleted) { // Muestra solo si no se ha deslizado y no se ha borrado
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = stringResource(id = R.string.swipe_to_delete),
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp) // Un poco de padding para que no esté pegado al borde
                    )
                }
            }
        }

        // Diálogo de confirmación
        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    scope.launch { offsetX.animateTo(0f) } // Vuelve si se cancela el diálogo
                },
                title = { Text(stringResource(id = R.string.confirm_deletion_title)) },
                text = { Text(stringResource(id = R.string.confirm_deletion_text)) },
                confirmButton = {
                    TextButton(onClick = {
                        onDelete()
                        deleted = true // Marca como borrado para evitar más interacciones
                        showDialog = false
                        // No necesitamos animar offsetX aquí, ya que el item desaparecerá
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
            verticalAlignment = Alignment.Bottom // O Alignment.CenterVertically si prefieres
        ) {
            OutlinedTextField(
                value = territoryFilter,
                onValueChange = onTerritoryChange,
                label = {
                    Text(
                        stringResource(id = R.string.contact_territory_label),
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = commonFieldHeight), // O .height(commonFieldHeight)
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )
            )

            Box(
                modifier = Modifier
                    .weight(1.5f)
                    .defaultMinSize(minHeight = commonFieldHeight) // O .height(commonFieldHeight)
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable(onClick = onNextVisitDateClick)
                    .padding(horizontal = 16.dp), // Padding interno para el contenido del Box
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
    paddingValues: PaddingValues, // Este padding viene del Scaffold
    viewModel: ContactViewModel,
    onContactClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val itemSpacing = 0.dp // El padding ya está en SwipeToDeleteWrapper

    Box(modifier = modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            Text(
                text = stringResource(id = R.string.no_contacts_found),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Aplica el padding del Scaffold
                    .wrapContentSize(Alignment.Center),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues), // Aplica el padding del Scaffold aquí también
                contentPadding = PaddingValues(bottom = 16.dp), // Padding adicional al final de la lista
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                items(contacts, key = { contact -> contact.id }) { contact ->
                    SwipeToDeleteWrapper(
                        onDelete = {
                            coroutineScope.launch {
                                viewModel.delete(contact)
                            }
                        }
                    ) {
                        ContactItem(
                            contact = contact,
                            onClick = { onContactClick(contact.id) }
                        )
                    }
                }
            }
        }

        // Indicador de "scroll para más"
        val currentCanScrollForward = listState.canScrollForward
        if (currentCanScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Considera el padding inferior del Scaffold y el contentPadding de LazyColumn
                    .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp + 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(id = R.string.scroll_for_more),
                    modifier = Modifier.size(40.dp), // Un poco más pequeño
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) // Con transparencia
                )
            }
        }
    }
}

@Composable
fun ContactItem(
    contact: ContactEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Clickable en toda la superficie
        shape = MaterialTheme.shapes.medium, // Coincide con el clip del SwipeToDeleteWrapper
        color = MaterialTheme.colorScheme.surface, // O background, según tu tema
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VisitStatusIndicator(
                statusColor = VisitStatusColorUtil.getVisitStatusColor(
                    nextVisitTimestamp = contact.nextVisitTimestamp
                )
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${contact.name} ${contact.lastName ?: ""}".trim(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        // fontSize = 18.sp // Ajusta si es necesario
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.territory_label)} ${contact.territory?.toString() ?: stringResource(R.string.not_available)}",
                        style = MaterialTheme.typography.bodyMedium,
                        // fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f) // Ajusta pesos para distribución
                    )
                    Spacer(Modifier.width(8.dp)) // Espacio entre territorio y fecha
                    Text(
                        text = "${stringResource(R.string.next_visit_label)} ${DateTimeUtils.formatDateTimeForDisplay(contact.nextVisitTimestamp)}",
                        style = MaterialTheme.typography.bodyMedium,
                        // fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.5f) // Ajusta pesos para distribución
                    )
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

    // Estados de filtros y UI
    val contactsByName by viewModel.allContactsSortedByName.collectAsState(initial = emptyList())
    var territoryFilter by remember { mutableStateOf("") }
    var nextVisitDateFilterTimestamp by remember { mutableStateOf<Long?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextVisitDateFilterTimestamp)

    val nextVisitDateDisplay = if (nextVisitDateFilterTimestamp != null) {
        remember(nextVisitDateFilterTimestamp) { DateTimeUtils.formatDateForDisplay(nextVisitDateFilterTimestamp) }
    } else {
        stringResource(id = R.string.next_visit_label_filter)
    }

    val filteredContacts by remember(contactsByName, territoryFilter, nextVisitDateFilterTimestamp) {
        derivedStateOf {
            // ... (lógica de filtrado sin cambios) ...
            if (territoryFilter.isBlank() && nextVisitDateFilterTimestamp == null) {
                contactsByName
            } else {
                contactsByName.filter { contact ->
                    val territoryMatches = if (territoryFilter.isNotBlank()) {
                        contact.territory?.toString()
                            ?.equals(territoryFilter, ignoreCase = true) == true
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

    // Estado para la hoja inferior de opciones de respaldo
    var showBackupOptionsBottomSheet by remember { mutableStateOf(false) }
    val backupOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)


    // Launcher para RESTAURAR
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    val contacts = BackupUtils.restoreFromJsonFile(context, it)
                    contacts?.let { viewModel.insertAllContacts(it) }
                }
            }
        }
    )

    // Launcher para GUARDAR ARCHIVO DE RESPALDO LOCALMENTE
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"), // Especifica el MIME type
        onResult = { uri: Uri? ->
            uri?.let { destinationUri ->
                coroutineScope.launch {
                    try {
                        val contacts = viewModel.getAllContacts() // Obtén los contactos a respaldar
                        val jsonString = BackupUtils.contactsToJson(contacts) // Asume que tienes esta función

                        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                            outputStream.write(jsonString.toByteArray())
                            Log.i("Backup", "Respaldo guardado localmente en: $destinationUri")
                            // Aquí podrías mostrar un Toast o Snackbar de confirmación
                        }
                    } catch (e: Exception) { // Captura excepciones más generales también
                        Log.e("Backup", "Error al guardar el respaldo localmente", e)
                        // Aquí podrías mostrar un Toast o Snackbar de error
                    }
                }
            }
        }
    )

    // Función para la opción de "Compartir"
    fun shareBackupFile() {
        coroutineScope.launch {
            try {
                val contacts = viewModel.getAllContacts()
                //  IMPORTANTE: Esta función debe usar FileProvider para compartir de forma segura.
                //  La implementación actual con Uri.fromFile(File(...)) es una solución temporal
                //  y NO se recomienda para producción.
                val uri = BackupUtils.backupToJsonFile(context, contacts)
                if (uri != null) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(
                        Intent.createChooser(shareIntent, context.getString(R.string.share_backup_file_title))
                    )
                } else {
                    Log.e("Backup", "Fallo al crear archivo temporal para compartir")
                    // Considera mostrar un Toast al usuario aquí
                }
            } catch (e: Exception) {
                Log.e("Backup", "Error durante compartir respaldo", e)
                // Considera mostrar un Toast al usuario aquí
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
                            val localDateUTC = Instant.ofEpochMilli(selectedMillisUTC).atZone(ZoneOffset.UTC).toLocalDate()
                            val startOfDayDeviceZone = localDateUTC.atStartOfDay(ZoneId.systemDefault())
                            nextVisitDateFilterTimestamp = startOfDayDeviceZone.toInstant().toEpochMilli()
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

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface // o onBackground
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            // Mostrar la hoja inferior con opciones de respaldo
                            showBackupOptionsBottomSheet = true
                        }) {
                            Icon(Icons.Default.Save, contentDescription = stringResource(R.string.backup_options_button_desc))
                        }
                        IconButton(onClick = {
                            restoreLauncher.launch(arrayOf("application/json"))
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.restore_button_desc))
                        }
                        IconButton(onClick = {
                            val contactIdsString = filteredContacts.joinToString(",") { it.id.toString() }
                            if (contactIdsString.isNotEmpty()) { // Solo muestra el mapa si hay contactos filtrados
                                onShowMapClick(contactIdsString)
                            } else {
                                // Opcional: Mostrar un Toast si no hay contactos para el mapa
                            }
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
                    onNextVisitDateClick = { showDatePickerDialog = true },
                    onClearFilters = {
                        territoryFilter = ""
                        nextVisitDateFilterTimestamp = null
                        datePickerState.selectedDateMillis = null // También resetea el estado del DatePicker
                    }
                )
            }
        }
    ) { paddingValuesFromScaffold ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValuesFromScaffold) // Aplica el padding del Scaffold aquí
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp), // Añadido padding horizontal
                horizontalArrangement = Arrangement.SpaceAround // O SpaceEvenly / Start
            ) {
                LegendItem(VisitStatusAppColors.Overdue, stringResource(R.string.legend_overdue))
                LegendItem(VisitStatusAppColors.Today, stringResource(R.string.legend_today))
                LegendItem(VisitStatusAppColors.DueSoon, stringResource(R.string.legend_due_soon))
                LegendItem(VisitStatusAppColors.DueFar, stringResource(R.string.legend_due_far))
            }

            ContactList(
                contacts = filteredContacts,
                viewModel = viewModel,
                onContactClick = onContactClick,
                paddingValues = PaddingValues(0.dp), // El padding del Scaffold ya se maneja arriba
                modifier = Modifier.weight(1f)
            )
        }
    }

    // Hoja inferior para las opciones de respaldo
    if (showBackupOptionsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBackupOptionsBottomSheet = false },
            sheetState = backupOptionsSheetState,
            // containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp) // Ejemplo de color
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) { // Padding vertical para todo el contenido
                Text(
                    stringResource(R.string.backup_options_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // Padding para el título
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_option_save_local)) },
                    leadingContent = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { backupOptionsSheetState.hide() }.invokeOnCompletion {
                            if (!backupOptionsSheetState.isVisible) { // Asegúrate de que esté oculta antes de actuar
                                showBackupOptionsBottomSheet = false
                                val timestamp = System.currentTimeMillis()
                                createDocumentLauncher.launch("contacts_backup_$timestamp.json")
                            }
                        }
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_option_share_file)) },
                    leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        coroutineScope.launch { backupOptionsSheetState.hide() }.invokeOnCompletion {
                            if (!backupOptionsSheetState.isVisible) {
                                showBackupOptionsBottomSheet = false
                                shareBackupFile()
                            }
                        }
                    }
                )
                Spacer(Modifier.height(8.dp)) // Espacio antes del botón de cancelar, si lo añades
            }
        }
    }
}

// Recuerda añadir estas strings a tu archivo strings.xml:
/*
<string name="backup_options_button_desc">Opciones de Respaldo</string>
<string name="restore_button_desc">Restaurar Respaldo</string>
<string name="backup_options_title">Opciones de Respaldo</string>
<string name="backup_option_save_local">Guardar en el dispositivo</string>
<string name="backup_option_share_file">Compartir archivo de respaldo</string>
<string name="share_backup_file_title">Compartir Archivo de Respaldo</string>
 */