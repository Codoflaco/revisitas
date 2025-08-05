package com.example.revisit.ui.contacts

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.revisit.data.local.ContactEntity
import com.example.revisit.data.repository.ContactRepository
import com.example.revisit.util.DateTimeUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class ContactViewModel(
    private val applicationContext: Context,
    private val repository: ContactRepository
) : ViewModel() {

    private val geocoder by lazy {
        Geocoder(applicationContext)
    }

    // --- INICIO: LÓGICA DE FILTRADO EN VIEWMODEL ---

    // Filtro por Territorio
    private val _territoryFilter = MutableStateFlow("")
    val territoryFilter: StateFlow<String> = _territoryFilter.asStateFlow() // Exponer como inmutable

    fun updateTerritoryFilter(newTerritory: String) {
        _territoryFilter.value = newTerritory
        Log.d("ViewModelFilter", "Territory filter updated to: $newTerritory")
    }

    // Filtro por Fecha de Próxima Visita (timestamp)
    private val _nextVisitDateFilterTimestamp = MutableStateFlow<Long?>(null)
    val nextVisitDateFilterTimestamp: StateFlow<Long?> = _nextVisitDateFilterTimestamp.asStateFlow()

    fun updateNextVisitDateFilter(newTimestamp: Long?) {
        _nextVisitDateFilterTimestamp.value = newTimestamp
        Log.d("ViewModelFilter", "Next visit date filter updated to: $newTimestamp")
    }

    fun clearFilters() {
        _territoryFilter.value = ""
        _nextVisitDateFilterTimestamp.value = null
        Log.d("ViewModelFilter", "Filters cleared")
        // No necesitamos resetear el DatePickerState aquí, eso es UI y se manejará en la pantalla
    }

    // Lista base de contactos
    val allContactsSortedByName: StateFlow<List<ContactEntity>> = repository.allContactsSortedByName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // NUEVO: StateFlow para los contactos filtrados que las pantallas observarán
    val displayedContacts: StateFlow<List<ContactEntity>> =
        combine(
            allContactsSortedByName,
            _territoryFilter,
            _nextVisitDateFilterTimestamp
        ) { contacts, territoryQuery, dateTimestamp ->
            Log.d(
                "ViewModelFilter",
                "Combining filters. Territory: '$territoryQuery', Date: $dateTimestamp, Contacts base: ${contacts.size}"
            )
            if (territoryQuery.isBlank() && dateTimestamp == null) {
                contacts
            } else {
                contacts.filter { contact ->
                    val territoryMatches = if (territoryQuery.isNotBlank()) {
                        contact.territory?.toString()
                            ?.equals(territoryQuery, ignoreCase = true) == true
                    } else {
                        true // Si el filtro de territorio está vacío, todos los contactos coinciden en este criterio
                    }

                    val dateMatches = if (dateTimestamp != null) {
                        contact.nextVisitTimestamp.let { visitTime ->
                            // Asegúrate que DateTimeUtils está disponible y funciona como se espera
                            val filterStartOfDay =
                                DateTimeUtils.getStartOfDayUTCTimestamp(dateTimestamp)
                            val filterEndOfDay =
                                DateTimeUtils.getEndOfDayUTCTimestamp(dateTimestamp)
                            visitTime >= filterStartOfDay && visitTime <= filterEndOfDay
                        }
                    } else {
                        true // Si el filtro de fecha está vacío, todos los contactos coinciden en este criterio
                    }
                    val matches = territoryMatches && dateMatches
                    // Log.v("ViewModelFilter", "Contact ID ${contact.id}: T_match=$territoryMatches, D_match=$dateMatches -> 최종 $matches") // Log detallado si es necesario
                    matches
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000L),
            initialValue = emptyList() // Inicialmente vacío hasta que allContactsSortedByName emita
        )
    // --- FIN: LÓGICA DE FILTRADO EN VIEWMODEL ---


    private suspend fun geocodeAddress(address: String): LatLng? {
        if (address.isBlank()) {
            return null
        }
        return withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    val latLng = LatLng(location.latitude, location.longitude)
                    Log.d("ViewModelGeocode", "Geocoded '$address' to: $latLng")
                    return@withContext latLng
                } else {
                    Log.w("ViewModelGeocode", "No geocoding result for address '$address'")
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("ViewModelGeocode", "Error geocoding address '$address'", e)
                return@withContext null
            }
        }
    }

    suspend fun saveOrUpdateContact(
        id: Int?,
        name: String,
        lastName: String?,
        phoneNumber: String?,
        address: String?,
        territory: Int?,
        notes: String?,
        imageUri: String?,
        creationOrFirstVisitTimestamp: Long,
        nextVisitTimestamp: Long,
        pickedLatitude: Double? = null,
        pickedLongitude: Double? = null
    ) {
        var finalLatitude: Double? = null
        var finalLongitude: Double? = null

        if (pickedLatitude != null && pickedLongitude != null) {
            finalLatitude = pickedLatitude
            finalLongitude = pickedLongitude
            Log.d("ViewModelSave", "Using picked coordinates: Lat=$finalLatitude, Lng=$finalLongitude. Address: '$address'")
        }
        else if (!address.isNullOrBlank()) {
            Log.d("ViewModelSave", "No picked coordinates, attempting to geocode address: '$address'")
            val geocodedLatLng = geocodeAddress(address)
            if (geocodedLatLng != null) {
                finalLatitude = geocodedLatLng.latitude
                finalLongitude = geocodedLatLng.longitude
                Log.d("ViewModelSave", "Successfully geocoded: Lat=$finalLatitude, Lng=$finalLongitude for address '$address'")
            } else {
                Log.w("ViewModelSave", "Geocoding failed or no result for address '$address'. Lat/Lng will be null.")
            }
        }
        else {
            Log.d("ViewModelSave", "Address is null or blank, and no picked coordinates. Lat/Lng will be null.")
        }

        val contact = ContactEntity(
            id = id ?: 0,
            name = name.trim(),
            lastName = lastName?.trim()?.ifEmpty { null },
            phoneNumber = phoneNumber?.trim()?.ifEmpty { null },
            address = address?.trim()?.ifEmpty { null },
            territory = territory,
            notes = notes?.trim()?.ifEmpty { null },
            imageUri = imageUri.toString().ifEmpty { null },
            creationOrFirstVisitTimestamp = creationOrFirstVisitTimestamp,
            nextVisitTimestamp = nextVisitTimestamp,
            nextVisitLastSetTimestamp = System.currentTimeMillis(),
            latitude = finalLatitude,
            longitude = finalLongitude,
            lastInteractionTimestamp = System.currentTimeMillis()
        )

        withContext(Dispatchers.IO) {
            if (id != null && id != 0) {
                Log.d("ViewModelSave", "Updating contact: $contact")
                repository.updateContact(contact)
            } else {
                Log.d(
                    "ViewModelSave",
                    "Inserting new contact: ${contact.copy(id = if (id == 0) 0 else 0)}"
                )
                repository.insertContact(contact.copy(id = if (id == 0 && contact.id == 0) 0 else 0))
            }
        }
    }

    fun updateContactCoordinates(contactId: Int, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val contact = repository.getContactById(contactId)
            if (contact != null) {
                val updatedContact = contact.copy(
                    latitude = latitude,
                    longitude = longitude,
                    lastInteractionTimestamp = System.currentTimeMillis()
                )
                repository.updateContact(updatedContact)
                Log.d("ContactViewModel", "Contact coordinates updated directly: ID $contactId, Lat $latitude, Lng $longitude")
            } else {
                Log.w("ContactViewModel", "Attempted to update coordinates for non-existent contact ID: $contactId")
            }
        }
    }

    suspend fun getContactsByIds(ids: List<Int>): List<ContactEntity> {
        return withContext(Dispatchers.IO) {
            if (ids.isEmpty()) {
                emptyList()
            } else {
                repository.getContactsByIds(ids)
            }
        }
    }

    suspend fun getAllContacts(): List<ContactEntity> = withContext(Dispatchers.IO) {
        repository.getAllSync()
    }

    fun insertAllContacts(contacts: List<ContactEntity>) {
        viewModelScope.launch {
            repository.insertAll(contacts)
        }
    }

    fun delete(contact: ContactEntity): Job = viewModelScope.launch {
        repository.deleteContact(contact)
    }

    suspend fun getContact(contactId: Int): ContactEntity? {
        return repository.getContactById(contactId)
    }

    companion object {
        class ContactViewModelFactory(
            private val applicationContext: Context,
            private val repository: ContactRepository
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ContactViewModel(applicationContext, repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}