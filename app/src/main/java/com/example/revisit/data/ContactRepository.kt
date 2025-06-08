package com.example.revisit.data

import com.example.revisit.data.db.ContactEntity
import com.example.revisit.data.db.ContactDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository module for handling data operations.
 * This class abstracts the data source (ContactDao) from the ViewModels.
 */
open class ContactRepository(private val contactDao: ContactDao) {

    // Get all contacts sorted by name
    val allContactsSortedByName: Flow<List<ContactEntity>> = contactDao.getAllContactsSortedByName()

    suspend fun getContactsByIds(ids: List<Int>): List<ContactEntity> {
        if (ids.isEmpty()) { // Es buena práctica manejar la lista vacía aquí también o en el ViewModel
            return emptyList()
        }
        return contactDao.getContactsByIds(ids) // <-- AQUÍ SE USA contactDao
}

    suspend fun insertAll(contacts: List<ContactEntity>) {
        contactDao.insertAll(contacts)
    }

    fun getAllSync(): List<ContactEntity> {
        return contactDao.getAllSync()
    }

    /**
     * Insert a new contact into the database.
     * This is a suspend function, so it must be called from a coroutine or another suspend function.
     */
    suspend fun insertContact(contact: ContactEntity) {
        contactDao.insertContact(contact)
    }
    /**
     * Update an existing contact.
     */
    suspend fun updateContact(contact: ContactEntity) {
        contactDao.updateContact(contact)
    }
    /**
     * Delete a contact.
     */
    suspend fun deleteContact(contact: ContactEntity) {
        contactDao.deleteContact(contact)
    }
    /**
     * Get a specific contact by its ID.
     * Returns null if the contact is not found.
     */
    suspend fun getContactById(contactId: Int): ContactEntity? {
        return contactDao.getContactById(contactId)
    }

    // You can add more methods here as needed, wrapping other DAO functions.
}