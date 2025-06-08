package com.example.revisit

import android.app.Application
import com.example.revisit.data.db.AppDatabase
import com.example.revisit.data.ContactRepository

class RevisitApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
    val repository: ContactRepository by lazy { ContactRepository(database.contactDao()) }
}