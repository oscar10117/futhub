package com.example.canchabolivia.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.canchabolivia.EstadoFicha

// Fichas médicas (reserva del jugador + respuesta/evaluación del médico).
// Los horarios del médico son fijos en el ViewModel, por eso horarioId no
// tiene clave foránea; el jugador sí.
@Entity(
    tableName = "fichas",
    foreignKeys = [
        ForeignKey(
            entity = JugadorEntity::class,
            parentColumns = ["id"],
            childColumns = ["jugadorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("jugadorId")]
)
data class FichaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val jugadorId: Int,
    val horarioId: Int,
    val estado: EstadoFicha
)
