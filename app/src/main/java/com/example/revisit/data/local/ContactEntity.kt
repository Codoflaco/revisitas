package com.example.revisit.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts_table")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val latitude: Double? = null,
    val longitude: Double? = null,

    val name: String,
    val lastName: String?,
    val territory: Int?,
    val imageUri: String? =  null,
    val address: String?,
    val phoneNumber: String?,

    val creationOrFirstVisitTimestamp: Long = System.currentTimeMillis(),
    val nextVisitLastSetTimestamp: Long,

    val nextVisitTimestamp: Long,

    val notes: String?,
    val lastInteractionTimestamp: Long
)