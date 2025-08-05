package com.example.revisit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ContactEntity::class],
    version = 4, // <--- VERSIÓN INCREMENTADA A 4
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // MIGRATION_1_3: Define cómo era la tabla en la versión 3.
        // ASUNCIONES BASADAS EN TUS RESPUESTAS:
        // - En v3, la columna se llamaba 'profile'.
        // - Los campos nextVisitLastSetTimestamp, lastInteractionTimestamp, latitude, longitude
        //   ya tenían su estado final para v3 en esta definición.
        val MIGRATION_1_3: Migration = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Esta es la estructura que TU ContactEntity y TU tabla tenían en v3
                db.execSQL("""
                    CREATE TABLE contacts_table_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL, /* O REAL NOT NULL si así estaba en v3 */
                        longitude REAL, /* O REAL NOT NULL si así estaba en v3 */
                        name TEXT NOT NULL,
                        lastName TEXT,
                        territory INTEGER, 
                        profile TEXT, /* <--- Columna 'profile' en v3 */
                        address TEXT,
                        phoneNumber TEXT,
                        creationOrFirstVisitTimestamp INTEGER NOT NULL,
                        nextVisitLastSetTimestamp INTEGER NOT NULL, /* Asumiendo que estaba en v3 */
                        nextVisitTimestamp INTEGER NOT NULL, 
                        notes TEXT,
                        lastInteractionTimestamp INTEGER NOT NULL /* Asumiendo que estaba en v3 */
                    )
                """.trimIndent())

                // Copiar datos desde la versión 1 a la nueva tabla de versión 3
                // DEBES ASEGURARTE que las columnas seleccionadas de 'contacts_table' (v1)
                // y las columnas de inserción en 'contacts_table_new' (v3) coincidan.
                // Esta parte es muy dependiente de cómo era tu v1.
                db.execSQL("""
                    INSERT INTO contacts_table_new (
                        id, latitude, longitude, name, lastName, territory, profile, 
                        address, phoneNumber, creationOrFirstVisitTimestamp, 
                        nextVisitLastSetTimestamp, nextVisitTimestamp, notes, lastInteractionTimestamp
                    )
                    SELECT 
                        id, latitude, longitude, name, lastName, territory, 
                        profile, /* O como se llamara en v1 y quisieras mapearlo a 'profile' en v3 */
                        address, phoneNumber, creationOrFirstVisitTimestamp, 
                        0, /* Valor por defecto para nextVisitLastSetTimestamp si no existía en v1 */
                        COALESCE(nextVisitTimestamp, creationOrFirstVisitTimestamp, (STRFTIME('%s','now') * 1000)), 
                        notes,
                        0  /* Valor por defecto para lastInteractionTimestamp si no existía en v1 */
                    FROM contacts_table 
                """.trimIndent())

                db.execSQL("DROP TABLE contacts_table")
                db.execSQL("ALTER TABLE contacts_table_new RENAME TO contacts_table")
            }
        }

        // MIGRACIÓN de la versión 3 a la 4
        // ÚNICO CAMBIO: Renombrar columna 'profile' a 'imageUri'.
        // No hay cambios en nextVisitLastSetTimestamp, lastInteractionTimestamp, latitude, longitude.
        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Usamos la técnica de crear tabla nueva, copiar, borrar, renombrar,
                // ya que es la forma más robusta y compatible de renombrar una columna
                // y asegurar que el esquema de la tabla final coincida con la entidad v4.

                // Paso 1: Crear una tabla temporal con el esquema de v4 (con 'imageUri')
                // La estructura debe ser idéntica a la de v3, EXCEPTO por el nombre de la columna.
                db.execSQL("""
                    CREATE TABLE contacts_table_v4_temp (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        latitude REAL, /* Mantiene estado de v3 */
                        longitude REAL, /* Mantiene estado de v3 */
                        name TEXT NOT NULL,
                        lastName TEXT,
                        territory INTEGER, 
                        imageUri TEXT, /* <--- Columna renombrada a 'imageUri' */
                        address TEXT,
                        phoneNumber TEXT,
                        creationOrFirstVisitTimestamp INTEGER NOT NULL,
                        nextVisitLastSetTimestamp INTEGER NOT NULL, /* Mantiene estado de v3 */
                        nextVisitTimestamp INTEGER NOT NULL, 
                        notes TEXT,
                        lastInteractionTimestamp INTEGER NOT NULL /* Mantiene estado de v3 */
                    )
                """.trimIndent())

                // Paso 2: Copiar los datos de la tabla v3 a la tabla v4 temporal.
                // El contenido de la columna 'profile' (de v3) se inserta en 'imageUri' (de v4).
                db.execSQL("""
                    INSERT INTO contacts_table_v4_temp (
                        id, latitude, longitude, name, lastName, territory, imageUri, 
                        address, phoneNumber, creationOrFirstVisitTimestamp, 
                        nextVisitLastSetTimestamp, nextVisitTimestamp, notes, lastInteractionTimestamp
                    )
                    SELECT 
                        id, latitude, longitude, name, lastName, territory, 
                        profile, /* <--- Selecciona de la columna 'profile' de la tabla v3 */
                        address, phoneNumber, creationOrFirstVisitTimestamp, 
                        nextVisitLastSetTimestamp, nextVisitTimestamp, notes, lastInteractionTimestamp
                    FROM contacts_table /* Esta es la tabla 'contacts_table' de la v3 */
                """.trimIndent())

                // Paso 3: Eliminar la tabla antigua (v3)
                db.execSQL("DROP TABLE contacts_table")

                // Paso 4: Renombrar la tabla temporal (v4) a la original
                db.execSQL("ALTER TABLE contacts_table_v4_temp RENAME TO contacts_table")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "revisit_database"
                )
                    .addMigrations(MIGRATION_1_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
