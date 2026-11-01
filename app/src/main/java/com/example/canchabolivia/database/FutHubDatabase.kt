package com.example.canchabolivia.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClimaEntity::class], version = 1, exportSchema = false)
abstract class FutHubDatabase : RoomDatabase() {

    abstract fun climaDao(): ClimaDao

    companion object {
        @Volatile
        private var instancia: FutHubDatabase? = null

        fun obtener(context: Context): FutHubDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    FutHubDatabase::class.java,
                    "futhub.db"
                )
                    // Solo guarda caché del clima: si la versión del esquema no coincide
                    // (por ejemplo, la base quedó de otra rama), se borra y se recrea.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build().also { instancia = it }
            }
    }
}
