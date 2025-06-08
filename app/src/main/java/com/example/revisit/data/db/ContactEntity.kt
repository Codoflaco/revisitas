package com.example.revisit.data.db

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
    val profile: String?,
    val address: String?,
    val phoneNumber: String?,

    // Defaulted to current time, can be overridden if needed
    val creationOrFirstVisitTimestamp: Long = System.currentTimeMillis(),
    val nextVisitLastSetTimestamp: Long,

    val nextVisitTimestamp: Long,

    val notes: String?,
    val lastInteractionTimestamp: Long
)