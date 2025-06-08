package com.example.revisit.ui

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.revisit.data.ContactRepository
import com.example.revisit.data.db.ContactEntity
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job

open class ContactViewModel(
    private val applicationContext: Context, // Ya está aquí, ¡genial!
    private val repository: ContactRepository
) : ViewModel() {

    private val geocoder by lazy {
        Geocoder(applicationContext) // Usa el applicationContext inyectado
    }

    private suspend fun geocodeAddress(address: String): LatLng? {
        if (address.isBlank()) {
            return null
        }
        return withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION") // getFromLocationName está deprecated en API 33+ pero es necesario para compatibilidad
                val addresses = geocoder.getFromLocationName(address, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val location = addresses[0]
                    val latLng = LatLng(location.latitude, location.longitude)
                    return@withContext latLng
                } else {
                    return@withContext null
                }
        }
    }

    // --- Nueva función para guardar o actualizar con geocodificación ---
    suspend fun saveOrUpdateContact(
        id: Int?, // Null si es un contacto nuevo
        name: String,
        lastName: String?,
        phoneNumber: String?,
        address: String?, // La dirección a geocodificar
        territory: Int?,
        notes: String?,
        profile: String?,
        creationOrFirstVisitTimestamp: Long,
        nextVisitTimestamp: Long,
        lastInteractionTimestamp: Long
        // No necesitas pasar latitude/longitude aquí, se obtendrán de geocodeAddress
    ) {
        //viewModelScope.launch {
            var finalLatitude: Double? = null
            var finalLongitude: Double? = null

            if (!address.isNullOrBlank()) {
                val geocodedLatLng = geocodeAddress(address) // Llama a tu función
                if (geocodedLatLng != null) {
                    finalLatitude = geocodedLatLng.latitude
                    finalLongitude = geocodedLatLng.longitude
                    Log.d("ViewModelSave", "Geocoded: Lat=$finalLatitude, Lng=$finalLongitude for address '$address'")
                } else {
                    Log.w("ViewModelSave", "Geocoding failed or no result for address '$address', Lat/Lng will be null.")
                }
            } else {
                Log.d("ViewModelSave", "Address is null or blank. Lat/Lng will be null.")
            }


            val contact = ContactEntity(
                id = id ?: 0, // Room maneja el autoincremento si id es 0
                name = name.trim(),
                lastName = lastName?.trim()?.ifEmpty { null },
                phoneNumber = phoneNumber?.trim()?.ifEmpty { null },
                address = address?.trim()?.ifEmpty { null }, // Guarda la dirección original
                territory = territory,
                notes = notes?.trim()?.ifEmpty { null },
                profile = profile?.trim()?.ifEmpty { null },
                creationOrFirstVisitTimestamp = creationOrFirstVisitTimestamp,
                nextVisitTimestamp = nextVisitTimestamp,
                nextVisitLastSetTimestamp = System.currentTimeMillis(),
                latitude = finalLatitude, // Guarda la latitud obtenida
                longitude = finalLongitude, // Guarda la longitud obtenida
                lastInteractionTimestamp = System.currentTimeMillis(),
            )
        withContext(Dispatchers.IO) {
            if (id != null && id != 0) {
                Log.d("ViewModelSave", "Updating contact: $contact")
                repository.updateContact(contact)
            } else {
                // Asegúrate que tu DAO insert maneja la autogeneración del ID correctamente
                Log.d("ViewModelSave", "Inserting new contact: $contact")
                repository.insertContact(contact.copy(id = 0)) // Explicitly set id to 0 for Room to auto-generate
            }
        }
        //}

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

    val allContactsSortedByName: StateFlow<List<ContactEntity>> = repository.allContactsSortedByName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

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
        // CAMBIO 1: Modificar el constructor de la Factory
        class ContactViewModelFactory(
            private val applicationContext: Context, // <-- AÑADIDO
            private val repository: ContactRepository
        ) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ContactViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    // CAMBIO 2: Pasar applicationContext al constructor del ViewModel
                    return ContactViewModel(applicationContext, repository) as T // <-- MODIFICADO
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}