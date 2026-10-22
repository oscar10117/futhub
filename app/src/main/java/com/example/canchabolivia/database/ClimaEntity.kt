package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.PrimaryKey

// Una sola fila (id fijo): guarda la última lectura para mostrarla sin internet.
@Entity(tableName = "clima")
data class ClimaEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    val temperatura: Double,
    val humedad: Int,
    val viento: Double,
    val hora: String
) {
    companion object {
        const val ID_UNICO = 1
    }
}
