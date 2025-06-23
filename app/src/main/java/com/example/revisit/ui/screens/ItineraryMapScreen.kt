package com.example.revisit.ui.screens

import android.content.Context
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
import com.google.maps.android.compose.Marker // Asegúrate que es esta importación
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.material3.MaterialTheme
import com.example.revisit.ui.util.VisitStatusColorUtil
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp
import android.graphics.Color as AndroidColor
import androidx.compose.ui.geometry.Offset
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import androidx.compose.ui.platform.LocalContext
import android.graphics.Paint
import android.graphics.Typeface

// Asegúrate que los imports al inicio del archivo sean correctos (ver arriba)

fun createMarkerWithLabelBitmap(
    context: Context,
    name: String,
    lastName: String,
    @DrawableRes markerIconResId: Int,
    markerTintColor: androidx.compose.ui.graphics.Color, // Este es Color de Compose, está bien
    labelTextColor: Int, // Este es un Int ARGB (android.graphics.Color), está bien
    labelBackgroundColor: Int, // Este es un Int ARGB (android.graphics.Color), está bien
    labelPadding: Int = 16,
    labelCornerRadius: Float = 20f,
    iconWidth: Int = 80,
    iconHeight: Int = 80,
    fontSizeSp: Float = 14f,
    // maxLabelWidthRatio: Float = 0.8f, // No lo estás usando, puedes quitarlo
    labelOffsetY: Int = 10
): BitmapDescriptor {

    // --- 1. Configuración de la pintura para la etiqueta ---
    val density = context.resources.displayMetrics.density
    val scaledFontSize = fontSizeSp * density

    // USA android.graphics.Paint
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { // Paint.ANTI_ALIAS_FLAG es de android.graphics.Paint
        color = labelTextColor // labelTextColor es un Int, correcto para android.graphics.Paint
        textSize = scaledFontSize // textSize es una propiedad de android.graphics.Paint
        textAlign = Paint.Align.CENTER // Paint.Align es de android.graphics.Paint
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // Typeface es de android.graphics
    }

    val fullName = "$name ${lastName.firstOrNull()?.let { "$it." } ?: ""}".trim()
    // USA android.graphics.Rect
    val textBounds = Rect() // Crea un android.graphics.Rect vacío
    textPaint.getTextBounds(fullName, 0, fullName.length, textBounds) // Correcto para android.graphics.Paint y android.graphics.Rect

    // --- 2. Calcular dimensiones de la etiqueta y del bitmap total ---
    // Los métodos width() y height() son de android.graphics.Rect
    val labelContentWidth = textBounds.width()
    val labelContentHeight = textBounds.height()

    val labelWidth = labelContentWidth + (2 * labelPadding) // Asegúrate que labelPadding no sea float si esperas Int
    val labelHeight = labelContentHeight + (2 * labelPadding)

    val totalBitmapWidth = labelWidth.coerceAtLeast(iconWidth)
    val totalBitmapHeight = labelHeight + iconHeight - labelOffsetY

    // Bitmap y Canvas son de android.graphics, correcto
    val finalBitmap = Bitmap.createBitmap(totalBitmapWidth, totalBitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(finalBitmap)

    // --- 3. Dibujar el fondo de la etiqueta ---
    // USA android.graphics.Paint
    val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = labelBackgroundColor // labelBackgroundColor es un Int, correcto para android.graphics.Paint
        style = Paint.Style.FILL // Paint.Style es de android.graphics.Paint
    }
    val labelLeft = (totalBitmapWidth - labelWidth) / 2f
    val labelTop = 0f
    // canvas.drawRoundRect espera android.graphics.Paint
    canvas.drawRoundRect(
        labelLeft,
        labelTop,
        labelLeft + labelWidth,
        labelTop + labelHeight,
        labelCornerRadius,
        labelCornerRadius,
        backgroundPaint // Debe ser android.graphics.Paint
    )

    val textX = labelLeft + labelWidth / 2f

    val textBaselineY = labelTop + (labelHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

    canvas.drawText(fullName, textX, textBaselineY, textPaint) // Debe ser android.graphics.Paint

    // --- 5. Dibujar el icono del marcador ---
    val markerIconDrawable = ContextCompat.getDrawable(context, markerIconResId)
    markerIconDrawable?.let {
        // markerTintColor es androidx.compose.ui.graphics.Color, necesitamos convertirlo a Int ARGB
        it.setTint(markerTintColor.toArgb())
        val iconActualX = (totalBitmapWidth - iconWidth) / 2f
        val iconActualY = (labelHeight - labelOffsetY).toFloat()

        it.setBounds(
            iconActualX.toInt(),
            iconActualY.toInt(),
            (iconActualX + iconWidth).toInt(),
            (iconActualY + iconHeight).toInt()
        )
        it.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(finalBitmap)
}

//@Composable
//internal fun mapComposeColorToHue(composeColor: Color): Float {
//
//    MaterialTheme.colorScheme.tertiary
//    MaterialTheme.colorScheme.error
//    MaterialTheme.colorScheme.primary
//
//
//    return when (composeColor) {
//        VisitStatusAppColors.DueSoon -> BitmapDescriptorFactory.HUE_YELLOW // Amarillo
//        VisitStatusAppColors.Overdue -> BitmapDescriptorFactory.HUE_RED      // Rojo
//        VisitStatusAppColors.Today -> BitmapDescriptorFactory.HUE_ORANGE   // Naranja
//        VisitStatusAppColors.DueFar -> BitmapDescriptorFactory.HUE_GREEN     // Verde
//        VisitStatusAppColors.ColorNoDate -> BitmapDescriptorFactory.HUE_AZURE // O el HUE que prefieras para "sin fecha"
//        else -> {
//            BitmapDescriptorFactory.HUE_CYAN
//        }
//    }
//}

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
            tempContacts = viewModel.allContactsSortedByName.value
        }

        contactsToShowOnMap = tempContacts.filter { it.latitude != null && it.longitude != null }
        Log.d("MapDebug", "Primer LE: Contactos filtrados para el mapa: ${contactsToShowOnMap.size}")

        if (tempContacts.isNotEmpty() && tempContacts.size != contactsToShowOnMap.size) {
            Log.d("MapDebug", "Primer LE: Algunos contactos no tienen dirección.")
        }
        isLoading = false
        Log.d("MapDebug", "Primer LE: Carga de contactos finalizada. isLoading = false")
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (contactsToShowOnMap.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.no_contacts_to_display_on_map),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.contact_verification_and_valid_address),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        mapToolbarEnabled = true
                        // Podrías considerar añadir myLocationButtonEnabled = true si tienes permisos y lo deseas
                    ),
                    properties = MapProperties(
                        // isMyLocationEnabled = true // Añade esto si quieres el botón de "mi ubicación" y tienes permisos
                    ),
                    onMapLoaded = {
                        isMapLoaded = true
                        Log.d("MapDebug", "GoogleMap onMapLoaded CALLED.") // Buen log para confirmar
                    }
                ) {
                    val currentContext = LocalContext.current // Obtener contexto para createMarkerWithLabelBitmap

                    contactsToShowOnMap.forEach { contact ->

                        val statusComposeColor = VisitStatusColorUtil.getVisitStatusColor(
                            nextVisitTimestamp = contact.nextVisitTimestamp
                        )

                        // Crear el icono con etiqueta usando la nueva función
                        val customIconWithLabel = remember(contact.id, contact.name, contact.lastName, statusComposeColor) {
                            Log.d("MarkerBitmap", "Creando/obteniendo bitmap para: ${contact.name} (ID: ${contact.id})")
                            try {
                                createMarkerWithLabelBitmap(
                                    context = currentContext,
                                    name = contact.name,
                                    lastName = contact.lastName ?: "",
                                    markerIconResId = R.drawable.ic_map_pin, // TU ICONO VECTORIAL
                                    markerTintColor = statusComposeColor,    // El Color de Compose
                                    labelTextColor = AndroidColor.BLACK,     // Color de Android
                                    labelBackgroundColor = AndroidColor.WHITE, // Color de Android
                                    // Puedes ajustar labelOffsetY, iconWidth, iconHeight, fontSizeSp según necesites
                                    iconWidth = 70, // Un poco más pequeño que el default de la función
                                    iconHeight = 70,
                                    labelOffsetY = 0 // Cuánto se superpone la etiqueta al icono
                                )
                            } catch (e: Exception) {
                                Log.e("MarkerBitmap", "Error creando bitmap para ${contact.name}: ${e.message}", e)
                                // Fallback a un marcador simple si la creación del bitmap falla
                                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            }
                        }

                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    contact.latitude!!,
                                    contact.longitude!!
                                )
                            ),
                            // El title y snippet son menos visibles con el icono personalizado,
                            // pero pueden ser útiles para accesibilidad o si el usuario hace un clic largo.
                            title = "${contact.name} ${contact.lastName ?: ""}".trim(),
                            snippet = contact.address,
                            icon = customIconWithLabel, // USAR EL NUEVO ICONO CON ETIQUETA
                            anchor = Offset(0.5f, 1f), // ¡IMPORTANTE! Ajusta este valor (especialmente Y)
                            // para que la "punta" del pin en tu bitmap se alinee
                            // con la coordenada geográfica. 0.95f es un buen punto de partida.
                            // X=0.5f lo centra horizontalmente.
                            // Y=1.0f sería el borde inferior exacto del bitmap.
                            onClick = {
                                Log.d("MapClick", "Clic en marcador etiquetado: ${contact.name}, ID: ${contact.id}")
                                navController.navigate("contactDetail/${contact.id}")
                                true
                            }
                        )
                    }
                }

                if (isLoading && contactsToShowOnMap.isNotEmpty()) { // Debería ser !isLoading
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }

                // Esta condición parece redundante o incorrecta si la de arriba es para !isLoading
                // if (!isLoading && contactsToShowOnMap.isEmpty()) {
                //     Text(
                //         text = stringResource(R.string.no_contacts_to_display_on_map),
                //         modifier = Modifier.align(Alignment.Center)
                //     )
                // }
            }
        }

        // SEGUNDO LaunchedEffect: Para la lógica del mapa una vez cargado y con contactos
        LaunchedEffect(contactsToShowOnMap, isMapLoaded) {
            Log.d("MapDebug", "Segundo LE: Disparado. isMapLoaded: $isMapLoaded, contactsToShowOnMap: ${contactsToShowOnMap.size}")
            if (!isMapLoaded) {
                Log.d("MapDebug", "Segundo LE: Mapa no cargado aún. Saliendo.")
                return@LaunchedEffect
            }

            if (contactsToShowOnMap.isNotEmpty()) {
                Log.d("MapDebug", "Segundo LE: Mapa cargado y hay contactos. Preparando animación.")
                val boundsBuilder = LatLngBounds.Builder()
                contactsToShowOnMap.forEach { contact ->
                    boundsBuilder.include(LatLng(contact.latitude!!, contact.longitude!!))
                }

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
                    if (contactsToShowOnMap.isNotEmpty()) {
                        if (contactsToShowOnMap.size == 1) {
                            contactsToShowOnMap.firstOrNull()?.let {
                                Log.d("MapDebug", "Segundo LE: Fallback - Estableciendo posición para un contacto.")
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                    LatLng(it.latitude!!, it.longitude!!), 15f
                                )
                            }
                        } else {
                            Log.d("MapDebug", "Segundo LE: Fallback - Múltiples contactos, considerar move().")
                            try {
                                cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 150))
                            } catch (moveEx: Exception) {
                                Log.e("MapDebug", "Segundo LE: Error también en move() fallback: ${moveEx.message}")
                            }
                        }
                    }
                }
            } else {
                Log.d("MapDebug", "Segundo LE: Mapa cargado pero no hay contactos. Estableciendo posición por defecto.")
                // Mueve la cámara a la posición por defecto si no hay contactos que mostrar
                cameraPositionState.position = CameraPosition.fromLatLngZoom(defaultCameraPosition, 2f) // o un zoom más apropiado
            }
        }
    }
}