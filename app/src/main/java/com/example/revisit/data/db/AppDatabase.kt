package com.example.revisit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class], // List all your entities here
    version = 3,                       // Increment this when you change the schema
    exportSchema = false               // Set to true if you want to export schema to a folder
)
// If you had TypeConverters, you would add this annotation:
// @TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao // Abstract method to get your DAO

    companion object {
        // @Volatile - The value of a volatile variable will never be cached,
        // and all writes and reads will be done to and from the main memory.
        // This helps make sure the value of INSTANCE is always up-to-date and
        // the same to all execution threads.
        @Volatile
        private var INSTANCE: AppDatabase? = null
        // Define your migration
        val MIGRATION_1_3: Migration = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table with nextVisitTimestamp as NOT NULL
                //    Make sure all column definitions match your ContactEntity EXACTLY
                //    (types, nullability, column names).
                //    The table name for ContactEntity will be "ContactEntity" by default
                //    unless you specified @Entity(tableName = "...")
                db.execSQL("""
                    CREATE TABLE contacts_table_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        name TEXT NOT NULL,
                        lastName TEXT,
                        territory INTEGER, 
                        profile TEXT,
                        address TEXT,
                        phoneNumber TEXT,
                        creationOrFirstVisitTimestamp INTEGER NOT NULL,
                        nextVisitTimestamp INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}, 
                        notes TEXT 
                    )
                """.trimIndent()) // Added 'notes TEXT' assuming it's in your entity. Verify all columns!

                // 2. Copy data. Use COALESCE to provide a default for nextVisitTimestamp
                //    if it was NULL in the old table.
                db.execSQL("""
                    INSERT INTO contacts_table_new (id, latitude, longitude, name, lastName, territory, profile, address, phoneNumber, creationOrFirstVisitTimestamp, nextVisitTimestamp, notes)
                    SELECT id, latitude, longitude, name, lastName, territory, profile, address, phoneNumber, creationOrFirstVisitTimestamp,
                           COALESCE(nextVisitTimestamp, creationOrFirstVisitTimestamp, ${System.currentTimeMillis()}), notes
                    FROM contacts_table 
                """.trimIndent()) // Ensure table name "ContactEntity" is correct

                // 3. Drop the old table
                db.execSQL("DROP TABLE contacts_table")

                // 4. Rename the new table to the original name
                db.execSQL("ALTER TABLE contacts_table_new RENAME TO contacts_table")
            }
        }
        fun getDatabase(context: Context): AppDatabase {
            // If the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "revisit_database" // Name of your database file
                )
                     .addMigrations(MIGRATION_1_3) // Add migrations if you update schema
                    // .fallbackToDestructiveMigration() // Use this ONLY during development if you don't want to write migrations
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}