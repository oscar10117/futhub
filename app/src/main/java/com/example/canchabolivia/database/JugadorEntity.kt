package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Tabla de jugadores. Cada jugador pertenece a un equipo (relación 1 a N):
// la clave foránea con CASCADE hace que al borrar el equipo se borren sus jugadores.
@Entity(
    tableName = "jugadores",
    foreignKeys = [
        ForeignKey(
            entity = EquipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["equipoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Índice sobre la clave foránea: Room lo pide para no recorrer toda la tabla.
    indices = [Index("equipoId")]
)
data class JugadorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val equipoId: Int,
    val dorsal: Int
)
