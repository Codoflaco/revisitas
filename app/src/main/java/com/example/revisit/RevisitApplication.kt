package com.example.revisit

import android.app.Application
import com.example.revisit.data.local.AppDatabase
import com.example.revisit.data.repository.ContactRepository

class RevisitApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ContactRepository by lazy { ContactRepository(database.contactDao()) }
}