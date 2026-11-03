package com.example.canchabolivia.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.canchabolivia.Formato

// La app maneja un solo torneo: una fila con id fijo (igual que el clima).
// Room guarda el enum Formato como texto ("LIGA" / "ELIMINACION").
@Entity(tableName = "torneo")
data class TorneoEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    val nombre: String,
    val formato: Formato,
    // versión 4: el organizador puede terminar el torneo
    @ColumnInfo(defaultValue = "0") val finalizado: Boolean = false,
    // Se guarda el nombre (no el id) para que el campeón siga apareciendo
    // aunque después se borre el equipo.
    val campeon: String? = null
) {
    companion object {
        const val ID_UNICO = 1
    }
}
