package com.example.revisit.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.revisit.data.db.ContactEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object BackupUtils {

    private const val BACKUP_FILE_NAME = "contacts_backup.json"

    fun backupToJsonFile(context: Context, contacts: List<ContactEntity>): Uri? {
        return try {
            val json = Gson().toJson(contacts)
            val file = File(context.getExternalFilesDir(null), BACKUP_FILE_NAME)
            file.writeText(json)

            // Use FileProvider to return a content:// URI
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreFromJsonFile(context: Context, uri: Uri): List<ContactEntity>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val json = inputStream?.bufferedReader().use { it?.readText() }
            val type = object : TypeToken<List<ContactEntity>>() {}.type
            Gson().fromJson<List<ContactEntity>>(json, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
