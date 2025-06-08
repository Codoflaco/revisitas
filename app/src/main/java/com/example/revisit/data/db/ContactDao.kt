package com.example.revisit.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {

    // --- Basic CRUD Operations ---

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    // --- Querying Contacts ---

    @Query("SELECT * FROM contacts_table")
    suspend fun getAll(): List<ContactEntity>

    @Query("SELECT * FROM contacts_table")
    fun getAllSync(): List<ContactEntity>

    @Query("SELECT * FROM contacts_table WHERE id = :contactId")
    suspend fun getContactById(contactId: Int): ContactEntity?

    @Query("SELECT * FROM contacts_table ORDER BY lastName ASC, name ASC")
    fun getAllContactsSortedByName(): Flow<List<ContactEntity>>

    // Adjusted this query to use the correct field name and handle potential nulls
    // if nextVisitTimestamp is null, those contacts will typically be sorted last or first
    // depending on the DB's NULL ordering (SQLite typically treats NULLs as smaller than other values).
    @Query("SELECT * FROM contacts_table ORDER BY nextVisitTimestamp ASC")
    fun getAllContactsSortedByNextVisit(): Flow<List<ContactEntity>>

    // --- Filtering and Searching ---

    // Assuming 'territory' is Int? in ContactEntity
    @Query("SELECT * FROM contacts_table WHERE territory = :territoryId ORDER BY lastName ASC, name ASC")
    fun getContactsByTerritory(territoryId: Int): Flow<List<ContactEntity>>

    // Get contacts whose next visit is on or before a certain date (for "due" visits)
    // and where nextVisitTimestamp is not null.
    @Query("SELECT * FROM contacts_table WHERE nextVisitTimestamp IS NOT NULL AND nextVisitTimestamp <= :dateTimestamp ORDER BY nextVisitTimestamp ASC")
    fun getContactsDueForVisit(dateTimestamp: Long): Flow<List<ContactEntity>>

    // Search by name, last name, or potentially other fields
    // Using LOWER() for case-insensitive search
    @Query("SELECT * FROM contacts_table WHERE " +
            "LOWER(name) LIKE '%' || LOWER(:searchQuery) || '%' OR " +
            "LOWER(lastName) LIKE '%' || LOWER(:searchQuery) || '%' OR " +
            "LOWER(notes) LIKE '%' || LOWER(:searchQuery) || '%' " +
            "ORDER BY lastName ASC, name ASC")
    fun searchContacts(searchQuery: String): Flow<List<ContactEntity>>

    // --- Specific fields (Examples) ---

    @Query("SELECT * FROM contacts_table WHERE id = :contactId")
    fun getContactFlowById(contactId: Int): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts_table WHERE id IN (:ids)") // :ids es la forma de pasar una lista de parámetros
    suspend fun getContactsByIds(ids: List<Int>): List<ContactEntity>

    
}